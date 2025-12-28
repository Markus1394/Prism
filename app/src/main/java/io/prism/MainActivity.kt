package io.prism

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import io.prism.data.model.LogoResource
import io.prism.data.model.WatermarkOrientation
import io.prism.data.model.WatermarkPosition
import io.prism.data.model.WatermarkStyle
import io.prism.data.model.WatermarkTheme
import io.prism.databinding.ActivityMainBinding
import io.prism.ui.MainUiState
import io.prism.ui.MainViewModel
import io.prism.ui.adapters.FontGridAdapter
import io.prism.ui.adapters.LogoGridAdapter
import io.prism.ui.adapters.PositionAdapter
import io.prism.ui.adapters.StyleGridAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var mainFontAdapter: FontGridAdapter
    private lateinit var exifFontAdapter: FontGridAdapter
    private lateinit var positionAdapter: PositionAdapter
    private lateinit var logoAdapter: LogoGridAdapter
    private lateinit var styleAdapter: StyleGridAdapter

    private var pendingLogoUri: Uri? = null

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.selectImage(it) }
    }

    private val logoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            pendingLogoUri = it
            showAddLogoDialogWithPreview(it)
        }
    }

    private val templateFilePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { loadTemplateFromFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapters()
        setupViews()
        observeState()
    }

    private fun setupAdapters() {
        mainFontAdapter = FontGridAdapter { font ->
            viewModel.updateMainTextFont(font)
        }

        exifFontAdapter = FontGridAdapter { font ->
            viewModel.updateExifTextFont(font)
        }

        positionAdapter = PositionAdapter { position ->
            viewModel.updatePosition(position)
        }

        logoAdapter = LogoGridAdapter(
            onLogoSelected = { logo ->
                viewModel.updateLogo(logo)
            },
            onAddCustomLogo = {
                logoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDeleteCustomLogo = { logo ->
                showDeleteLogoConfirmation(logo)
            }
        )

        styleAdapter = StyleGridAdapter(
            onStyleSelected = { style ->
                viewModel.updateStyle(style)
            },
            onAddCustomTemplate = {
                showAddTemplateDialog()
            },
            onDeleteCustomTemplate = { style ->
                showDeleteTemplateConfirmation(style)
            }
        )
    }

    private fun setupViews() {
        with(binding) {
            
            selectImageButton.setOnClickListener {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }

            
            mainTextInput.doAfterTextChanged { text ->
                viewModel.updateMainText(text?.toString() ?: "")
            }

            
            useExifCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateUseExifData(isChecked)
            }

            
            mainFontRecyclerView.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 2)
                adapter = mainFontAdapter
            }
            mainFontAdapter.submitList(viewModel.fonts)

            
            exifFontRecyclerView.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 2)
                adapter = exifFontAdapter
            }
            exifFontAdapter.submitList(viewModel.fonts)

            
            positionRecyclerView.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 4)
                adapter = positionAdapter
            }

            
            Log.d("PRISM", "Positions: ${viewModel.positions}")
            Log.d("PRISM", "Position count: ${viewModel.positions.size}")
            Log.d("PRISM", "Adapter item count: ${positionAdapter.itemCount}")

            
            logoRecyclerView.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 3)
                adapter = logoAdapter
            }

            
            styleRecyclerView.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 2)
                adapter = styleAdapter
            }

            
            templateHelpLink.setOnClickListener {
                openTemplateGuide()
            }

            
            themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    val theme = when (checkedId) {
                        R.id.lightThemeButton -> WatermarkTheme.LIGHT
                        R.id.darkThemeButton -> WatermarkTheme.DARK
                        else -> return@addOnButtonCheckedListener
                    }
                    viewModel.updateTheme(theme)
                }
            }

            
            scaleSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateScale(value)
                }
                scaleValueText.text = getString(R.string.scale_value_format, value.toInt())
            }

            
            processButton.setOnClickListener {
                viewModel.processImage()
            }

            
            lightThemeButton.isChecked = true
            scaleValueText.text = getString(R.string.scale_value_format, 10)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: MainUiState) {
        with(binding) {
            
            if (state.isLoadingImage) {
                imagePreview.visibility = View.VISIBLE
                imagePreview.setImageResource(R.drawable.image_preview_background)
                imageSizeText.text = getString(R.string.loading_image)
                imageSizeText.visibility = View.VISIBLE
            } else if (state.previewBitmap != null) {
                imagePreview.setImageBitmap(state.previewBitmap)
                imagePreview.visibility = View.VISIBLE
                selectImageButton.text = getString(R.string.change_image)
                imageSizeText.text = getString(
                    R.string.image_size_format,
                    state.actualImageWidth,
                    state.actualImageHeight
                )
                imageSizeText.visibility = View.VISIBLE
            } else {
                imagePreview.visibility = View.GONE
                imageSizeText.visibility = View.GONE
                selectImageButton.text = getString(R.string.select_image)
            }

            
            if (state.config.useExifData && state.exifData != io.prism.data.model.ExifData.EMPTY) {
                val exifText = state.exifData.formatForWatermark()
                if (exifText.isNotBlank()) {
                    exifPreviewText.text = exifText
                    exifPreviewText.visibility = View.VISIBLE
                } else {
                    exifPreviewText.visibility = View.GONE
                }
            } else {
                exifPreviewText.visibility = View.GONE
            }

            
            mainFontAdapter.setSelectedFont(state.config.mainTextFont)
            exifFontAdapter.setSelectedFont(state.config.exifTextFont)
            positionAdapter.setSelectedPosition(state.config.position)

            
            logoAdapter.submitLogos(state.allLogos)
            logoAdapter.setSelectedLogo(state.config.logo)

            
            styleAdapter.submitStyles(state.allStyles)
            styleAdapter.setSelectedStyle(state.config.style)

            
            if (state.isProcessing) {
                processingOverlay.visibility = View.VISIBLE
                processingText.text = state.processingProgress
                processButton.isEnabled = false
            } else {
                processingOverlay.visibility = View.GONE
                processButton.isEnabled = state.isReadyToProcess

                if (state.isReadyToProcess) {
                    processButton.text = getString(R.string.process_image)
                } else {
                    val errors = state.validationErrors
                    processButton.text = if (errors.isNotEmpty()) {
                        errors.first()
                    } else {
                        getString(R.string.process_image)
                    }
                }
            }

            selectImageButton.isEnabled = !state.isLoadingImage

            
            state.savedImageUri?.let {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.image_saved_success),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.clearSavedUri()
            }

            
            state.error?.let { error ->
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.error_title)
                    .setMessage(error)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
                viewModel.clearError()
            }
        }
    }

    private fun showAddLogoDialogWithPreview(uri: Uri) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_logo, null)

        val logoPreview = dialogView.findViewById<ImageView>(R.id.logoPreview)
        val selectButton = dialogView.findViewById<View>(R.id.selectLogoButton)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.logoNameInput)
        val monochromeSwitch = dialogView.findViewById<MaterialSwitch>(R.id.monochromeSwitch)

        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                logoPreview.setImageBitmap(bitmap)
                logoPreview.visibility = View.VISIBLE
                selectButton.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = nameInput.text?.toString()?.trim() ?: ""
                val isMonochrome = monochromeSwitch.isChecked

                if (name.isNotBlank()) {
                    viewModel.addCustomLogo(uri, name, isMonochrome)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddTemplateDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_template, null)

        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.templateNameInput)
        val descInput = dialogView.findViewById<TextInputEditText>(R.id.templateDescInput)
        val jsonInput = dialogView.findViewById<TextInputEditText>(R.id.templateJsonInput)
        val loadFromFileButton = dialogView.findViewById<View>(R.id.loadFromFileButton)

        loadFromFileButton.setOnClickListener {
            templateFilePicker.launch("application/json")
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = nameInput.text?.toString()?.trim() ?: ""
                val description = descInput.text?.toString()?.trim() ?: ""
                val json = jsonInput.text?.toString()?.trim() ?: ""

                if (name.isNotBlank() && json.isNotBlank()) {
                    viewModel.addCustomTemplate(
                        name = name,
                        description = description,
                        templateJson = json,
                        orientation = WatermarkOrientation.HORIZONTAL
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteLogoConfirmation(logo: LogoResource) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_logo_title)
            .setMessage(getString(R.string.delete_logo_message, logo.customName ?: ""))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCustomLogo(logo.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteTemplateConfirmation(style: WatermarkStyle) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_template_title)
            .setMessage(getString(R.string.delete_template_message, style.customName ?: ""))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCustomTemplate(style.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openTemplateGuide() {
        val url = getString(R.string.template_guide_url)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun loadTemplateFromFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val json = input.bufferedReader().readText()
                Toast.makeText(this, R.string.template_loaded, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_loading_template, Toast.LENGTH_SHORT).show()
        }
    }
}