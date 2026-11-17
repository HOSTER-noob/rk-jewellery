package com.rkjewellery.app.data.repo

import com.rkjewellery.app.data.local.RKDatabase
import com.rkjewellery.app.data.local.entity.CashDirection
import com.rkjewellery.app.data.local.entity.CashLedger
import com.rkjewellery.app.data.local.entity.GirviEntry
import com.rkjewellery.app.data.local.entity.GirviPayment
import com.rkjewellery.app.data.local.entity.GirviStatus
import com.rkjewellery.app.data.local.entity.LedgerRefType
import kotlinx.coroutines.flow.Flow

class GirviRepository(private val db: RKDatabase) {

    fun activeGirvis(): Flow<List<GirviEntry>> = db.girviDao().observeActive()
    fun allGirvis(): Flow<List<GirviEntry>> = db.girviDao().observeAll()
    fun girvi(id: Long): Flow<GirviEntry?> = db.girviDao().observeById(id)
    fun payments(id: Long): Flow<List<GirviPayment>> = db.girviDao().observePayments(id)
    fun totalLent(): Flow<Long> = db.girviDao().observeTotalLent()
    fun accruedInterest(now: Long): Flow<Long> = db.girviDao().observeAccruedInterest(now)

    suspend fun nextTicketNo(): String =
        com.rkjewellery.app.util.DocNumberGenerator.next("G", db.girviDao().count())

    /**
     * GIRVI IN — saves the pledge and AUTO-DEDUCTS the loan amount from the shop's
     * cash ledger (cash goes out of the shop when we hand money to the customer).
     */
    suspend fun saveGirviIn(entry: GirviEntry): Long {
        val id = db.girviDao().insert(entry)
        db.cashLedgerDao().insert(
            CashLedger(
                direction = CashDirection.DEBIT,          // money out of shop
                amountPaise = entry.loanAmountPaise,
                refType = LedgerRefType.GIRVI_IN,
                refId = id,
                note = "Girvi ${entry.ticketNo} · ${entry.customerName} · ${entry.metalType.name.lowercase()} loan"
            )
        )
        return id
    }

    /** Interest accrual: principal * rate% * elapsedDays / 365 (rounded to paise). */
    fun interestUpto(entry: GirviEntry, now: Long, uptoPrincipalPaise: Long = entry.loanAmountPaise): Long {
        if (now <= entry.entryDate) return 0
        val days = ((now - entry.entryDate) / 86_400_000L).coerceAtLeast(0)
        val interest = uptoPrincipalPaise * entry.annualInterestPercent / 100.0 * days / 365.0
        return interest.toLong()
    }

    fun interestDue(entry: GirviEntry, now: Long): Long =
        (interestUpto(entry, now, entry.loanAmountPaise) - entry.interestPaidPaise).coerceAtLeast(0)

    /**
     * Payment against a girvi: covers interest first, remainder goes to principal.
     * Cash automatically CREDITS (comes into) the shop ledger. When the outstanding
     * principal reaches zero the pledge is marked RELEASED (item released to customer).
     */
    suspend fun addPayment(girviId: Long, amountPaise: Long, note: String = ""): GirviPayment {
        val entry = db.girviDao().getById(girviId)
            ?: throw IllegalArgumentException("Girvi entry not found")
        val now = System.currentTimeMillis()
        val interestDue = interestDue(entry, now)
        val interestPart = amountPaise.coerceAtMost(interestDue)
        val principalPart = (amountPaise - interestPart).coerceAtMost(entry.outstandingPaise)
        val total = interestPart + principalPart

        val payment = GirviPayment(
            girviId = girviId,
            amountPaise = total,
            principalComponentPaise = principalPart,
            interestComponentPaise = interestPart,
            note = note
        )
        db.girviDao().insertPayment(payment)

        // Cash comes in
        db.cashLedgerDao().insert(
            CashLedger(
                direction = CashDirection.CREDIT,
                amountPaise = total,
                refType = LedgerRefType.GIRVI_PAYMENT,
                refId = girviId,
                note = "Payment on ${entry.ticketNo} (${entry.customerName})"
            )
        )

        // Update entry state
        val newInterestPaid = entry.interestPaidPaise + interestPart
        val newPrincipalPaid = entry.principalPaidPaise + principalPart
        val outstanding = entry.loanAmountPaise - newPrincipalPaid
        val newStatus = when {
            outstanding <= 0 -> GirviStatus.RELEASED           // fully paid -> item released
            principalPart > 0 -> GirviStatus.PARTIAL           // some principal paid
            else -> entry.status
        }
        db.girviDao().update(
            entry.copy(
                interestPaidPaise = newInterestPaid,
                principalPaidPaise = newPrincipalPaid,
                status = newStatus,
                releaseDate = if (newStatus == GirviStatus.RELEASED) now else entry.releaseDate
            )
        )
        return payment
    }

    /**
     * GIRVI OUT (release/settle): closes the entry, records final cash received and
     * releases the pledged item back to the customer.
     */
    suspend fun releaseGirvi(girviId: Long, finalAmountPaise: Long, note: String = "") {
        val entry = db.girviDao().getById(girviId)
            ?: throw IllegalArgumentException("Girvi entry not found")
        val now = System.currentTimeMillis()

        if (finalAmountPaise > 0) {
            val interestDue = interestDue(entry, now)
            val interestPart = finalAmountPaise.coerceAtMost(interestDue + entry.outstandingPaise)
            val principalPart = (finalAmountPaise - interestPart).coerceAtMost(entry.outstandingPaise)
            val total = interestPart + principalPart
            db.girviDao().insertPayment(
                GirviPayment(
                    girviId = girviId,
                    amountPaise = total,
                    principalComponentPaise = principalPart,
                    interestComponentPaise = interestPart,
                    note = note.ifBlank { "Settlement / Release" }
                )
            )
            db.cashLedgerDao().insert(
                CashLedger(
                    direction = CashDirection.CREDIT,
                    amountPaise = total,
                    refType = LedgerRefType.GIRVI_RELEASE,
                    refId = girviId,
                    note = "Release ${entry.ticketNo} (${entry.customerName})"
                )
            )
        }

        db.girviDao().update(
            entry.copy(status = GirviStatus.RELEASED, releaseDate = now)
        )
    }

    /** Marks entry SETTLED (e.g. defaulted & gold retained) without cash movement. */
    suspend fun markSettled(girviId: Long, note: String = "") {
        val entry = db.girviDao().getById(girviId) ?: return
        db.girviDao().update(entry.copy(status = GirviStatus.SETTLED, releaseDate = System.currentTimeMillis()))
    }
}
