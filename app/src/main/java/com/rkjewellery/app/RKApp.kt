package com.rkjewellery.app

import android.app.Application
import com.rkjewellery.app.data.local.RKDatabase
import com.rkjewellery.app.data.repo.*

class RKApp : Application() {
    lateinit var db: RKDatabase
        private set
    lateinit var girviRepo: GirviRepository
    lateinit var invoiceRepo: InvoiceRepository
    lateinit var bullionRepo: BullionRepository
    lateinit var rateRepo: RateRepository
    lateinit var creditRepo: CreditRepository

    override fun onCreate() {
        super.onCreate()
        db = RKDatabase.get(this)
        girviRepo = GirviRepository(db)
        invoiceRepo = InvoiceRepository(db)
        bullionRepo = BullionRepository(db)
        rateRepo = RateRepository(db)
        creditRepo = CreditRepository(db)
    }
}
