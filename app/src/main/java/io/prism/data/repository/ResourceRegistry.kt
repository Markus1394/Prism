package io.prism.data.repository

import io.prism.R
import io.prism.data.model.FontResource
import io.prism.data.model.LogoResource
import io.prism.data.model.WatermarkStyle

/**
 * Central registry for all static resources.
 */
object ResourceRegistry {

    // ==================== FONTS ====================

    val fonts: List<FontResource> = listOf(
        FontResource(
            id = "roboto_regular",
            nameResId = R.string.font_roboto,
            fontResId = R.font.roboto_regular
        ),
        FontResource(
            id = "roboto_bold",
            nameResId = R.string.font_roboto_bold,
            fontResId = R.font.roboto_bold
        ),
        FontResource(
            id = "playfair",
            nameResId = R.string.font_playfair,
            fontResId = R.font.playfair_display
        ),
        FontResource(
            id = "playfair_bold",
            nameResId = R.string.font_playfair_bold,
            fontResId = R.font.playfairdisplay_bold
        ),
        FontResource(
            id = "badscript_regular",
            nameResId = R.string.badscript_regular,
            fontResId = R.font.badscript_regular
        ),
//        FontResource(
//            id = "montserrat",
//            nameResId = R.string.font_montserrat,
//            fontResId = R.font.montserrat_regular
//        ),
        FontResource(
            id = "oswald",
            nameResId = R.string.font_oswald,
            fontResId = R.font.oswald_regular
        ),
        FontResource(
            id = "jetbrains_mono",
            nameResId = R.string.font_jetbrains,
            fontResId = R.font.jetbrains_mono
        ),
        FontResource(
            id = "jetbrains_mono_bold",
            nameResId = R.string.font_jetbrains_bold,
            fontResId = R.font.jetbrains_mono_bold
        )
    )

    val defaultFont: FontResource = fonts.find { it.id == "roboto_regular" } ?: fonts.first()

    // ==================== LOGOS ====================

    val builtInLogos: List<LogoResource> = listOf(
        LogoResource.NO_LOGO.copy(nameResId = R.string.logo_none), // "Без лого"
        LogoResource(
            id = "prism",
            nameResId = R.string.logo_prism,
            drawableResId = R.drawable.logo_prism,
            isMonochrome = false
        )
    )

    val defaultLogo: LogoResource = builtInLogos.find { it.id == "prism" } ?: builtInLogos[1]

    // ==================== STYLES ====================

    val styles: List<WatermarkStyle> = listOf(
        WatermarkStyle(
            id = "standard",
            nameResId = R.string.style_standard,
            horizontalLayoutResId = R.layout.watermark_horizontal_standard,
            verticalLayoutResId = R.layout.watermark_vertical_standard,
            descriptionResId = R.string.style_standard_desc
        ),
        WatermarkStyle(
            id = "minimal",
            nameResId = R.string.style_minimal,
            horizontalLayoutResId = R.layout.watermark_horizontal_minimal,
            verticalLayoutResId = R.layout.watermark_vertical_minimal,
            descriptionResId = R.string.style_minimal_desc
        ),
        WatermarkStyle(
            id = "classic",
            nameResId = R.string.style_classic,
            horizontalLayoutResId = R.layout.watermark_horizontal_classic,
            verticalLayoutResId = R.layout.watermark_vertical_classic,
            descriptionResId = R.string.style_classic_desc
        )
    )

    val defaultStyle: WatermarkStyle = styles.find { it.id == "classic" } ?: styles.first()

    // ==================== HELPER FUNCTIONS ====================

    fun findFontById(id: String): FontResource? = fonts.find { it.id == id }
    fun findLogoById(id: String): LogoResource? = builtInLogos.find { it.id == id }
    fun findStyleById(id: String): WatermarkStyle? = styles.find { it.id == id }
}