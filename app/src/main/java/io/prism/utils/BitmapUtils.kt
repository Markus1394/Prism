package io.prism.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import io.prism.data.model.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object BitmapUtils {

    
    private const val MAX_PREVIEW_SIZE = 2048

    
    private const val MAX_PROCESSING_SIZE = 8192

    
    suspend fun loadBitmapForPreview(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val dimensions = getImageDimensions(context, uri) ?: return@withContext null
            val (originalWidth, originalHeight) = dimensions

            val sampleSize = calculateSampleSize(originalWidth, originalHeight, MAX_PREVIEW_SIZE)

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    ?: return@withContext null

                handleExifRotation(context, uri, bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }

    
    suspend fun loadBitmapForProcessing(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val dimensions = getImageDimensions(context, uri) ?: return@withContext null
            val (originalWidth, originalHeight) = dimensions

            val maxDimension = max(originalWidth, originalHeight)
            val sampleSize = if (maxDimension > MAX_PROCESSING_SIZE) {
                calculateSampleSize(originalWidth, originalHeight, MAX_PROCESSING_SIZE)
            } else {
                1
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    ?: return@withContext null

                handleExifRotation(context, uri, bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            loadBitmapWithFallback(context, uri)
        }
    }

    private suspend fun loadBitmapWithFallback(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val dimensions = getImageDimensions(context, uri) ?: return@withContext null
            val (originalWidth, originalHeight) = dimensions

            val sampleSize = calculateSampleSize(originalWidth, originalHeight, 4096)

            val options = BitmapFactory.Options().apply {
                inSampleSize = max(sampleSize, 2)
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    ?: return@withContext null
                handleExifRotation(context, uri, bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getActualImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        try {
            val dimensions = getImageDimensions(context, uri) ?: return@withContext null
            var (width, height) = dimensions

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90,
                    ExifInterface.ORIENTATION_ROTATE_270,
                    ExifInterface.ORIENTATION_TRANSVERSE,
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        val temp = width
                        width = height
                        height = temp
                    }
                }
            }

            Pair(width, height)
        } catch (e: Exception) {
            e.printStackTrace()
            getImageDimensions(context, uri)
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        val maxDimension = max(width, height)

        while (maxDimension / sampleSize > maxSize) {
            sampleSize *= 2
        }

        return sampleSize
    }

    private fun handleExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                rotateBitmap(bitmap, orientation)
            } ?: bitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }

    
    fun appendWatermark(
        original: Bitmap,
        watermark: Bitmap,
        position: WatermarkPosition
    ): Bitmap {
        val resultWidth: Int
        val resultHeight: Int
        val originalX: Int
        val originalY: Int
        val watermarkX: Int
        val watermarkY: Int

        when (position) {
            WatermarkPosition.TOP -> {
                resultWidth = maxOf(original.width, watermark.width)
                resultHeight = original.height + watermark.height
                originalX = (resultWidth - original.width) / 2
                originalY = watermark.height
                watermarkX = (resultWidth - watermark.width) / 2
                watermarkY = 0
            }
            WatermarkPosition.BOTTOM -> {
                resultWidth = maxOf(original.width, watermark.width)
                resultHeight = original.height + watermark.height
                originalX = (resultWidth - original.width) / 2
                originalY = 0
                watermarkX = (resultWidth - watermark.width) / 2
                watermarkY = original.height
            }
            WatermarkPosition.LEFT -> {
                resultWidth = original.width + watermark.width
                resultHeight = maxOf(original.height, watermark.height)
                originalX = watermark.width
                originalY = (resultHeight - original.height) / 2
                watermarkX = 0
                watermarkY = (resultHeight - watermark.height) / 2
            }
            WatermarkPosition.RIGHT -> {
                resultWidth = original.width + watermark.width
                resultHeight = maxOf(original.height, watermark.height)
                originalX = 0
                originalY = (resultHeight - original.height) / 2
                watermarkX = original.width
                watermarkY = (resultHeight - watermark.height) / 2
            }
        }

        val result = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        canvas.drawBitmap(original, originalX.toFloat(), originalY.toFloat(), null)
        canvas.drawBitmap(watermark, watermarkX.toFloat(), watermarkY.toFloat(), null)

        return result
    }
}