# RK Jewellery — Offline Shop Management App (Android)

Production-ready, **offline-first** Android app for a retail jewellery shop, built with **Kotlin + Jetpack Compose + Room (SQLite)**. UI follows the modern card-based dashboard reference (dark ink cards on warm white, rounded corners, centre-FAB bottom navigation). Bills are auto-rendered as a **minimalist PNG invoice** (script "Invoice" header, thin-ruled table, totals, Thank-You footer) and shared straight to **WhatsApp** via FileProvider.

## Modules (all 6 implemented)

| # | Module | How it works |
|---|--------|--------------|
| 1 | **Girvi Entry (In/Out + Auto-Deduct)** | Saving a *Girvi In* inserts the pledge **and automatically posts a DEBIT to the cash ledger** (loan amount leaves the shop's cash). *Girvi Out / Release* settles interest-first → principal, credits cash, and marks the item `RELEASED`. Partial payments set status `PARTIAL` and reduce outstanding principal. |
| 2 | **Bill Invoice (In/Out)** | Sale (Bill Out) and Purchase (Bill In) invoices with item name, weight (grams), rate/gm, making charges, wastage %, qty, GST and totals. Header shows **RK Jewellery**. |
| 3 | **Auto-PNG Invoice + WhatsApp Share** | On save, `InvoicePngGenerator` draws the bill into a 1080px-wide PNG matching the minimalist template. `WhatsAppShareUtils` saves it to `cacheDir/images`, exposes it via **FileProvider**, and launches WhatsApp directly (with `jid` targeting when the customer's phone is known; chooser fallback). |
| 4 | **Gold Purchase & Selling (Bullion)** | Raw gold/silver BUY/SELL trades linked to daily metal rates; BUY adds stock + debits cash, SELL removes stock + credits cash. |
| 5 | **Stock Checking (Inventory)** | Real-time stock per metal + purity in **grams/milligrams**, computed live from invoices + bullion: purchases/buys add, sales deduct automatically the moment a Bill Out is saved. |
| 6 | **Credited Members & Ledger (Udhaar Khata)** | Per-member ledger tracking **pending cash, pending gold grams, pending silver grams** in real time (take/return pairs). An unpaid balance on a sale bill can be posted to a member's khata. |

## Architecture

```
app/src/main/java/com/rkjewellery/app/
├── RKApp.kt                  # Application: DB + repositories
├── MainActivity.kt           # Single-activity Compose host
├── data/
│   ├── local/
│   │   ├── RKDatabase.kt     # Room DB (v1, 9 entities)
│   │   ├── Converters.kt     # Enum <-> String converters
│   │   ├── entity/           # Girvi, GirviPayment, Invoice, InvoiceItem,
│   │   │                     # MetalRate, CashLedger, BullionTransaction,
│   │   │                     # CreditMember, CreditTransaction, Enums
│   │   └── dao/              # GirviDao, InvoiceDao, CashLedgerDao,
│   │                         # MetalRateDao, BullionDao, CreditDao
│   └── repo/                 # GirviRepository + ShopRepositories
│                             # (all business rules live here)
├── ui/
│   ├── theme/                # Ink/Gold palette, typography, 28dp-rounded shapes
│   ├── navigation/RKNavHost.kt
│   ├── components/           # RKBottomBar (centre FAB), HeroCard, RKTopBar,
│   │                         # RKTextField, money/weight string helpers
│   ├── viewmodels/           # Dashboard, Girvi, Invoice, Stock, Khata VMs
│   └── screens/              # Dashboard, Girvi(+detail), Invoices(+create,
│                             # +preview/share), Stock, Khata(+member), CashBook
└── util/
    ├── FormatUtils.kt        # ₹ paise formatting (Indian grouping), mg<->g, dates
    ├── InvoicePngGenerator.kt# Canvas-based minimalist invoice PNG
    ├── BitmapUtils.kt        # viewToBitmap() + savePngToCache()
    └── WhatsAppShareUtils.kt # sharePngToWhatsApp() via FileProvider
```

### Money & weight precision
- Money stored as **Long paise** (₹1 = 100) — no floating point errors.
- Weight stored as **Long milligrams** (1 g = 1000 mg) — grams/mg precision for stock.
- Interest accrual: `principal × rate% × days / 365` (per-entry, interest paid tracked separately).

### Cash ledger invariants
Every cash movement is a single-row `cash_ledger` entry (`CREDIT` = in, `DEBIT` = out) with a `refType` (GIRVI_IN, GIRVI_PAYMENT, GIRVI_RELEASE, SALE, PURCHASE, BULLION, MANUAL). Balance = credits − debits — always in sync because Girvi/Bill/Bullion saves write their ledger row inside the same repository call.

### Key utility functions (as required)

```kotlin
// util/BitmapUtils.kt — exact View-to-Bitmap conversion
BitmapUtils.viewToBitmap(view)                     // measures detached views too
BitmapUtils.savePngToCache(bitmap, cacheDir, "images", fileName)

// util/WhatsAppShareUtils.kt — FileProvider + direct WhatsApp launch
WhatsAppShareUtils.sharePngToWhatsApp(context, bitmap, fileName, phoneWithCountryCode)
// -> ACTION_SEND image/png, EXTRA_STREAM content://com.rkjewellery.app.fileprovider/...,
//    putExtra("jid", "91XXXXXXXXXX@s.whatsapp.net"), FLAG_GRANT_READ_URI_PERMISSION,
//    falls back to system chooser if WhatsApp is absent.
```

FileProvider paths: `@xml/file_paths` → `cache-path images/`; authority `com.rkjewellery.app.fileprovider`.

## Build & Run

1. Open the project in **Android Studio** (Koala or newer, JDK 17+). Studio will sync Gradle (wrapper config included; AGP 8.5.2 / Kotlin 2.0.20 / Room 2.6.1 / Compose BOM 2024.09.03).
2. Sync → Run on a device/emulator with API 24+. No server, no API keys — 100% offline.
3. Optional CLI: `gradle :app:assembleDebug` (needs Android SDK).

### Suggested first-run flow
1. **Stock → Set** today's gold/silver per-gram rates.
2. **Girvi → +** record a pledge — watch cash in hand drop by the loan amount on the dashboard.
3. **Bills → + → Sale Invoice** — add items (weight, rate, making, GST), save → PNG preview → **WhatsApp** share.
4. **Stock** — see grams deducted automatically; **Khata** — add trusted customers and track pending cash/gold/silver.

## Data safety
All data stays in local Room DB `rk_jewellery.db`. Shared bill images live only in app cache (`cache/images`) and are cleaned by `WhatsAppShareUtils.cleanupCache()` after 24 h. Saved PNG copies go to `Pictures/RK Jewellery` via MediaStore (no storage permission needed).
