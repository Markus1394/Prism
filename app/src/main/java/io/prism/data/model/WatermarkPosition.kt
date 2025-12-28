package io.prism.data.model

enum class WatermarkPosition {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT;

    fun isHorizontal(): Boolean {
        return this == TOP || this == BOTTOM
    }

    fun isVertical(): Boolean {
        return this == LEFT || this == RIGHT
    }
}