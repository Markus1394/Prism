package io.prism.data.model

data class WatermarkConfig(
    val mainText: String = "",
    val useExifData: Boolean = true,
    val mainTextFont: FontResource,
    val exifTextFont: FontResource,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM,
    val logo: LogoResource?,  
    val theme: WatermarkTheme = WatermarkTheme.LIGHT,
    val style: WatermarkStyle,
    val scalePercent: Float = 10f
) {
    companion object {
        const val MAX_TEXT_LENGTH = 45
        const val MIN_SCALE = 7.5f
        const val MAX_SCALE = 45f
        const val SCALE_STEP = 2.5f
    }
}