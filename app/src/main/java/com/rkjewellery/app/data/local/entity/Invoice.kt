package com.rkjewellery.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rkjewellery.app.data.local.Converters
import com.rkjewellery.app.data.local.entity.InvoiceType
import com.rkjewellery.app.data.local.entity.MetalType

/** Master invoice. SALE (Bill Out) and PURCHASE (Bill In). */
@Entity(tableName = "invoices")
@TypeConverters(Converters::class)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNo: String,                      // e.g. INV-2026-0007
    val type: InvoiceType,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val date: Long = System.currentTimeMillis(),
    val itemsSubtotalPaise: Long = 0,           // sum of (net weight * rate + making)
    val makingChargesPaise: Long = 0,           // kept in item totals too; informational
    val gstPercent: Double = 3.0,
    val gstAmountPaise: Long = 0,
    val discountPaise: Long = 0,
    val totalPaise: Long = 0,
    val paidPaise: Long = 0,
    val notes: String = ""
)

/** Line item of an invoice. Weight in milligrams, rate per gram in paise. */
@Entity(tableName = "invoice_items")
@TypeConverters(Converters::class)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long = 0,
    val itemName: String,
    val metalType: MetalType,
    val purity: Purity,
    val weightMg: Long,
    val qty: Int = 1,
    val ratePerGramPaise: Long,                 // metal rate per gram
    val makingChargesPaise: Long,               // total making charges for this line
    val wastagePercent: Double = 0.0            // optional wastage on weight
) {
    val chargeableWeightMg: Long get() = weightMg + ((wastagePercent / 100.0) * weightMg).toLong()
    val metalValuePaise: Long get() = chargeableWeightMg / 1000 * ratePerGramPaise
    val lineTotalPaise: Long get() = metalValuePaise + makingChargesPaise
}

@Entity(tableName = "metal_rates")
data class MetalRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val goldPerGramPaise: Long,
    val silverPerGramPaise: Long,
    val notes: String = ""
)
