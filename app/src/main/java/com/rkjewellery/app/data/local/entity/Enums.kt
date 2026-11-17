package com.rkjewellery.app.data.local.entity

enum class MetalType { GOLD, SILVER }

enum class Purity(val label: String, val factor: Double) {
    N24("24K (99.9%)", 0.999), N22("22K (91.6%)", 0.916), N21("21K (87.5%)", 0.875),
    N18("18K (75%)", 0.75), N14("14K (58.5%)", 0.585),
    S999("Silver 999", 0.999), S925("Silver 925", 0.925), S900("Silver 900", 0.9), OTHER("Other", 1.0)
}

enum class GirviStatus { ACTIVE, PARTIAL, RELEASED, SETTLED }

enum class InvoiceType { SALE, PURCHASE }

/** Cash ledger entry types. Sign convention: DEBIT = money goes out of shop, CREDIT = money comes in. */
enum class CashDirection { DEBIT, CREDIT }

enum class LedgerRefType { GIRVI_IN, GIRVI_PAYMENT, GIRVI_RELEASE, SALE, PURCHASE, BULLION, MANUAL }

/** Whether a bullion txn adds or removes stock and cash. */
enum class BullionDirection { BUY, SELL }

/** Udhaar Khata transaction: what the customer took / returned. */
enum class CreditTxnType { CASH_TAKEN, CASH_RETURNED, GOLD_TAKEN, GOLD_RETURNED, SILVER_TAKEN, SILVER_RETURNED }
