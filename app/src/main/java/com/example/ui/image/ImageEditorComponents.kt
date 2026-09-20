package com.example.ui.image

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.image.CardQuadCorners
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Interactive Crop Overlay with 4 Corner Draggers, 4 Edge Bars,
 * Rule-of-Thirds Grid, Dimmed Exterior, and Butter-Smooth 60/120 FPS Dragging.
 *
 * Mathematically maintains true aspect ratio across any canvas dimension
 * without squashing or warping the card image.
 */
@Composable
fun InteractiveCropOverlay(
    cropRect: RectF, // Normalized in [0..1]
    onCropRectChanged: (RectF) -> Unit,
    aspectRatio: Float? = null,
    imageWidth: Int = 0,
    imageHeight: Int = 0,
    modifier: Modifier = Modifier
) {
    var activeHandle by remember { mutableStateOf<CropHandle?>(null) }

    // rememberUpdatedState prevents gesture listener teardown during drag
    val currentRect by rememberUpdatedState(cropRect)
    val currentAspect by rememberUpdatedState(aspectRatio)
    val onRectChanged by rememberUpdatedState(onCropRectChanged)

    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { 38.dp.toPx() }
    val edgeRadiusPx = with(density) { 26.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val touchX = offset.x
                        val touchY = offset.y
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        activeHandle = hitTestCropHandlePx(
                            touchPxX = touchX,
                            touchPxY = touchY,
                            rect = currentRect,
                            widthPx = w,
                            heightPx = h,
                            cornerRadiusPx = cornerRadiusPx,
                            edgeRadiusPx = edgeRadiusPx
                        )
                    },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val handle = activeHandle ?: return@detectDragGestures

                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 10f || h <= 10f) return@detectDragGestures

                        val canvasRatio = w / h
                        val targetAspect = currentAspect
                        val rect = currentRect
                        val minNormSize = 0.05f

                        val dx = dragAmount.x / w
                        val dy = dragAmount.y / h

                        val newRect = RectF(rect)

                        when (handle) {
                            CropHandle.TOP_LEFT -> {
                                val anchorX = rect.right
                                val anchorY = rect.bottom
                                if (targetAspect != null && targetAspect > 0f) {
                                    val proposedLeft = (rect.left + dx).coerceIn(0f, anchorX - minNormSize)
                                    var normW = anchorX - proposedLeft
                                    var normH = normW * canvasRatio / targetAspect
                                    if (anchorY - normH < 0f) {
                                        normH = anchorY
                                        normW = normH * targetAspect / canvasRatio
                                    }
                                    newRect.left = (anchorX - normW).coerceIn(0f, anchorX - minNormSize)
                                    newRect.top = (anchorY - normH).coerceIn(0f, anchorY - minNormSize)
                                } else {
                                    newRect.left = (rect.left + dx).coerceIn(0f, anchorX - minNormSize)
                                    newRect.top = (rect.top + dy).coerceIn(0f, anchorY - minNormSize)
                                }
                            }
                            CropHandle.TOP_RIGHT -> {
                                val anchorX = rect.left
                                val anchorY = rect.bottom
                                if (targetAspect != null && targetAspect > 0f) {
                                    val proposedRight = (rect.right + dx).coerceIn(anchorX + minNormSize, 1f)
                                    var normW = proposedRight - anchorX
                                    var normH = normW * canvasRatio / targetAspect
                                    if (anchorY - normH < 0f) {
                                        normH = anchorY
                                        normW = normH * targetAspect / canvasRatio
                                    }
                                    newRect.right = (anchorX + normW).coerceIn(anchorX + minNormSize, 1f)
                                    newRect.top = (anchorY - normH).coerceIn(0f, anchorY - minNormSize)
                                } else {
                                    newRect.right = (rect.right + dx).coerceIn(anchorX + minNormSize, 1f)
                                    newRect.top = (rect.top + dy).coerceIn(0f, anchorY - minNormSize)
                                }
                            }
                            CropHandle.BOTTOM_RIGHT -> {
                                val anchorX = rect.left
                                val anchorY = rect.top
                                if (targetAspect != null && targetAspect > 0f) {
                                    val proposedRight = (rect.right + dx).coerceIn(anchorX + minNormSize, 1f)
                                    var normW = proposedRight - anchorX
                                    var normH = normW * canvasRatio / targetAspect
                                    if (anchorY + normH > 1f) {
                                        normH = 1f - anchorY
                                        normW = normH * targetAspect / canvasRatio
                                    }
                                    newRect.right = (anchorX + normW).coerceIn(anchorX + minNormSize, 1f)
                                    newRect.bottom = (anchorY + normH).coerceIn(anchorY + minNormSize, 1f)
                                } else {
                                    newRect.right = (rect.right + dx).coerceIn(anchorX + minNormSize, 1f)
                                    newRect.bottom = (rect.bottom + dy).coerceIn(anchorY + minNormSize, 1f)
                                }
                            }
                            CropHandle.BOTTOM_LEFT -> {
                                val anchorX = rect.right
                                val anchorY = rect.top
                                if (targetAspect != null && targetAspect > 0f) {
                                    val proposedLeft = (rect.left + dx).coerceIn(0f, anchorX - minNormSize)
                                    var normW = anchorX - proposedLeft
                                    var normH = normW * canvasRatio / targetAspect
                                    if (anchorY + normH > 1f) {
                                        normH = 1f - anchorY
                                        normW = normH * targetAspect / canvasRatio
                                    }
                                    newRect.left = (anchorX - normW).coerceIn(0f, anchorX - minNormSize)
                                    newRect.bottom = (anchorY + normH).coerceIn(anchorY + minNormSize, 1f)
                                } else {
                                    newRect.left = (rect.left + dx).coerceIn(0f, anchorX - minNormSize)
                                    newRect.bottom = (rect.bottom + dy).coerceIn(anchorY + minNormSize, 1f)
                                }
                            }
                            CropHandle.TOP_EDGE -> {
                                if (targetAspect != null && targetAspect > 0f) {
                                    val newTop = (rect.top + dy).coerceIn(0f, rect.bottom - minNormSize)
                                    val normH = rect.bottom - newTop
                                    val normW = normH * targetAspect / canvasRatio
                                    val centerX = (rect.left + rect.right) / 2f
                                    val halfW = normW / 2f
                                    if (centerX - halfW >= 0f && centerX + halfW <= 1f) {
                                        newRect.top = newTop
                                        newRect.left = centerX - halfW
                                        newRect.right = centerX + halfW
                                    }
                                } else {
                                    newRect.top = (rect.top + dy).coerceIn(0f, rect.bottom - minNormSize)
                                }
                            }
                            CropHandle.BOTTOM_EDGE -> {
                                if (targetAspect != null && targetAspect > 0f) {
                                    val newBottom = (rect.bottom + dy).coerceIn(rect.top + minNormSize, 1f)
                                    val normH = newBottom - rect.top
                                    val normW = normH * targetAspect / canvasRatio
                                    val centerX = (rect.left + rect.right) / 2f
                                    val halfW = normW / 2f
                                    if (centerX - halfW >= 0f && centerX + halfW <= 1f) {
                                        newRect.bottom = newBottom
                                        newRect.left = centerX - halfW
                                        newRect.right = centerX + halfW
                                    }
                                } else {
                                    newRect.bottom = (rect.bottom + dy).coerceIn(rect.top + minNormSize, 1f)
                                }
                            }
                            CropHandle.LEFT_EDGE -> {
                                if (targetAspect != null && targetAspect > 0f) {
                                    val newLeft = (rect.left + dx).coerceIn(0f, rect.right - minNormSize)
                                    val normW = rect.right - newLeft
                                    val normH = normW * canvasRatio / targetAspect
                                    val centerY = (rect.top + rect.bottom) / 2f
                                    val halfH = normH / 2f
                                    if (centerY - halfH >= 0f && centerY + halfH <= 1f) {
                                        newRect.left = newLeft
                                        newRect.top = centerY - halfH
                                        newRect.bottom = centerY + halfH
                                    }
                                } else {
                                    newRect.left = (rect.left + dx).coerceIn(0f, rect.right - minNormSize)
                                }
                            }
                            CropHandle.RIGHT_EDGE -> {
                                if (targetAspect != null && targetAspect > 0f) {
                                    val newRight = (rect.right + dx).coerceIn(rect.left + minNormSize, 1f)
                                    val normW = newRight - rect.left
                                    val normH = normW * canvasRatio / targetAspect
                                    val centerY = (rect.top + rect.bottom) / 2f
                                    val halfH = normH / 2f
                                    if (centerY - halfH >= 0f && centerY + halfH <= 1f) {
                                        newRect.right = newRight
                                        newRect.top = centerY - halfH
                                        newRect.bottom = centerY + halfH
                                    }
                                } else {
                                    newRect.right = (rect.right + dx).coerceIn(rect.left + minNormSize, 1f)
                                }
                            }
                            CropHandle.INSIDE_PAN -> {
                                val rw = rect.width()
                                val rh = rect.height()
                                val l = (rect.left + dx).coerceIn(0f, 1f - rw)
                                val t = (rect.top + dy).coerceIn(0f, 1f - rh)
                                newRect.left = l
                                newRect.top = t
                                newRect.right = l + rw
                                newRect.bottom = t + rh
                            }
                        }

                        onRectChanged(newRect)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val leftPx = cropRect.left * w
            val topPx = cropRect.top * h
            val rightPx = cropRect.right * w
            val bottomPx = cropRect.bottom * h
            val cropWPx = rightPx - leftPx
            val cropHPx = bottomPx - topPx

            val dimColor = Color(0xAA000000)

            // Dim outside 4 regions around crop rectangle
            drawRect(dimColor, Offset(0f, 0f), Size(w, topPx)) // Top
            drawRect(dimColor, Offset(0f, topPx), Size(leftPx, cropHPx)) // Left
            drawRect(dimColor, Offset(rightPx, topPx), Size(w - rightPx, cropHPx)) // Right
            drawRect(dimColor, Offset(0f, bottomPx), Size(w, h - bottomPx)) // Bottom

            // Rule-of-thirds grid
            val gridColor = Color.White.copy(alpha = 0.28f)
            val stroke1dp = 1.dp.toPx()

            val col1 = leftPx + cropWPx / 3f
            val col2 = leftPx + 2f * cropWPx / 3f
            drawLine(gridColor, Offset(col1, topPx), Offset(col1, bottomPx), strokeWidth = stroke1dp)
            drawLine(gridColor, Offset(col2, topPx), Offset(col2, bottomPx), strokeWidth = stroke1dp)

            val row1 = topPx + cropHPx / 3f
            val row2 = topPx + 2f * cropHPx / 3f
            drawLine(gridColor, Offset(leftPx, row1), Offset(rightPx, row1), strokeWidth = stroke1dp)
            drawLine(gridColor, Offset(leftPx, row2), Offset(rightPx, row2), strokeWidth = stroke1dp)

            // Main crop box border
            val boxBorder = CardMateTealPrimary
            drawRect(
                boxBorder,
                Offset(leftPx, topPx),
                Size(cropWPx, cropHPx),
                style = Stroke(width = 2.dp.toPx())
            )

            // Corner Handles (Thick L-shapes & circular touch anchor)
            val cornerLen = 22.dp.toPx()
            val cornerStroke = 4.dp.toPx()
            val cornerColor = Color.White
            val activeColor = CardMateGoldAccent

            // Helper to draw corner bracket + circle
            fun drawCornerKnob(x: Float, y: Float, isTL: Boolean, isTR: Boolean, isBR: Boolean, isBL: Boolean, isActive: Boolean) {
                val c = if (isActive) activeColor else cornerColor
                val radius = if (isActive) 9.dp.toPx() else 7.dp.toPx()

                // Glow ring if active
                if (isActive) {
                    drawCircle(color = activeColor.copy(alpha = 0.4f), radius = radius + 6.dp.toPx(), center = Offset(x, y))
                }

                // Drop shadow dot
                drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = radius + 2.dp.toPx(), center = Offset(x, y))
                drawCircle(color = c, radius = radius, center = Offset(x, y))
                drawCircle(color = Color.Black, radius = radius - 3.dp.toPx(), center = Offset(x, y))

                // Draw bracket lines
                if (isTL) {
                    drawLine(c, Offset(x - 2, y), Offset(x + cornerLen, y), cornerStroke)
                    drawLine(c, Offset(x, y - 2), Offset(x, y + cornerLen), cornerStroke)
                } else if (isTR) {
                    drawLine(c, Offset(x - cornerLen, y), Offset(x + 2, y), cornerStroke)
                    drawLine(c, Offset(x, y - 2), Offset(x, y + cornerLen), cornerStroke)
                } else if (isBR) {
                    drawLine(c, Offset(x - cornerLen, y), Offset(x + 2, y), cornerStroke)
                    drawLine(c, Offset(x, y - cornerLen), Offset(x, y + 2), cornerStroke)
                } else if (isBL) {
                    drawLine(c, Offset(x - 2, y), Offset(x + cornerLen, y), cornerStroke)
                    drawLine(c, Offset(x, y - cornerLen), Offset(x, y + 2), cornerStroke)
                }
            }

            drawCornerKnob(leftPx, topPx, isTL = true, isTR = false, isBR = false, isBL = false, isActive = activeHandle == CropHandle.TOP_LEFT)
            drawCornerKnob(rightPx, topPx, isTL = false, isTR = true, isBR = false, isBL = false, isActive = activeHandle == CropHandle.TOP_RIGHT)
            drawCornerKnob(rightPx, bottomPx, isTL = false, isTR = false, isBR = true, isBL = false, isActive = activeHandle == CropHandle.BOTTOM_RIGHT)
            drawCornerKnob(leftPx, bottomPx, isTL = false, isTR = false, isBR = false, isBL = true, isActive = activeHandle == CropHandle.BOTTOM_LEFT)

            // Center edge bar indicator pills
            val edgeBarLen = 18.dp.toPx()
            val midX = (leftPx + rightPx) / 2f
            val midY = (topPx + bottomPx) / 2f

            fun drawEdgePill(start: Offset, end: Offset, isActive: Boolean) {
                val c = if (isActive) activeColor else cornerColor
                val str = if (isActive) 5.dp.toPx() else 4.dp.toPx()
                drawLine(Color.Black.copy(alpha = 0.5f), start, end, str + 2.dp.toPx())
                drawLine(c, start, end, str)
            }

            drawEdgePill(Offset(midX - edgeBarLen / 2, topPx), Offset(midX + edgeBarLen / 2, topPx), activeHandle == CropHandle.TOP_EDGE)
            drawEdgePill(Offset(midX - edgeBarLen / 2, bottomPx), Offset(midX + edgeBarLen / 2, bottomPx), activeHandle == CropHandle.BOTTOM_EDGE)
            drawEdgePill(Offset(leftPx, midY - edgeBarLen / 2), Offset(leftPx, midY + edgeBarLen / 2), activeHandle == CropHandle.LEFT_EDGE)
            drawEdgePill(Offset(rightPx, midY - edgeBarLen / 2), Offset(rightPx, midY + edgeBarLen / 2), activeHandle == CropHandle.RIGHT_EDGE)
        }
    }
}

