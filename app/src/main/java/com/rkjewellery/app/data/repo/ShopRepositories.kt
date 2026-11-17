package com.rkjewellery.app.data.repo

import com.rkjewellery.app.data.local.RKDatabase
import com.rkjewellery.app.data.local.entity.BullionDirection
import com.rkjewellery.app.data.local.entity.BullionTransaction
import com.rkjewellery.app.data.local.entity.CashDirection
import com.rkjewellery.app.data.local.entity.CashLedger
import com.rkjewellery.app.data.local.entity.CreditTransaction
import com.rkjewellery.app.data.local.entity.CreditTxnType
import com.rkjewellery.app.data.local.entity.Invoice
import com.rkjewellery.app.data.local.entity.InvoiceItem
import com.rkjewellery.app.data.local.entity.InvoiceType
import com.rkjewellery.app.data.local.entity.LedgerRefType
import com.rkjewellery.app.data.local.dao.InvoiceWithItems
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val db: RKDatabase) {

    fun all(): Flow<List<Invoice>> = db.invoiceDao().observeAll()
    fun byType(type: InvoiceType): Flow<List<Invoice>> = db.invoiceDao().observeByType(type.name)
    fun invoice(id: Long): Flow<Invoice?> = db.invoiceDao().observeById(id)
    fun items(id: Long): Flow<List<InvoiceItem>> = db.invoiceDao().observeItems(id)
    fun salesReceivable(): Flow<Long> = db.invoiceDao().observeSalesReceivable()

    suspend fun nextInvoiceNo(): String =
        com.rkjewellery.app.util.DocNumberGenerator.next("INV", db.invoiceDao().count())

    /**
     * Saves invoice with items. Auto effects:
     *  - SALE (Bill Out): stock auto-deducted (computed live from invoice rows),
     *    cash CREDIT for the paid amount, unpaid part optionally posted to Udhaar Khata.
     *  - PURCHASE (Bill In): stock auto-added, cash DEBIT for paid amount.
     */
    suspend fun saveInvoice(
        invoice: Invoice,
        items: List<InvoiceItem>,
        paidPaise: Long,
        creditMemberId: Long? = null
    ): Long {
        if (items.isEmpty()) throw IllegalArgumentException("Add at least one item")
        val subtotal = items.sumOf { it.lineTotalPaise }
        val taxable = subtotal - invoice.discountPaise
        val gst = (taxable * invoice.gstPercent / 100.0).toLong()
        val total = taxable + gst
        val paid = paidPaise.coerceIn(0, total)

        val savedInvoice = invoice.copy(
            itemsSubtotalPaise = subtotal,
            gstAmountPaise = gst,
            totalPaise = total,
            paidPaise = paid
        )
        val id = db.invoiceDao().insertInvoice(savedInvoice)
        db.invoiceDao().insertItems(items.map { it.copy(invoiceId = id) })

        if (paid > 0) {
            db.cashLedgerDao().insert(
                CashLedger(
                    direction = if (invoice.type == InvoiceType.SALE) CashDirection.CREDIT else CashDirection.DEBIT,
                    amountPaise = paid,
                    refType = if (invoice.type == InvoiceType.SALE) LedgerRefType.SALE else LedgerRefType.PURCHASE,
                    refId = id,
                    note = invoice.invoiceNo + " · " + invoice.customerName
                )
            )
        }

        // Balance on credit goes to the Udhaar Khata ledger
        val balance = total - paid
        if (balance > 0 && creditMemberId != null && invoice.type == InvoiceType.SALE) {
            db.creditDao().insertTxn(
                CreditTransaction(
                    memberId = creditMemberId,
                    type = CreditTxnType.CASH_TAKEN,
                    cashPaise = balance,
                    note = "Invoice " + invoice.invoiceNo + " pending"
                )
            )
        }
        return id
    }

    suspend fun getWithItems(id: Long): InvoiceWithItems? {
        val invoice = db.invoiceDao().getById(id) ?: return null
        return InvoiceWithItems(invoice, db.invoiceDao().itemsOf(id))
    }

    suspend fun delete(id: Long) = db.invoiceDao().deleteWithItems(id)
}

class BullionRepository(private val db: RKDatabase) {

    fun all(): Flow<List<BullionTransaction>> = db.bullionDao().observeAll()

    /** BUY: stock auto-adds, cash DEBIT (money out). SELL: stock auto-deducts, cash CREDIT. */
    suspend fun save(txn: BullionTransaction): Long {
        val amount = txn.weightMg / 1000 * txn.ratePerGramPaise
        val id = db.bullionDao().insert(txn.copy(amountPaise = amount))
        db.cashLedgerDao().insert(
            CashLedger(
                direction = if (txn.direction == BullionDirection.BUY) CashDirection.DEBIT else CashDirection.CREDIT,
                amountPaise = amount,
                refType = LedgerRefType.BULLION,
                refId = id,
                note = txn.direction.name + " " + txn.metalType.name.lowercase() +
                        " " + com.rkjewellery.app.util.FormatUtils.gramsText(txn.weightMg) + "g" +
                        (if (txn.partyName.isNotBlank()) " · " + txn.partyName else "")
            )
        )
        return id
    }

    suspend fun delete(id: Long) = db.bullionDao().delete(id)
}

class RateRepository(private val db: RKDatabase) {
    fun latest(): Flow<com.rkjewellery.app.data.local.entity.MetalRate?> = db.metalRateDao().observeLatest()
    fun all(): Flow<List<com.rkjewellery.app.data.local.entity.MetalRate>> = db.metalRateDao().observeAll()
    suspend fun save(rate: com.rkjewellery.app.data.local.entity.MetalRate) = db.metalRateDao().insert(rate)
}

class CreditRepository(private val db: RKDatabase) {

    fun members(): Flow<List<com.rkjewellery.app.data.local.entity.CreditMember>> = db.creditDao().observeMembers()
    fun member(id: Long): Flow<com.rkjewellery.app.data.local.entity.CreditMember?> = db.creditDao().observeMember(id)
    fun txns(id: Long): Flow<List<CreditTransaction>> = db.creditDao().observeTxns(id)
    fun totalPendingCash(): Flow<Long> = db.creditDao().observeTotalPendingCash()

    suspend fun addMember(member: com.rkjewellery.app.data.local.entity.CreditMember): Long =
        db.creditDao().insertMember(member)

    suspend fun deleteMember(id: Long) = db.creditDao().deleteMember(id)

    suspend fun addTxn(txn: CreditTransaction): Long = db.creditDao().insertTxn(txn)

    data class PendingTotals(val cashPaise: Long, val goldMg: Long, val silverMg: Long)

    suspend fun pendingTotals(memberId: Long): PendingTotals = PendingTotals(
        cashPaise = db.creditDao().pendingCashPaise(memberId),
        goldMg = db.creditDao().pendingGoldMg(memberId),
        silverMg = db.creditDao().pendingSilverMg(memberId)
    )
}
