package com.example.recommend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Resize (longest side) + JPEG compress before Firebase Storage upload.
 */
object ImageCompress {

    /** Feed / post photos */
    const val POST_MAX_SIDE_PX = 1600
    const val POST_JPEG_QUALITY = 82

    /** Profile avatars */
    const val AVATAR_MAX_SIDE_PX = 512
    const val AVATAR_JPEG_QUALITY = 85

    /**
     * @return JPEG bytes or null if open/decode fails
     */
    fun compressUriToJpeg(
        context: Context,
        uri: Uri,
        maxSidePx: Int,
        quality: Int
    ): ByteArray? {
        val original = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val toEncode: Bitmap = try {
            val w = original.width
            val h = original.height
            val scale = min(maxSidePx.toFloat() / max(w, h), 1f)
            if (scale < 1f) {
                val nw = max(1, (w * scale).roundToInt())
                val nh = max(1, (h * scale).roundToInt())
                val scaled = Bitmap.createScaledBitmap(original, nw, nh, true)
                original.recycle()
                scaled
            } else {
                original
            }
        } catch (_: Exception) {
            if (!original.isRecycled) original.recycle()
            return null
        }

        return try {
            ByteArrayOutputStream().use { out ->
                toEncode.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(50, 95), out)
                out.toByteArray()
            }
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            if (!toEncode.isRecycled) toEncode.recycle()
        }
    }
}
