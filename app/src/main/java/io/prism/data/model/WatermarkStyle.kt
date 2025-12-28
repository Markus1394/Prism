package io.prism.data.model

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes


data class WatermarkStyle(
    val id: String,
    @StringRes val nameResId: Int = 0,
    val customName: String? = null,
    @LayoutRes val horizontalLayoutResId: Int = 0,
    @LayoutRes val verticalLayoutResId: Int = 0,
    @StringRes val descriptionResId: Int? = null,
    val customDescription: String? = null,
    val customTemplateJson: String? = null,
    val supportedOrientations: List<WatermarkOrientation> = listOf(
        WatermarkOrientation.HORIZONTAL,
        WatermarkOrientation.VERTICAL
    ),
    val isCustom: Boolean = false
) {
    val displayName: String
        get() = customName ?: ""

    val displayDescription: String?
        get() = customDescription

    fun supportsOrientation(orientation: WatermarkOrientation): Boolean {
        return orientation in supportedOrientations
    }
}