private enum class CropHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT,
    TOP_EDGE, BOTTOM_EDGE, LEFT_EDGE, RIGHT_EDGE,
    INSIDE_PAN
}

/**
 * Pixel-accurate hit tester with generous touch targets.
 */
private fun hitTestCropHandlePx(
    touchPxX: Float,
    touchPxY: Float,
    rect: RectF,
    widthPx: Float,
    heightPx: Float,
    cornerRadiusPx: Float,
    edgeRadiusPx: Float
): CropHandle? {
    val leftPx = rect.left * widthPx
    val topPx = rect.top * heightPx
    val rightPx = rect.right * widthPx
    val bottomPx = rect.bottom * heightPx

    // 1. Check 4 corners first with prioritized circular radius
    val dTL = hypot(touchPxX - leftPx, touchPxY - topPx)
    val dTR = hypot(touchPxX - rightPx, touchPxY - topPx)
    val dBR = hypot(touchPxX - rightPx, touchPxY - bottomPx)
    val dBL = hypot(touchPxX - leftPx, touchPxY - bottomPx)

    val minCorner = minOf(dTL, dTR, dBR, dBL)
    if (minCorner <= cornerRadiusPx) {
        return when (minCorner) {
            dTL -> CropHandle.TOP_LEFT
            dTR -> CropHandle.TOP_RIGHT
            dBR -> CropHandle.BOTTOM_RIGHT
            else -> CropHandle.BOTTOM_LEFT
        }
    }

    // 2. Check 4 edges
    if (abs(touchPxY - topPx) <= edgeRadiusPx && touchPxX in (leftPx - 10)..(rightPx + 10)) return CropHandle.TOP_EDGE
    if (abs(touchPxY - bottomPx) <= edgeRadiusPx && touchPxX in (leftPx - 10)..(rightPx + 10)) return CropHandle.BOTTOM_EDGE
    if (abs(touchPxX - leftPx) <= edgeRadiusPx && touchPxY in (topPx - 10)..(bottomPx + 10)) return CropHandle.LEFT_EDGE
    if (abs(touchPxX - rightPx) <= edgeRadiusPx && touchPxY in (topPx - 10)..(bottomPx + 10)) return CropHandle.RIGHT_EDGE

    // 3. Inside Pan
    if (touchPxX in leftPx..rightPx && touchPxY in topPx..bottomPx) {
        return CropHandle.INSIDE_PAN
    }

    return null
}

