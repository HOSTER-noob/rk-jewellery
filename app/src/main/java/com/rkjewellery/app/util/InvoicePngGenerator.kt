package com.rkjewellery.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.rkjewellery.app.data.local.entity.Invoice
import com.rkjewellery.app.data.local.entity.InvoiceItem

/**
 * Programmatically renders the minimalist invoice template (cream paper, script-style
 * "Invoice" header, thin-ruled item table, right-aligned totals, Thank-You footer)
 * into a shareable PNG Bitmap — matching the "Minimalist Invoice Template" reference.
 */
object InvoicePngGenerator {

    // Template colours (deep red ink on warm paper, per the reference)
    private const val PAPER = 0xFFF7F3EC.toInt()
    private const val ACCENT = 0xFF8A2A2A.toInt()
    private const val INK = 0xFF26221D.toInt()
    private const val MUTED = 0xFF6E675C.toInt()
    private const val RULE = 0xFF2B2620.toInt()

    private const val W = 1080
    private const val M = 72            // page margin
    private val CONTENT_W = W - M * 2

    fun generate(invoice: Invoice, items: List<InvoiceItem>): Bitmap {
        val rowH = 92
        val height = 2100.coerceAtLeast(
            1180 + items.size * rowH + 520
        )
        val bitmap = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(PAPER)

        val script = Typeface.create(Typeface.SERIF, Typeface.ITALIC or Typeface.BOLD)
        val serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        val serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

        var y = 90f

        // ---- Decorative corner accents (replacing leaf illustration) ----
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT; alpha = 60 }
        val corner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT; alpha = 46
            shader = LinearGradient(0f, 0f, 260f, 260f, ACCENT, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawArc(RectF(-90f, -90f, 230f, 230f), 0f, 360f, true, corner)
        val corner2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT; alpha = 40
            shader = LinearGradient(W.toFloat(), height.toFloat(), W - 280f, height - 280f, ACCENT, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawArc(RectF(W - 230f, height - 230f, W + 90f, height + 90f), 0f, 360f, true, corner2)

        // ---- Script "Invoice" title ----
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT; typeface = script; textSize = 168f; textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Invoice", W / 2f, y + 150f, title)
        y += 210f

        // ---- Shop brand ----
        val brand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK; typeface = sansBold; textSize = 34f; textAlign = Paint.Align.CENTER
            letterSpacing = 0.28f
        }
        canvas.drawText("RK  JEWELLERY", W / 2f, y, brand)
        y += 44f
        val tagline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT; typeface = sans; textSize = 24f; textAlign = Paint.Align.CENTER
            letterSpacing = 0.2f
        }
        val typeLine = if (invoice.type == com.rkjewellery.app.data.local.entity.InvoiceType.SALE) "SALE  INVOICE" else "PURCHASE  INVOICE"
        canvas.drawText(typeLine, W / 2f, y, tagline)
        y += 110f

