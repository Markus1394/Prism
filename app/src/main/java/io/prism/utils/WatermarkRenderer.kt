package io.prism.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import io.prism.R
import io.prism.data.model.ExifData
import io.prism.data.model.WatermarkConfig
import io.prism.data.model.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object WatermarkRenderer {

    
    private const val BASE_LOGO_SIZE_DP = 48f
    private const val BASE_TEXT_SIZE_SP = 18f
    private const val BASE_EXIF_TEXT_SIZE_SP = 12f
    private const val BASE_PADDING_DP = 24f
    private const val BASE_MARGIN_DP = 16f
    private const val BASE_DIVIDER_WIDTH_DP = 2f
    private const val BASE_DIVIDER_HEIGHT_DP = 40f

    suspend fun renderWatermark(
        context: Context,
        config: WatermarkConfig,
        exifData: ExifData,
        imageWidth: Int,
        imageHeight: Int
    ): Bitmap = withContext(Dispatchers.Main) {

        
        val isHorizontal = config.position.isHorizontal()

        val layoutResId = if (isHorizontal) {
            config.style.horizontalLayoutResId
        } else {
            config.style.verticalLayoutResId
        }

        
        if (layoutResId == 0) {
            
            val fallbackLayoutResId = if (isHorizontal) {
                R.layout.watermark_horizontal_standard
            } else {
                R.layout.watermark_vertical_standard
            }
            return@withContext renderWithLayout(
                context, fallbackLayoutResId, config, exifData, imageWidth, imageHeight, isHorizontal
            )
        }

        renderWithLayout(context, layoutResId, config, exifData, imageWidth, imageHeight, isHorizontal)
    }

    private suspend fun renderWithLayout(
        context: Context,
        layoutResId: Int,
        config: WatermarkConfig,
        exifData: ExifData,
        imageWidth: Int,
        imageHeight: Int,
        isHorizontal: Boolean
    ): Bitmap = withContext(Dispatchers.Main) {
        val inflater = LayoutInflater.from(context)
        val watermarkView = inflater.inflate(layoutResId, null) as ViewGroup

        
        val scaleFactor = calculateScaleFactor(
            context, imageWidth, imageHeight, config.scalePercent, isHorizontal
        )

        
        configureView(context, watermarkView, config, exifData, scaleFactor)

        
        measureAndLayoutView(watermarkView, isHorizontal, imageWidth, imageHeight)

        renderViewToBitmap(watermarkView)
    }

    
    private fun calculateScaleFactor(
        context: Context,
        imageWidth: Int,
        imageHeight: Int,
        scalePercent: Float,
        isHorizontal: Boolean
    ): Float {
        val density = context.resources.displayMetrics.density

        
        val referenceSize = if (isHorizontal) imageHeight else imageWidth

        
        val targetSize = referenceSize * scalePercent / 100f

        
        val baseSize = 100f * density

        
        return (targetSize / baseSize).coerceIn(0.5f, 10f)
    }

    private fun configureView(
        context: Context,
        view: ViewGroup,
        config: WatermarkConfig,
        exifData: ExifData,
        scaleFactor: Float
    ) {
        val density = context.resources.displayMetrics.density

        
        view.findViewById<View>(R.id.watermarkBG)?.apply {
            setBackgroundColor(ContextCompat.getColor(context, config.theme.backgroundColor))
            val scaledPadding = (BASE_PADDING_DP * density * scaleFactor).toInt()
            setPadding(scaledPadding, scaledPadding, scaledPadding, scaledPadding)
        }

        
        view.findViewById<ImageView>(R.id.watermarkLogo)?.apply {
            val logo = config.logo

            if (logo == null || logo.isNoLogo) {
                
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE

                
                when {
                    logo.isCustom && !logo.customLogoPath.isNullOrEmpty() -> {
                        val file = File(logo.customLogoPath)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            setImageBitmap(bitmap)
                        }
                    }
                    logo.drawableResId != 0 -> {
                        setImageResource(logo.drawableResId)
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }

                
                if (visibility == View.VISIBLE && logo.isMonochrome) {
                    val textColor = ContextCompat.getColor(context, config.theme.textColor)
                    setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
                } else {
                    colorFilter = null
                }

                
                if (visibility == View.VISIBLE) {
                    val scaledLogoSize = (BASE_LOGO_SIZE_DP * density * scaleFactor).toInt()
                    layoutParams = layoutParams?.apply {
                        width = scaledLogoSize
                        height = scaledLogoSize
                    } ?: LinearLayout.LayoutParams(scaledLogoSize, scaledLogoSize)

                    (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                        val scaledMargin = (BASE_MARGIN_DP * density * scaleFactor).toInt()
                        marginEnd = scaledMargin
                    }
                }
            }
        }

        
        view.findViewWithTag<View>("divider")?.apply {
            val hasLogo = config.logo != null && !config.logo.isNoLogo
            visibility = if (hasLogo) View.VISIBLE else View.GONE

            if (visibility == View.VISIBLE) {
                val scaledWidth = (BASE_DIVIDER_WIDTH_DP * density * scaleFactor).toInt()
                val scaledHeight = (BASE_DIVIDER_HEIGHT_DP * density * scaleFactor).toInt()
                layoutParams = layoutParams?.apply {
                    width = scaledWidth
                    height = scaledHeight
                }

                (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                    val scaledMargin = (BASE_MARGIN_DP * density * scaleFactor).toInt()
                    marginEnd = scaledMargin
                }
            }
        }

        
        view.findViewById<TextView>(R.id.watermarkText)?.apply {
            text = config.mainText.ifBlank { context.getString(R.string.default_watermark_text) }
            setTextColor(ContextCompat.getColor(context, config.theme.textColor))

            val scaledTextSize = BASE_TEXT_SIZE_SP * scaleFactor
            setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTextSize)

            typeface = if (config.mainTextFont.fontResId != 0) {
                try {
                    ResourcesCompat.getFont(context, config.mainTextFont.fontResId)
                } catch (e: Exception) {
                    Typeface.DEFAULT_BOLD
                }
            } else {
                Typeface.DEFAULT_BOLD
            }

            (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                val scaledMargin = (4 * density * scaleFactor).toInt()
                topMargin = scaledMargin
                bottomMargin = scaledMargin
            }
        }

        
        view.findViewById<TextView>(R.id.watermarkEXIFDataText)?.apply {
            val exifText = if (config.useExifData) {
                exifData.formatForWatermark().ifBlank {
                    "" 
                }
            } else {
                ""
            }

            if (exifText.isNotBlank()) {
                text = exifText
                visibility = View.VISIBLE
                setTextColor(ContextCompat.getColor(context, config.theme.textColor))
                alpha = 0.7f

                val scaledTextSize = BASE_EXIF_TEXT_SIZE_SP * scaleFactor
                setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTextSize)

                typeface = if (config.exifTextFont.fontResId != 0) {
                    try {
                        ResourcesCompat.getFont(context, config.exifTextFont.fontResId)
                    } catch (e: Exception) {
                        Typeface.DEFAULT
                    }
                } else {
                    Typeface.DEFAULT
                }

                (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                    val scaledMargin = (4 * density * scaleFactor).toInt()
                    topMargin = scaledMargin
                }
            } else {
                visibility = View.GONE
            }
        }

        
        scaleViewHierarchy(view, scaleFactor, density)
    }

    
    private fun scaleViewHierarchy(view: View, scaleFactor: Float, density: Float) {
        if (view is ViewGroup) {
            for (child in view.children) {
                scaleViewHierarchy(child, scaleFactor, density)
            }
        }

        
        when (view.id) {
            R.id.watermarkBG, R.id.watermarkLogo, R.id.watermarkText, R.id.watermarkEXIFDataText -> return
        }

        
        if (view.tag == "divider") return

        
        val currentPadding = view.paddingLeft + view.paddingTop + view.paddingRight + view.paddingBottom
        if (currentPadding > 0) {
            val scaledPaddingLeft = (view.paddingLeft * scaleFactor).toInt()
            val scaledPaddingTop = (view.paddingTop * scaleFactor).toInt()
            val scaledPaddingRight = (view.paddingRight * scaleFactor).toInt()
            val scaledPaddingBottom = (view.paddingBottom * scaleFactor).toInt()
            view.setPadding(scaledPaddingLeft, scaledPaddingTop, scaledPaddingRight, scaledPaddingBottom)
        }

        
        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            val hasMargins = leftMargin + topMargin + rightMargin + bottomMargin > 0
            if (hasMargins) {
                leftMargin = (leftMargin * scaleFactor).toInt()
                topMargin = (topMargin * scaleFactor).toInt()
                rightMargin = (rightMargin * scaleFactor).toInt()
                bottomMargin = (bottomMargin * scaleFactor).toInt()
            }
        }
    }

    private fun measureAndLayoutView(
        view: View,
        isHorizontal: Boolean,
        imageWidth: Int,
        imageHeight: Int
    ) {
        val widthSpec: Int
        val heightSpec: Int

        if (isHorizontal) {
            
            widthSpec = View.MeasureSpec.makeMeasureSpec(imageWidth, View.MeasureSpec.EXACTLY)
            heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        } else {
            
            widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            heightSpec = View.MeasureSpec.makeMeasureSpec(imageHeight, View.MeasureSpec.EXACTLY)
        }

        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    private suspend fun renderViewToBitmap(view: View): Bitmap = withContext(Dispatchers.Default) {
        val width = view.measuredWidth.coerceAtLeast(1)
        val height = view.measuredHeight.coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        bitmap
    }
}