/**
 * Interactive Perspective 4-Corner Quad Warp Overlay with smooth, responsive drag
 * and pixel-accurate touch targets.
 */
@Composable
fun InteractivePerspectiveOverlay(
    quad: CardQuadCorners,
    onQuadChanged: (CardQuadCorners) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCorner by remember { mutableStateOf<CornerType?>(null) }

    val currentQuad by rememberUpdatedState(quad)
    val onQuadUpdate by rememberUpdatedState(onQuadChanged)

    val density = LocalDensity.current
    val touchRadiusPx = with(density) { 42.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val touchX = offset.x
                        val touchY = offset.y
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        activeCorner = hitTestQuadCornerPx(touchX, touchY, currentQuad, w, h, touchRadiusPx)
                    },
                    onDragEnd = { activeCorner = null },
                    onDragCancel = { activeCorner = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val corner = activeCorner ?: return@detectDragGestures

                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 10f || h <= 10f) return@detectDragGestures

                        val dx = dragAmount.x / w
                        val dy = dragAmount.y / h
                        val q = currentQuad

                        when (corner) {
                            CornerType.TOP_LEFT -> {
                                val newP = PointF(
                                    (q.topLeft.x + dx).coerceIn(0.01f, q.topRight.x - 0.05f),
                                    (q.topLeft.y + dy).coerceIn(0.01f, q.bottomLeft.y - 0.05f)
                                )
                                onQuadUpdate(q.copy(topLeft = newP))
                            }
                            CornerType.TOP_RIGHT -> {
                                val newP = PointF(
                                    (q.topRight.x + dx).coerceIn(q.topLeft.x + 0.05f, 0.99f),
                                    (q.topRight.y + dy).coerceIn(0.01f, q.bottomRight.y - 0.05f)
                                )
                                onQuadUpdate(q.copy(topRight = newP))
                            }
                            CornerType.BOTTOM_RIGHT -> {
                                val newP = PointF(
                                    (q.bottomRight.x + dx).coerceIn(q.bottomLeft.x + 0.05f, 0.99f),
                                    (q.bottomRight.y + dy).coerceIn(q.topRight.y + 0.05f, 0.99f)
                                )
                                onQuadUpdate(q.copy(bottomRight = newP))
                            }
                            CornerType.BOTTOM_LEFT -> {
                                val newP = PointF(
                                    (q.bottomLeft.x + dx).coerceIn(0.01f, q.bottomRight.x - 0.05f),
                                    (q.bottomLeft.y + dy).coerceIn(q.topLeft.y + 0.05f, 0.99f)
                                )
                                onQuadUpdate(q.copy(bottomLeft = newP))
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val tl = Offset(quad.topLeft.x * w, quad.topLeft.y * h)
            val tr = Offset(quad.topRight.x * w, quad.topRight.y * h)
            val br = Offset(quad.bottomRight.x * w, quad.bottomRight.y * h)
            val bl = Offset(quad.bottomLeft.x * w, quad.bottomLeft.y * h)

            // Draw connecting polygon quad boundary
            val path = Path().apply {
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }

            // Fill subtle tinted overlay inside quad
            drawPath(path, color = CardMateTealPrimary.copy(alpha = 0.15f))

            // Outline
            drawPath(
                path,
                color = CardMateGoldAccent,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Target Corner Knobs
            val handleRadius = 14.dp.toPx()
            val innerRadius = 5.dp.toPx()

            listOf(
                tl to CornerType.TOP_LEFT,
                tr to CornerType.TOP_RIGHT,
                br to CornerType.BOTTOM_RIGHT,
                bl to CornerType.BOTTOM_LEFT
            ).forEach { (pt, corner) ->
                val isActive = activeCorner == corner

                // Glow ring when being dragged
                if (isActive) {
                    drawCircle(
                        color = CardMateGoldAccent.copy(alpha = 0.45f),
                        radius = handleRadius + 8.dp.toPx(),
                        center = pt
                    )
                }

                // Outer ring
                drawCircle(
                    color = Color.Black.copy(alpha = 0.55f),
                    radius = handleRadius + 2.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = if (isActive) CardMateGoldAccent else CardMateTealPrimary,
                    radius = handleRadius,
                    center = pt,
                    style = Stroke(width = 3.dp.toPx())
                )
                // Center solid bullseye dot
                drawCircle(
                    color = Color.White,
                    radius = innerRadius,
                    center = pt
                )
            }
        }
    }
}

private enum class CornerType {
    TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT
}

private fun hitTestQuadCornerPx(
    touchPxX: Float,
    touchPxY: Float,
    quad: CardQuadCorners,
    widthPx: Float,
    heightPx: Float,
    touchRadiusPx: Float
): CornerType? {
    val dTL = hypot(touchPxX - quad.topLeft.x * widthPx, touchPxY - quad.topLeft.y * heightPx)
    val dTR = hypot(touchPxX - quad.topRight.x * widthPx, touchPxY - quad.topRight.y * heightPx)
    val dBR = hypot(touchPxX - quad.bottomRight.x * widthPx, touchPxY - quad.bottomRight.y * heightPx)
    val dBL = hypot(touchPxX - quad.bottomLeft.x * widthPx, touchPxY - quad.bottomLeft.y * heightPx)

    val minD = minOf(dTL, dTR, dBR, dBL)
    if (minD > touchRadiusPx) return null

    return when (minD) {
        dTL -> CornerType.TOP_LEFT
        dTR -> CornerType.TOP_RIGHT
        dBR -> CornerType.BOTTOM_RIGHT
        else -> CornerType.BOTTOM_LEFT
    }
}
