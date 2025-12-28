package io.prism.ui.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.slider.Slider
import io.prism.data.model.WatermarkConfig

class ScaleSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.sliderStyle
) : Slider(context, attrs, defStyleAttr) {

    init {
        valueFrom = WatermarkConfig.MIN_SCALE
        valueTo = WatermarkConfig.MAX_SCALE
        stepSize = WatermarkConfig.SCALE_STEP
        value = 15f

        setLabelFormatter { value ->
            "${value.toInt()}%"
        }
    }
}