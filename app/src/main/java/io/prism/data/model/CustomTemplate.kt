package io.prism.data.model

import java.util.UUID


data class CustomTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val templateJson: String, 
    val orientation: WatermarkOrientation,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toWatermarkStyle(): WatermarkStyle {
        return WatermarkStyle(
            id = id,
            customName = name,
            customDescription = description,
            customTemplateJson = templateJson,
            supportedOrientations = listOf(orientation),
            isCustom = true
        )
    }
}


data class TemplateElement(
    val type: String, 
    val size: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val marginStart: Int? = null,
    val marginEnd: Int? = null,
    val marginTop: Int? = null,
    val marginBottom: Int? = null,
    val bold: Boolean = false,
    val alpha: Float = 1f,
    val children: List<TemplateElement>? = null
)

data class TemplateDefinition(
    val layout: String, 
    val backgroundColor: String? = null, 
    val padding: Int = 24,
    val elements: List<TemplateElement>
)