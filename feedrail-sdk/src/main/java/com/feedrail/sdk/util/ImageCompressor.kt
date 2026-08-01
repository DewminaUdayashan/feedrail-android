package com.feedrail.sdk.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * Downscales and re-encodes user-picked images before upload.
 *
 * Phone cameras produce 4–12MB JPEGs at resolutions far beyond what the feedback
 * UI ever renders, so uploading the original wastes the server's disk (attachments
 * are stored on local disk, not object storage) and the user's bandwidth. Shrinking
 * to [MAX_DIMENSION] on the long edge at [JPEG_QUALITY] typically cuts a camera
 * photo to a few hundred KB with no visible loss at display size.
 *
 * Non-image types and GIFs are returned untouched — re-encoding a GIF through
 * Bitmap would silently drop its animation.
 */
internal object ImageCompressor {

    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 80

    /** Result of a compression attempt: the bytes to upload and their true type. */
    data class Result(val bytes: ByteArray, val mimeType: String, val fileName: String)

    fun compress(context: Context, uri: Uri, mimeType: String, fileName: String): Result? {
        val original = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null

        if (!mimeType.startsWith("image/") || mimeType == "image/gif") {
            return Result(original, mimeType, fileName)
        }

        val bitmap = decodeScaled(original) ?: return Result(original, mimeType, fileName)
        val oriented = applyExifRotation(context, uri, bitmap)

        val out = ByteArrayOutputStream()
        oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        oriented.recycle()
        val compressed = out.toByteArray()

        // A tiny PNG screenshot can grow when forced through JPEG — keep whichever
        // is actually smaller so compression never makes the upload worse.
        if (compressed.size >= original.size) return Result(original, mimeType, fileName)

        return Result(compressed, "image/jpeg", replaceExtensionWithJpg(fileName))
    }

    /**
     * Two-pass decode: read bounds only, pick a power-of-two [inSampleSize] so the
     * decoded bitmap stays near the target, then scale precisely. This keeps a large
     * photo from being fully decoded into memory just to be shrunk.
     */
    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
        if (longEdge <= MAX_DIMENSION) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        var sample = 1
        while (longEdge / (sample * 2) >= MAX_DIMENSION) sample *= 2

        val decoded = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null

        val decodedLongEdge = maxOf(decoded.width, decoded.height)
        if (decodedLongEdge <= MAX_DIMENSION) return decoded

        val ratio = MAX_DIMENSION.toFloat() / decodedLongEdge
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * ratio).toInt().coerceAtLeast(1),
            (decoded.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled != decoded) decoded.recycle()
        return scaled
    }

    /**
     * JPEG encoding drops EXIF, so a photo taken in portrait would upload sideways.
     * Bake the orientation into the pixels before re-encoding.
     */
    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) {
            0f
        }
        if (degrees == 0f) return bitmap

        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun replaceExtensionWithJpg(fileName: String): String {
        val base = fileName.substringBeforeLast('.', fileName)
        return "$base.jpg"
    }
}
