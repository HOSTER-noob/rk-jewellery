@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.rkjewellery.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rkjewellery.app.RKApp
import com.rkjewellery.app.data.local.entity.*
import com.rkjewellery.app.data.repo.*
import com.rkjewellery.app.ui.components.toDoubleOrZero
import com.rkjewellery.app.ui.components.toLongOrZero
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

// ---------- Shared factory ----------
class VMFactory(private val app: RKApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        DashboardViewModel::class.java -> DashboardViewModel(app)
        GirviViewModel::class.java -> GirviViewModel(app)
        InvoiceViewModel::class.java -> InvoiceViewModel(app)
        StockViewModel::class.java -> StockViewModel(app)
        KhataViewModel::class.java -> KhataViewModel(app)
        else -> throw IllegalArgumentException("Unknown VM $modelClass")
    } as T
}

// ---------- Dashboard ----------
class DashboardViewModel(app: RKApp) : ViewModel() {
    val cashBalance = app.db.cashLedgerDao().observeBalance()
    val totalLent = app.db.girviDao().observeTotalLent()
    val accruedInterest = app.db.girviDao().observeAccruedInterest(System.currentTimeMillis())
    val recentCash = app.db.cashLedgerDao().observeRecent(6)
    val activeGirviCount = app.girviRepo.activeGirvis().map { it.size }
    val pendingKhataCash = app.creditRepo.totalPendingCash()
    val latestRate = app.rateRepo.latest()
    val receivable = app.invoiceRepo.salesReceivable()
}

// ---------- Girvi ----------
class GirviViewModel(app: RKApp) : ViewModel() {
    private val repo = app.girviRepo
    val active = repo.activeGirvis()
    val all = repo.allGirvis()
    val totalLent = repo.totalLent()
    val accruedInterest = repo.accruedInterest(System.currentTimeMillis())

    private val _current = MutableStateFlow<GirviEntry?>(null)
    val current: StateFlow<GirviEntry?> = _current
    val currentPayments = _current.mapNotNull { it?.id }.flatMapLatest { repo.payments(it) }

    fun load(id: Long) {
        viewModelScope.launch { _current.value = app.db.girviDao().getById(id) }
    }

    suspend fun nextTicket(): String = repo.nextTicketNo()

    fun save(entry: GirviEntry, onDone: (Long) -> Unit) {
        viewModelScope.launch { onDone(repo.saveGirviIn(entry)) }
    }

    fun pay(amountPaise: Long, note: String, onDone: () -> Unit = {}) {
        val id = _current.value?.id ?: return
        viewModelScope.launch {
            repo.addPayment(id, amountPaise, note)
            _current.value = app.db.girviDao().getById(id)
            onDone()
        }
    }

    fun release(finalPaise: Long, note: String, onDone: () -> Unit = {}) {
        val id = _current.value?.id ?: return
        viewModelScope.launch {
            repo.releaseGirvi(id, finalPaise, note)
            _current.value = app.db.girviDao().getById(id)
            onDone()
        }
    }

    fun interestDue(entry: GirviEntry): Long = repo.interestDue(entry, System.currentTimeMillis())
}

// ---------- Invoices ----------
data class DraftItem(
    val itemName: String = "",
    val metalType: MetalType = MetalType.GOLD,
    val purity: Purity = Purity.N22,
    val weightGramsText: String = "",
    val qtyText: String = "1",
    val ratePerGramText: String = "",
    val makingText: String = "",
    val wastageText: String = "0"
) {
    val weightMg: Long get() = (weightGramsText.toDoubleOrZero() * 1000).toLong()
    val rate: Long get() = ratePerGramText.toLongOrZero()
    val making: Long get() = makingText.toLongOrZero()
    val qty: Int get() = qtyText.toIntOrNull()?.takeIf { it > 0 } ?: 1
    val wastage: Double get() = wastageText.toDoubleOrZero()
    val lineTotalPaise: Long
        get() {
            val chargeMg = weightMg + ((wastage / 100.0) * weightMg).toLong()
            return chargeMg / 1000 * rate + making
        }
    val isValid: Boolean get() = itemName.isNotBlank() && weightMg > 0 && rate > 0
}

