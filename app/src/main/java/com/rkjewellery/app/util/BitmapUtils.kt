package com.rkjewellery.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.File

/**
 * Exact utility functions for converting a Compose/View layout into a Bitmap PNG,
 * saving securely to cache and sharing via FileProvider + WhatsApp.
 */
object BitmapUtils {

    /** Convert any Android View (including a ComposeView rendered inside it) to a Bitmap. */
    fun viewToBitmap(view: View, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        if (view.width <= 0 || view.height <= 0) {
            // Measure a detached view if it has never been laid out.
            view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, config)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    /** Save a bitmap as PNG inside cacheDir/<subdir>. Returns the File. */
    fun savePngToCache(bitmap: Bitmap, cacheDir: File, subdir: String = "images", fileName: String): File {
        val dir = File(cacheDir, subdir).apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return file
    }
}
