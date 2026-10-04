package com.example.viewer

import android.graphics.RectF

object CoordinateTransformer {

    data class NormalizedPoint(val x: Float, val y: Float, val isValid: Boolean)

    /**
     * Translates a touch coordinate within Salim's viewer container to normalized Ubaid coordinates [0.0, 1.0].
     * Accurately accounts for aspect-fit letterboxing / pillarboxing.
     */
    fun transformToNormalized(
        touchX: Float,
        touchY: Float,
        containerWidth: Float,
        containerHeight: Float,
        remoteWidth: Float = 1080f,
        remoteHeight: Float = 2400f
    ): NormalizedPoint {
        if (containerWidth <= 0 || containerHeight <= 0 || remoteWidth <= 0 || remoteHeight <= 0) {
            return NormalizedPoint(0f, 0f, false)
        }

        val containerAspect = containerWidth / containerHeight
        val remoteAspect = remoteWidth / remoteHeight

        var renderedWidth: Float
        var renderedHeight: Float
        var offsetX = 0f
        var offsetY = 0f

        if (containerAspect > remoteAspect) {
            // Pillarbox: black bars on left & right
            renderedHeight = containerHeight
            renderedWidth = containerHeight * remoteAspect
            offsetX = (containerWidth - renderedWidth) / 2f
        } else {
            // Letterbox: black bars on top & bottom
            renderedWidth = containerWidth
            renderedHeight = containerWidth / remoteAspect
            offsetY = (containerHeight - renderedHeight) / 2f
        }

        // Check if touch is within rendered display bounds
        val bounds = RectF(offsetX, offsetY, offsetX + renderedWidth, offsetY + renderedHeight)
        if (!bounds.contains(touchX, touchY)) {
            // Outside active remote screen content
            return NormalizedPoint(0f, 0f, false)
        }

        val relativeX = touchX - offsetX
        val relativeY = touchY - offsetY

        val normX = (relativeX / renderedWidth).coerceIn(0f, 1f)
        val normY = (relativeY / renderedHeight).coerceIn(0f, 1f)

        return NormalizedPoint(normX, normY, true)
    }
}
