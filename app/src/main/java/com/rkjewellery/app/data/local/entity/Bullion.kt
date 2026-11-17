package com.rkjewellery.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rkjewellery.app.data.local.Converters
import com.rkjewellery.app.data.local.entity.CashDirection
import com.rkjewellery.app.data.local.entity.LedgerRefType
import com.rkjewellery.app.data.local.entity.BullionDirection
import com.rkjewellery.app.data.local.entity.MetalType
import com.rkjewellery.app.data.local.entity.Purity

/** Single-entry cash book of the shop. DEBIT = cash out, CREDIT = cash in. */
@Entity(tableName = "cash_ledger")
@TypeConverters(Converters::class)
data class CashLedger(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val direction: CashDirection,
    val amountPaise: Long,
    val refType: LedgerRefType,
    val refId: Long = 0,
    val note: String = ""
)

/** Raw bullion (gold/silver) buy & sell transactions, linked to daily rate. */
@Entity(tableName = "bullion_txns")
@TypeConverters(Converters::class)
data class BullionTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val direction: BullionDirection,            // BUY adds stock+cash out, SELL removes stock+cash in
    val metalType: MetalType,
    val purity: Purity,
    val weightMg: Long,
    val ratePerGramPaise: Long,
    val amountPaise: Long,                      // weight_g * rate
    val partyName: String = "",
    val notes: String = ""
)

/** Udhaar Khata member (credited customer). */
@Entity(tableName = "credit_members")
data class CreditMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * Khata ledger line. Only one of cash/gold/silver is typically non-zero per line.
 * When the customer TAKES something, pending increases (CASH_TAKEN etc.);
 * when they RETURN, pending decreases.
 */
@Entity(tableName = "credit_txns")
@TypeConverters(Converters::class)
data class CreditTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val date: Long = System.currentTimeMillis(),
    val type: com.rkjewellery.app.data.local.entity.CreditTxnType,
    val cashPaise: Long = 0,
    val goldMg: Long = 0,
    val silverMg: Long = 0,
    val note: String = ""
)
