package com.rkjewellery.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Converts a bitmap to PNG in cache, exposes it via FileProvider and launches
 * WhatsApp directly to share the image with the customer (falls back to chooser).
 */
object WhatsAppShareUtils {

    /**
     * @param phoneWithCountryCode e.g. "919876543210" (with country code, no +). Optional;
     *        when blank, WhatsApp opens its contact chooser instead of a specific chat.
     */
    fun sharePngToWhatsApp(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "invoice_${System.currentTimeMillis()}.png",
        phoneWithCountryCode: String? = null
    ): Intent {
        // 1. Save to cache securely
        val file = BitmapUtils.savePngToCache(bitmap, context.cacheDir, "images", fileName)

        // 2. Expose via FileProvider
        val uri: android.net.Uri = FileProvider.getUriForFile(
            context, "com.rkjewellery.app.fileprovider", file
        )

        // 3. Build the share intent
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Invoice from RK Jewellery. Thank you for your business!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (!phoneWithCountryCode.isNullOrBlank()) {
                putExtra("jid", "$phoneWithCountryCode@s.whatsapp.net") // direct chat targeting
            }
        }

        // 4. Try WhatsApp first, fall back to any image-sharing app
        val pm = context.packageManager
        val whatsappLaunchers = mutableListOf<Intent>()
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            val candidate = Intent(intent).apply { setPackage(pkg) }
            if (pm.getLaunchIntentForPackage(pkg) != null) {
                whatsappLaunchers.add(candidate)
            }
        }

        val finalIntent: Intent = if (whatsappLaunchers.isNotEmpty()) {
            // Grant read permission explicitly to each resolvable package
            val resolvers = pm.queryIntentActivities(whatsappLaunchers[0], 0)
            for (r in resolvers) {
                context.grantUriPermission(r.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            whatsappLaunchers[0].addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent.createChooser(intent, "Share invoice via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(finalIntent)
        return finalIntent
    }

    /** Deletes cached shared images (call from e.g. app maintenance). */
    fun cleanupCache(cacheDir: File, olderThanMillis: Long = 86_400_000L) {
        val dir = File(cacheDir, "images")
        val cutoff = System.currentTimeMillis() - olderThanMillis
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }
}
