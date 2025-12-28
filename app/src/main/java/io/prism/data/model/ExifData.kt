package io.prism.data.model

data class ExifData(
    val make: String? = null,
    val model: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val dateTime: String? = null
) {
    
    fun formatForWatermark(): String {
        val parts = mutableListOf<String>()

        
        
        val deviceName = buildDeviceName()
        if (deviceName.isNotBlank()) {
            parts.add(deviceName)
        }

        
        val techSpecs = buildTechSpecs()
        if (techSpecs.isNotBlank()) {
            parts.add(techSpecs)
        }

        return parts.joinToString(" • ")
    }

    private fun buildDeviceName(): String {
        return when {
            !make.isNullOrBlank() && !model.isNullOrBlank() -> {
                
                val modelUpper = model.uppercase()
                val makeUpper = make.uppercase()

                if (modelUpper.contains(makeUpper) || modelUpper.startsWith(makeUpper)) {
                    
                    model
                } else {
                    
                    "$make $model"
                }
            }
            !model.isNullOrBlank() -> model
            !make.isNullOrBlank() -> make
            else -> ""
        }
    }

    private fun buildTechSpecs(): String {
        val specs = mutableListOf<String>()

        iso?.let {
            if (it.isNotBlank()) specs.add("ISO $it")
        }
        focalLength?.let {
            if (it.isNotBlank()) specs.add("${it}mm")
        }
        aperture?.let {
            if (it.isNotBlank()) specs.add("f/$it")
        }
        exposureTime?.let {
            if (it.isNotBlank()) specs.add("${it}s")
        }

        return specs.joinToString(" • ")
    }

    companion object {
        val EMPTY = ExifData()
    }
}