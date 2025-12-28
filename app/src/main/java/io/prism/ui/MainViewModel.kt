package io.prism.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.prism.data.model.*
import io.prism.data.repository.WatermarkRepository
import io.prism.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedImageUri: Uri? = null,
    val previewBitmap: Bitmap? = null,
    val actualImageWidth: Int = 0,
    val actualImageHeight: Int = 0,
    val exifData: ExifData = ExifData.EMPTY,
    val config: WatermarkConfig,
    val allLogos: List<LogoResource> = emptyList(),
    val allStyles: List<WatermarkStyle> = emptyList(),
    val isProcessing: Boolean = false,
    val isLoadingImage: Boolean = false,
    val processingProgress: String = "",
    val savedImageUri: Uri? = null,
    val error: String? = null,
    val showAddLogoDialog: Boolean = false,
    val showAddTemplateDialog: Boolean = false
) {
    val isReadyToProcess: Boolean
        get() = selectedImageUri != null &&
                previewBitmap != null &&
                actualImageWidth > 0 &&
                actualImageHeight > 0 &&
                config.mainText.isNotBlank() &&
                !isProcessing &&
                !isLoadingImage

    val validationErrors: List<String>
        get() = buildList {
            if (selectedImageUri == null) add("Select an image")
            if (config.mainText.isBlank()) add("Enter watermark text")
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WatermarkRepository(application)

    private val _uiState = MutableStateFlow(
        MainUiState(config = repository.getDefaultConfig())
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val fonts: List<FontResource> = repository.getAllFonts()
    val positions: List<WatermarkPosition> = repository.getAllPositions()

    init {
        loadLogosAndStyles()
    }

    private fun loadLogosAndStyles() {
        viewModelScope.launch {
            val logos = repository.getAllLogos()
            val styles = repository.getAllStyles()

            _uiState.update {
                it.copy(
                    allLogos = logos,
                    allStyles = styles
                )
            }
        }
    }

    fun selectImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImage = true, error = null) }

            try {
                val dimensions = BitmapUtils.getActualImageDimensions(getApplication(), uri)

                if (dimensions == null) {
                    _uiState.update {
                        it.copy(
                            isLoadingImage = false,
                            error = "Failed to load image dimensions"
                        )
                    }
                    return@launch
                }

                val (actualWidth, actualHeight) = dimensions
                val previewBitmap = BitmapUtils.loadBitmapForPreview(getApplication(), uri)

                if (previewBitmap == null) {
                    _uiState.update {
                        it.copy(
                            isLoadingImage = false,
                            error = "Failed to load image preview"
                        )
                    }
                    return@launch
                }

                val exifData = ExifUtils.extractExifData(getApplication(), uri)

                _uiState.update {
                    it.copy(
                        selectedImageUri = uri,
                        previewBitmap = previewBitmap,
                        actualImageWidth = actualWidth,
                        actualImageHeight = actualHeight,
                        exifData = exifData,
                        isLoadingImage = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoadingImage = false,
                        error = "Error loading image: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateMainText(text: String) {
        val trimmedText = text.take(WatermarkConfig.MAX_TEXT_LENGTH)
        _uiState.update {
            it.copy(config = it.config.copy(mainText = trimmedText))
        }
    }

    fun updateUseExifData(use: Boolean) {
        _uiState.update {
            it.copy(config = it.config.copy(useExifData = use))
        }
    }

    fun updateMainTextFont(font: FontResource) {
        _uiState.update {
            it.copy(config = it.config.copy(mainTextFont = font))
        }
    }

    fun updateExifTextFont(font: FontResource) {
        _uiState.update {
            it.copy(config = it.config.copy(exifTextFont = font))
        }
    }

    fun updatePosition(position: WatermarkPosition) {
        _uiState.update {
            it.copy(config = it.config.copy(position = position))
        }
    }

    fun updateLogo(logo: LogoResource?) {
        _uiState.update {
            it.copy(config = it.config.copy(logo = if (logo?.isNoLogo == true) null else logo))
        }
    }

    fun updateTheme(theme: WatermarkTheme) {
        _uiState.update {
            it.copy(config = it.config.copy(theme = theme))
        }
    }

    fun updateStyle(style: WatermarkStyle) {
        _uiState.update {
            it.copy(config = it.config.copy(style = style))
        }
    }

    fun updateScale(scale: Float) {
        _uiState.update {
            it.copy(config = it.config.copy(scalePercent = scale))
        }
    }

    
    fun showAddLogoDialog() {
        _uiState.update { it.copy(showAddLogoDialog = true) }
    }

    fun hideAddLogoDialog() {
        _uiState.update { it.copy(showAddLogoDialog = false) }
    }

    fun showAddTemplateDialog() {
        _uiState.update { it.copy(showAddTemplateDialog = true) }
    }

    fun hideAddTemplateDialog() {
        _uiState.update { it.copy(showAddTemplateDialog = false) }
    }

    
    fun addCustomLogo(uri: Uri, name: String, isMonochrome: Boolean) {
        viewModelScope.launch {
            try {
                val newLogo = repository.addCustomLogo(getApplication(), uri, name, isMonochrome)
                loadLogosAndStyles()
                _uiState.update {
                    it.copy(
                        showAddLogoDialog = false,
                        config = it.config.copy(logo = newLogo)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to add logo: ${e.message}")
                }
            }
        }
    }

    
    fun addCustomTemplate(
        name: String,
        description: String,
        templateJson: String,
        orientation: WatermarkOrientation
    ) {
        viewModelScope.launch {
            try {
                val newStyle = repository.addCustomTemplate(name, description, templateJson, orientation)
                loadLogosAndStyles()
                _uiState.update {
                    it.copy(
                        showAddTemplateDialog = false,
                        config = it.config.copy(style = newStyle)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to add template: ${e.message}")
                }
            }
        }
    }

    
    fun deleteCustomLogo(logoId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCustomLogo(logoId)
                loadLogosAndStyles()

                
                if (_uiState.value.config.logo?.id == logoId) {
                    _uiState.update {
                        it.copy(config = it.config.copy(logo = repository.getDefaultConfig().logo))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to delete logo: ${e.message}")
                }
            }
        }
    }

    
    fun deleteCustomTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCustomTemplate(templateId)
                loadLogosAndStyles()

                
                if (_uiState.value.config.style.id == templateId) {
                    _uiState.update {
                        it.copy(config = it.config.copy(style = repository.getDefaultConfig().style))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to delete template: ${e.message}")
                }
            }
        }
    }

    fun processImage() {
        val currentState = _uiState.value

        if (!currentState.isReadyToProcess) {
            _uiState.update {
                it.copy(error = "Please complete all required fields")
            }
            return
        }

        val imageUri = currentState.selectedImageUri ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isProcessing = true, processingProgress = "Loading full image...")
            }

            try {
                val originalBitmap = BitmapUtils.loadBitmapForProcessing(getApplication(), imageUri)

                if (originalBitmap == null) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processingProgress = "",
                            error = "Failed to load image for processing"
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(processingProgress = "Rendering watermark...") }

                val watermarkBitmap = WatermarkRenderer.renderWatermark(
                    context = getApplication(),
                    config = currentState.config,
                    exifData = currentState.exifData,
                    imageWidth = originalBitmap.width,
                    imageHeight = originalBitmap.height
                )

                _uiState.update { it.copy(processingProgress = "Applying watermark...") }

                val resultBitmap = BitmapUtils.appendWatermark(
                    original = originalBitmap,
                    watermark = watermarkBitmap,
                    position = currentState.config.position
                )

                _uiState.update { it.copy(processingProgress = "Saving to gallery...") }

                val saveResult = ImageSaver.saveToGallery(
                    context = getApplication(),
                    bitmap = resultBitmap
                )

                originalBitmap.recycle()
                watermarkBitmap.recycle()
                resultBitmap.recycle()

                saveResult.fold(
                    onSuccess = { uri ->
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                processingProgress = "",
                                savedImageUri = uri,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                processingProgress = "",
                                error = "Failed to save: ${error.message}"
                            )
                        }
                    }
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingProgress = "",
                        error = "Processing error: ${e.message}"
                    )
                }
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingProgress = "",
                        error = "Image too large. Try a smaller image."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSavedUri() {
        _uiState.update { it.copy(savedImageUri = null) }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value.previewBitmap?.recycle()
    }
}