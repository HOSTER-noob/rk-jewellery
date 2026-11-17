package com.rkjewellery.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rkjewellery.app.data.local.Converters
import com.rkjewellery.app.data.local.entity.MetalType
import com.rkjewellery.app.data.local.entity.GirviStatus
import com.rkjewellery.app.data.local.entity.Purity

/**
 * Girvi (mortgage/pledge) entry.
 * Money stored as Long paise (1 rupee = 100). Weight stored as Long milligrams (1 g = 1000 mg).
 */
@Entity(tableName = "girvi_entries")
@TypeConverters(Converters::class)
data class GirviEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketNo: String,                       // e.g. G-2026-001
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val metalType: MetalType,
    val purity: Purity,
    val itemName: String,                       // e.g. "Chain", "Bangles"
    val itemCount: Int = 1,
    val grossWeightMg: Long,
    val netWeightMg: Long,
    val loanAmountPaise: Long,                  // principal given to customer
    val annualInterestPercent: Double = 12.0,
    val entryDate: Long = System.currentTimeMillis(),
    val dueDate: Long,
    val status: GirviStatus = GirviStatus.ACTIVE,
    val interestPaidPaise: Long = 0,
    val principalPaidPaise: Long = 0,
    val releaseDate: Long? = null,
    val notes: String = ""
) {
    val outstandingPaise: Long get() = loanAmountPaise - principalPaidPaise
    val isClosed: Boolean get() = status == GirviStatus.RELEASED || status == GirviStatus.SETTLED
}

@Entity(tableName = "girvi_payments")
data class GirviPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val girviId: Long,
    val date: Long = System.currentTimeMillis(),
    val amountPaise: Long,
    val principalComponentPaise: Long,
    val interestComponentPaise: Long,
    val note: String = ""
)
