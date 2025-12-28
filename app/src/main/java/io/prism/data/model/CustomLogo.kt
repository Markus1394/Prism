package io.prism.data.model

import java.util.UUID


data class CustomLogo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filePath: String,
    val isMonochrome: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toLogoResource(): LogoResource {
        return LogoResource(
            id = id,
            customName = name,
            customLogoPath = filePath,
            isMonochrome = isMonochrome,
            isCustom = true
        )
    }
}