package com.rkjewellery.app.data.local

import androidx.room.TypeConverter
import com.rkjewellery.app.data.local.entity.BullionDirection
import com.rkjewellery.app.data.local.entity.CashDirection
import com.rkjewellery.app.data.local.entity.CreditTxnType
import com.rkjewellery.app.data.local.entity.GirviStatus
import com.rkjewellery.app.data.local.entity.InvoiceType
import com.rkjewellery.app.data.local.entity.LedgerRefType
import com.rkjewellery.app.data.local.entity.MetalType
import com.rkjewellery.app.data.local.entity.Purity

class Converters {
    @TypeConverter fun metalToString(m: MetalType?): String = m?.name ?: MetalType.GOLD.name
    @TypeConverter fun stringToMetal(s: String?): MetalType = runCatching { MetalType.valueOf(s ?: "GOLD") }.getOrDefault(MetalType.GOLD)

    @TypeConverter fun purityToString(p: Purity?): String = p?.name ?: Purity.N22.name
    @TypeConverter fun stringToPurity(s: String?): Purity = runCatching { Purity.valueOf(s ?: "N22") }.getOrDefault(Purity.N22)

    @TypeConverter fun girviStatusToString(s: GirviStatus?): String = s?.name ?: GirviStatus.ACTIVE.name
    @TypeConverter fun stringToGirviStatus(s: String?): GirviStatus = runCatching { GirviStatus.valueOf(s ?: "ACTIVE") }.getOrDefault(GirviStatus.ACTIVE)

    @TypeConverter fun invoiceTypeToString(t: InvoiceType?): String = t?.name ?: InvoiceType.SALE.name
    @TypeConverter fun stringToInvoiceType(s: String?): InvoiceType = runCatching { InvoiceType.valueOf(s ?: "SALE") }.getOrDefault(InvoiceType.SALE)

    @TypeConverter fun cashDirectionToString(d: CashDirection?): String = d?.name ?: CashDirection.CREDIT.name
    @TypeConverter fun stringToCashDirection(s: String?): CashDirection = runCatching { CashDirection.valueOf(s ?: "CREDIT") }.getOrDefault(CashDirection.CREDIT)

    @TypeConverter fun ledgerRefToString(r: LedgerRefType?): String = r?.name ?: LedgerRefType.MANUAL.name
    @TypeConverter fun stringToLedgerRef(s: String?): LedgerRefType = runCatching { LedgerRefType.valueOf(s ?: "MANUAL") }.getOrDefault(LedgerRefType.MANUAL)

    @TypeConverter fun bullionDirectionToString(d: BullionDirection?): String = d?.name ?: BullionDirection.BUY.name
    @TypeConverter fun stringToBullionDirection(s: String?): BullionDirection = runCatching { BullionDirection.valueOf(s ?: "BUY") }.getOrDefault(BullionDirection.BUY)

    @TypeConverter fun creditTxnTypeToString(t: CreditTxnType?): String = t?.name ?: CreditTxnType.CASH_TAKEN.name
    @TypeConverter fun stringToCreditTxnType(s: String?): CreditTxnType = runCatching { CreditTxnType.valueOf(s ?: "CASH_TAKEN") }.getOrDefault(CreditTxnType.CASH_TAKEN)
}
