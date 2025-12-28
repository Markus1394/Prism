package io.prism.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes


data class LogoResource(
    val id: String,
    @StringRes val nameResId: Int = 0,
    val customName: String? = null, 
    @DrawableRes val drawableResId: Int = 0,
    val customLogoPath: String? = null, 
    val isMonochrome: Boolean = false,
    val isCustom: Boolean = false,
    val isNoLogo: Boolean = false 
) {
    val displayName: String
        get() = customName ?: ""

    companion object {
        val NO_LOGO = LogoResource(
            id = "no_logo",
            nameResId = 0,
            customName = null,
            drawableResId = 0,
            isNoLogo = true
        )
    }
}