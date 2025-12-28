package io.prism.data.model

import androidx.annotation.ColorRes
import io.prism.R

enum class WatermarkTheme(
    @ColorRes val backgroundColor: Int,
    @ColorRes val textColor: Int
) {
    LIGHT(
        backgroundColor = R.color.watermark_bg_light,
        textColor = R.color.watermark_text_dark
    ),
    DARK(
        backgroundColor = R.color.watermark_bg_dark,
        textColor = R.color.watermark_text_light
    )
}