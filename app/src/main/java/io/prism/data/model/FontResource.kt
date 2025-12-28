package io.prism.data.model

import androidx.annotation.FontRes
import androidx.annotation.StringRes


data class FontResource(
    val id: String,
    @StringRes val nameResId: Int,
    @FontRes val fontResId: Int
)