class InvoiceViewModel(app: RKApp) : ViewModel() {
    private val repo = app.invoiceRepo
    val all = repo.all()
    val sales = repo.byType(InvoiceType.SALE)
    val purchases = repo.byType(InvoiceType.PURCHASE)

    suspend fun nextInvoiceNo(): String = repo.nextInvoiceNo()

    fun save(
        invoice: Invoice,
        items: List<DraftItem>,
        paidPaise: Long,
        creditMemberId: Long?,
        onDone: (Long) -> Unit
    ) {
        val mapped = items.filter { it.isValid }.map {
            InvoiceItem(
                itemName = it.itemName.trim(),
                metalType = it.metalType,
                purity = it.purity,
                weightMg = it.weightMg,
                qty = it.qty,
                ratePerGramPaise = it.rate,
                makingChargesPaise = it.making,
                wastagePercent = it.wastage
            )
        }
        viewModelScope.launch {
            val id = repo.saveInvoice(invoice, mapped, paidPaise, creditMemberId)
            onDone(id)
        }
    }

    fun loadForPreview(id: Long, cb: (Invoice?, List<InvoiceItem>) -> Unit) {
        viewModelScope.launch {
            val w = repo.getWithItems(id)
            cb(w?.invoice, w?.items ?: emptyList())
        }
    }
}

// ---------- Stock ----------
class StockViewModel(app: RKApp) : ViewModel() {
    private val bullionRepo = BullionRepository(app.db)
    val goldStock = app.db.invoiceDao().observeStockByPurity(MetalType.GOLD.name)
    val silverStock = app.db.invoiceDao().observeStockByPurity(MetalType.SILVER.name)
    val bullion = bullionRepo.all()
    val rates = app.rateRepo.all()
    val latestRate = app.rateRepo.latest()
    val cash = app.db.cashLedgerDao().observeBalance()

    fun saveRate(goldPerGramPaise: Long, silverPerGramPaise: Long, notes: String = "") {
        viewModelScope.launch {
            app.rateRepo.save(
                MetalRate(
                    goldPerGramPaise = goldPerGramPaise,
                    silverPerGramPaise = silverPerGramPaise,
                    notes = notes
                )
            )
        }
    }

    fun saveBullion(txn: BullionTransaction) {
        viewModelScope.launch { bullionRepo.save(txn) }
    }
}

// ---------- Udhaar Khata ----------
class KhataViewModel(app: RKApp) : ViewModel() {
    private val repo = app.creditRepo
    val members = repo.members()
    val totalPendingCash = repo.totalPendingCash()

    private val _memberId = MutableStateFlow(0L)
    val currentMember: StateFlow<com.rkjewellery.app.data.local.entity.CreditMember?> =
        _memberId.flatMapLatest { id -> if (id == 0L) flowOf(null) else repo.member(id) }
    val memberTxns: Flow<List<CreditTransaction>> =
        _memberId.flatMapLatest { id -> if (id == 0L) flowOf(emptyList()) else repo.txns(id) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val memberPending: StateFlow<CreditRepository.PendingTotals?> =
        _memberId.flatMapLatest { id ->
            if (id == 0L) flowOf(null)
            // re-compute whenever the ledger changes
            else repo.txns(id).map { repo.pendingTotals(id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun load(id: Long) { _memberId.value = id }

    fun addMember(name: String, phone: String, address: String) {
        viewModelScope.launch { repo.addMember(CreditMember(name = name.trim(), phone = phone, address = address)) }
    }

    fun addTxn(type: CreditTxnType, cash: Long, goldMg: Long, silverMg: Long, note: String) {
        val memberId = _memberId.value
        if (memberId == 0L) return
        viewModelScope.launch {
            repo.addTxn(CreditTransaction(memberId = memberId, type = type, cashPaise = cash, goldMg = goldMg, silverMg = silverMg, note = note))
        }
    }
}
