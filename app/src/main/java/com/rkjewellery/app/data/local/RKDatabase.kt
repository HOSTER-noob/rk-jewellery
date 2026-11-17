package com.rkjewellery.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rkjewellery.app.data.local.dao.BullionDao
import com.rkjewellery.app.data.local.dao.CashLedgerDao
import com.rkjewellery.app.data.local.dao.CreditDao
import com.rkjewellery.app.data.local.dao.GirviDao
import com.rkjewellery.app.data.local.dao.InvoiceDao
import com.rkjewellery.app.data.local.dao.MetalRateDao
import com.rkjewellery.app.data.local.entity.BullionTransaction
import com.rkjewellery.app.data.local.entity.CashLedger
import com.rkjewellery.app.data.local.entity.CreditMember
import com.rkjewellery.app.data.local.entity.CreditTransaction
import com.rkjewellery.app.data.local.entity.GirviEntry
import com.rkjewellery.app.data.local.entity.GirviPayment
import com.rkjewellery.app.data.local.entity.Invoice
import com.rkjewellery.app.data.local.entity.InvoiceItem
import com.rkjewellery.app.data.local.entity.MetalRate

@Database(
    entities = [
        GirviEntry::class, GirviPayment::class,
        Invoice::class, InvoiceItem::class,
        MetalRate::class, CashLedger::class,
        BullionTransaction::class,
        CreditMember::class, CreditTransaction::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RKDatabase : RoomDatabase() {

    abstract fun girviDao(): GirviDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun cashLedgerDao(): CashLedgerDao
    abstract fun metalRateDao(): MetalRateDao
    abstract fun bullionDao(): BullionDao
    abstract fun creditDao(): CreditDao

    companion object {
        @Volatile private var instance: RKDatabase? = null

        fun get(context: Context): RKDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RKDatabase::class.java,
                    "rk_jewellery.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
