package io.prism.data.repository

import android.content.Context
import io.prism.data.local.CustomLogoStorage
import io.prism.data.local.CustomTemplateStorage
import io.prism.data.model.*

class WatermarkRepository(context: Context) {

    private val customLogoStorage = CustomLogoStorage(context)
    private val customTemplateStorage = CustomTemplateStorage(context)

    fun getDefaultConfig(): WatermarkConfig {
        return WatermarkConfig(
            mainText = "",
            useExifData = true,
            mainTextFont = ResourceRegistry.defaultFont,
            exifTextFont = ResourceRegistry.defaultFont,
            position = WatermarkPosition.BOTTOM,
            logo = ResourceRegistry.defaultLogo,
            theme = WatermarkTheme.LIGHT,
            style = ResourceRegistry.defaultStyle,
            scalePercent = 10f
        )
    }

    fun getAllFonts(): List<FontResource> = ResourceRegistry.fonts

    fun getBuiltInLogos(): List<LogoResource> = ResourceRegistry.builtInLogos

    suspend fun getAllLogos(): List<LogoResource> {
        val customLogos = customLogoStorage.getAllCustomLogos().map { it.toLogoResource() }
        return ResourceRegistry.builtInLogos + customLogos
    }

    fun getBuiltInStyles(): List<WatermarkStyle> = ResourceRegistry.styles

    suspend fun getAllStyles(): List<WatermarkStyle> {
        val customStyles = customTemplateStorage.getAllCustomTemplates().map { it.toWatermarkStyle() }
        return ResourceRegistry.styles + customStyles
    }

    // Custom logos
    suspend fun addCustomLogo(
        context: Context,
        uri: android.net.Uri,
        name: String,
        isMonochrome: Boolean
    ): LogoResource {
        val customLogo = customLogoStorage.saveCustomLogo(uri, name, isMonochrome)
        return customLogo.toLogoResource()
    }

    suspend fun deleteCustomLogo(id: String) {
        customLogoStorage.deleteCustomLogo(id)
    }

    suspend fun updateCustomLogo(logo: CustomLogo) {
        customLogoStorage.updateCustomLogo(logo)
    }

    // Custom templates
    suspend fun addCustomTemplate(
        name: String,
        description: String,
        templateJson: String,
        orientation: WatermarkOrientation
    ): WatermarkStyle {
        val template = customTemplateStorage.saveCustomTemplate(
            name, description, templateJson, orientation
        )
        return template.toWatermarkStyle()
    }

    suspend fun deleteCustomTemplate(id: String) {
        customTemplateStorage.deleteCustomTemplate(id)
    }

    fun getAllPositions(): List<WatermarkPosition> = WatermarkPosition.entries.toList()
}