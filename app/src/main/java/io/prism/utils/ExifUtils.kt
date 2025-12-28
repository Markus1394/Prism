package io.prism.utils

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import io.prism.data.model.ExifData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object ExifUtils {

    suspend fun extractExifData(context: Context, uri: Uri): ExifData = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                parseExif(inputStream)
            } ?: ExifData.EMPTY
        } catch (e: Exception) {
            e.printStackTrace()
            ExifData.EMPTY
        }
    }

    private fun parseExif(inputStream: InputStream): ExifData {
        val exif = ExifInterface(inputStream)

        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()

        
        val cleanedModel = cleanModelName(make, model)

        return ExifData(
            make = make,
            model = cleanedModel,
            iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS),
            focalLength = parseFocalLength(exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)),
            aperture = parseAperture(
                exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                    ?: exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE)
            ),
            exposureTime = parseExposureTime(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)),
            dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
        )
    }

    
    private fun cleanModelName(make: String?, model: String?): String? {
        if (model == null) return null
        if (make == null) return model

        val makeUpper = make.uppercase().trim()
        val modelUpper = model.uppercase().trim()

        
        return if (modelUpper.startsWith(makeUpper)) {
            model.substring(make.length).trim()
        } else if (modelUpper.startsWith("$makeUpper ")) {
            model.substring(make.length + 1).trim()
        } else {
            model
        }
    }

    
    private fun parseFocalLength(value: String?): String? {
        if (value == null) return null
        return try {
            val parts = value.split("/")
            if (parts.size == 2) {
                val numerator = parts[0].trim().toDoubleOrNull() ?: return value
                val denominator = parts[1].trim().toDoubleOrNull() ?: return value

                if (denominator == 0.0) return value

                val result = numerator / denominator

                
                if (result == result.toLong().toDouble()) {
                    
                    result.toLong().toString()
                } else {
                    
                    String.format("%.1f", result).replace(",", ".")
                }
            } else {
                
                val num = value.toDoubleOrNull()
                if (num != null) {
                    if (num == num.toLong().toDouble()) {
                        num.toLong().toString()
                    } else {
                        String.format("%.1f", num).replace(",", ".")
                    }
                } else {
                    value
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            value
        }
    }

    
    private fun parseAperture(value: String?): String? {
        if (value == null) return null
        return try {
            val parts = value.split("/")
            if (parts.size == 2) {
                val numerator = parts[0].trim().toDoubleOrNull() ?: return value
                val denominator = parts[1].trim().toDoubleOrNull() ?: return value

                if (denominator == 0.0) return value

                val result = numerator / denominator
                String.format("%.1f", result).replace(",", ".")
            } else {
                
                val num = value.toDoubleOrNull()
                if (num != null) {
                    String.format("%.1f", num).replace(",", ".")
                } else {
                    value
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            value
        }
    }

    
    private fun parseExposureTime(value: String?): String? {
        if (value == null) return null
        return try {
            val exposure = value.toDoubleOrNull() ?: return value

            when {
                exposure <= 0 -> value
                exposure < 1 -> {
                    
                    val denominator = (1.0 / exposure).toLong()
                    "1/$denominator"
                }
                exposure == exposure.toLong().toDouble() -> {
                    
                    "${exposure.toLong()}"
                }
                else -> {
                    
                    String.format("%.1f", exposure).replace(",", ".")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            value
        }
    }
}