        // ---- Invoice To (left) | Invoice No / Date (right) ----
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sans; textSize = 30f }
        val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = serifBold; textSize = 40f }
        val right = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sans; textSize = 30f; textAlign = Paint.Align.RIGHT }
        val rightBig = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = serifBold; textSize = 38f; textAlign = Paint.Align.RIGHT }

        canvas.drawText("Invoice To :", M.toFloat(), y, label)
        canvas.drawText("Invoice No. " + invoice.invoiceNo, W - M.toFloat(), y, rightBig)
        y += 56f
        canvas.drawText(invoice.customerName.uppercase(), M.toFloat(), y, big)
        canvas.drawText("Date : " + FormatUtils.date(invoice.date), W - M.toFloat(), y, right)
        y += 52f
        if (invoice.customerPhone.isNotBlank()) {
            val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; typeface = sans; textSize = 26f }
            canvas.drawText("Ph : " + invoice.customerPhone, M.toFloat(), y, muted)
        }
        y += 60f

        // ---- Table header ----
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RULE; strokeWidth = 3f }
        val head = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sansBold; textSize = 30f; letterSpacing = 0.06f }
        val colItem = M.toFloat()
        val colQty = (M + CONTENT_W * 0.52).toFloat()
        val colWeight = (M + CONTENT_W * 0.66).toFloat()
        val colRate = (M + CONTENT_W * 0.80).toFloat()
        val colTotal = (W - M).toFloat()

        canvas.drawLine(M.toFloat(), y, (W - M).toFloat(), y, rule)
        y += 58f
        canvas.drawText("DESCRIPTION", colItem, y, head)
        canvas.drawText("QTY", colQty, y, head)
        canvas.drawText("WEIGHT", colWeight, y, head)
        canvas.drawText("RATE/GM", colRate, y, head)
        canvas.drawText("TOTAL", colTotal, y, head.apply { textAlign = Paint.Align.RIGHT })
        head.textAlign = Paint.Align.LEFT
        y += 30f
        canvas.drawLine(M.toFloat(), y, (W - M).toFloat(), y, rule)
        y += 64f

        // ---- Item rows ----
        val cell = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sans; textSize = 30f }
        val cellBold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sansBold; textSize = 30f }
        val cellRight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sans; textSize = 30f; textAlign = Paint.Align.RIGHT }

        for (item in items) {
            val desc = item.itemName + "  (" + item.metalType.name.lowercase().replaceFirstChar { it.uppercase() } +
                    " · " + item.purity.label + ")"
            canvas.drawText(ellipsize(desc, 34), colItem, y, cell)
            canvas.drawText(item.qty.toString(), colQty, y, cell)
            canvas.drawText(FormatUtils.gramsText(item.chargeableWeightMg) + " g", colWeight, y, cell)
            canvas.drawText(FormatUtils.money(item.ratePerGramPaise, false), colRate, y, cell)
            canvas.drawText(FormatUtils.money(item.lineTotalPaise, false), colTotal, y, cellRight)
            y += 34f
            val mk = "Making: " + FormatUtils.money(item.makingChargesPaise, false) +
                    (if (item.wastagePercent > 0) "  ·  Wastage " + item.wastagePercent + "%" else "")
            val mkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; typeface = sans; textSize = 23f }
            canvas.drawText(mk, colItem, y, mkPaint)
            y += rowH - 34f
        }
        canvas.drawLine(M.toFloat(), y, (W - M).toFloat(), y, rule)
        y += 80f

        // ---- Totals ----
        val totalLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sansBold; textSize = 30f; textAlign = Paint.Align.RIGHT }
        val totalValue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sans; textSize = 30f; textAlign = Paint.Align.RIGHT }
        val lx = (W - M - CONTENT_W * 0.30).toFloat()
        val vx = (W - M).toFloat()

        fun drawTotal(label: String, value: String, bold: Boolean = false, accent: Boolean = false) {
            val p = if (bold || accent) totalLabel else totalLabel.apply { }
            p.color = if (accent) ACCENT else INK
            canvas.drawText(label, lx, y, p)
            val v = Paint(cellRight).apply { textSize = if (bold || accent) 34f else 30f; typeface = if (bold || accent) serifBold else sans }
            v.color = if (accent) ACCENT else INK
            canvas.drawText(value, vx, y, v)
            y += 52f
        }

        drawTotal("SUB TOTAL", FormatUtils.money(invoice.itemsSubtotalPaise, false))
        if (invoice.discountPaise > 0) drawTotal("DISCOUNT", "-" + FormatUtils.money(invoice.discountPaise, false))
        drawTotal("GST (" + trimZero(invoice.gstPercent) + "%)", FormatUtils.money(invoice.gstAmountPaise, false))
        drawTotal("TOTAL", FormatUtils.money(invoice.totalPaise, false), accent = true)
        if (invoice.paidPaise > 0) {
            drawTotal("PAID", FormatUtils.money(invoice.paidPaise, false))
            drawTotal("BALANCE DUE", FormatUtils.money(invoice.totalPaise - invoice.paidPaise, false), accent = true)
        }
        y += 40f

        // ---- Payment footer info ----
        val payHead = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sansBold; textSize = 30f }
        canvas.drawText("Send Payment To :", M.toFloat(), y, payHead)
        y += 46f
        val payBody = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sans; textSize = 26f; letterSpacing = 0.02f }
        canvas.drawText("Payee : RK Jewellery", M.toFloat(), y, payBody)
        y += 38f
        canvas.drawText("UPI : rkjewellery@upi", M.toFloat(), y, payBody)
        y += 60f

        // ---- Thank You footer ----
        val thanks = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT; typeface = script; textSize = 96f; textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Thank You", M.toFloat() + 20f, y + 90f, thanks)
        val foot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = sans; textSize = 26f; textAlign = Paint.Align.RIGHT }
        var fy = y + 50f
        canvas.drawText("RK Jewellery · Main Bazaar Road", vx, fy, foot); fy += 36f
        canvas.drawText("+91-98765-43210", vx, fy, foot); fy += 36f
        canvas.drawText("GSTIN : 08ABCDE1234F1Z5", vx, fy, foot)
        if (invoice.notes.isNotBlank()) {
            val note = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; typeface = sans; textSize = 24f; textAlign = Paint.Align.LEFT }
            canvas.drawText(ellipsize("Note: " + invoice.notes, 64), M.toFloat(), y + 130f, note)
        }
        return bitmap
    }

    private fun ellipsize(text: String, max: Int): String =
        if (text.length <= max) text else text.take(max - 1) + "…"

    private fun trimZero(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
}
