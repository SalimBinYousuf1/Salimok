package com.example.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.viewer.CoordinateTransformer

@Composable
fun RemoteTouchSurface(
    onTap: (normX: Float, normY: Float) -> Unit,
    onLongPress: (normX: Float, normY: Float) -> Unit,
    onSwipe: (normX1: Float, normY1: Float, normX2: Float, normY2: Float, durationMs: Long) -> Unit,
    remoteResolutionWidth: Float = 1080f,
    remoteResolutionHeight: Float = 2400f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var dragStartTime by remember { mutableStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(containerSize, remoteResolutionWidth, remoteResolutionHeight) {
                detectTapGestures(
                    onTap = { offset ->
                        val pt = CoordinateTransformer.transformToNormalized(
                            touchX = offset.x,
                            touchY = offset.y,
                            containerWidth = containerSize.width.toFloat(),
                            containerHeight = containerSize.height.toFloat(),
                            remoteWidth = remoteResolutionWidth,
                            remoteHeight = remoteResolutionHeight
                        )
                        if (pt.isValid) {
                            onTap(pt.x, pt.y)
                        }
                    },
                    onLongPress = { offset ->
                        val pt = CoordinateTransformer.transformToNormalized(
                            touchX = offset.x,
                            touchY = offset.y,
                            containerWidth = containerSize.width.toFloat(),
                            containerHeight = containerSize.height.toFloat(),
                            remoteWidth = remoteResolutionWidth,
                            remoteHeight = remoteResolutionHeight
                        )
                        if (pt.isValid) {
                            onLongPress(pt.x, pt.y)
                        }
                    }
                )
            }
            .pointerInput(containerSize, remoteResolutionWidth, remoteResolutionHeight) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartOffset = offset
                        dragStartTime = System.currentTimeMillis()
                    },
                    onDragEnd = {
                        val start = dragStartOffset
                        if (start != null) {
                            val duration = (System.currentTimeMillis() - dragStartTime).coerceAtLeast(100L)
                            // We can use the drag start point or compute displacement
                            // Note: onDrag tracks displacement, or onDragEnd signals completion
                        }
                        dragStartOffset = null
                    },
                    onDragCancel = {
                        dragStartOffset = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val start = dragStartOffset
                        if (start != null) {
                            val currentPos = change.position
                            val distSq = (currentPos.x - start.x) * (currentPos.x - start.x) +
                                    (currentPos.y - start.y) * (currentPos.y - start.y)

                            // Threshold for a swipe gesture (e.g. 40 pixels)
                            if (distSq > 1600f) {
                                val p1 = CoordinateTransformer.transformToNormalized(
                                    start.x, start.y,
                                    containerSize.width.toFloat(), containerSize.height.toFloat(),
                                    remoteResolutionWidth, remoteResolutionHeight
                                )
                                val p2 = CoordinateTransformer.transformToNormalized(
                                    currentPos.x, currentPos.y,
                                    containerSize.width.toFloat(), containerSize.height.toFloat(),
                                    remoteResolutionWidth, remoteResolutionHeight
                                )

                                if (p1.isValid && p2.isValid) {
                                    val duration = (System.currentTimeMillis() - dragStartTime).coerceIn(150L, 800L)
                                    onSwipe(p1.x, p1.y, p2.x, p2.y, duration)
                                    dragStartOffset = currentPos // Reset start for continuous drag
                                    dragStartTime = System.currentTimeMillis()
                                }
                            }
                        }
                    }
                )
            }
    ) {
        content()
    }
}
