package com.example.image

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Detection States for Smart Business Card Scanner
 */
enum class CardDetectionState {
    SEARCHING,
    CARD_DETECTED,
    CARD_STABLE,
    CAPTURING,
    PROCESSING,
    READY_FOR_REVIEW,
    FAILED
}

/**
 * Real-time analysis output produced on each camera frame
 */
data class DetectedCardFrame(
    val corners: CardQuadCorners = CardQuadCorners.DEFAULT,
    val confidence: Float = 0f,
    val isStable: Boolean = false,
    val stableFrameCount: Int = 0,
    val detectionState: CardDetectionState = CardDetectionState.SEARCHING,
    val estimatedAspectRatio: Float = 1.75f,
    val hasGlare: Boolean = false,
    val isBlurry: Boolean = false,
    val averageLuminance: Float = 128f
)

/**
 * Production-ready Smart Card Detection & Frame Stability Engine.
 * Analyzes live camera feed in real-time, finds card boundaries,
 * tracks temporal stability, and calculates image quality metrics.
 */
class SmartCardDetectionEngine {

    private var previousCorners: CardQuadCorners? = null
    private var consecutiveStableFrames: Int = 0
    private var lastAnalysisTimestamp: Long = 0L

    companion object {
        const val STABILITY_DISTANCE_THRESHOLD = 0.035f // 3.5% normalized movement threshold
        const val REQUIRED_STABLE_FRAMES = 3 // Frames of steady position before triggering auto-capture
        const val MIN_ANALYSIS_INTERVAL_MS = 65L // ~15 FPS throttle for fast, responsive detection without battery drain
    }

    /**
     * Resets the stability counter and tracking history.
     */
    fun reset() {
        previousCorners = null
        consecutiveStableFrames = 0
        lastAnalysisTimestamp = 0L
    }

    /**
     * Analyzes a CameraX ImageProxy frame using its Y (Luminance) plane.
     */
    fun analyzeImageProxy(imageProxy: ImageProxy): DetectedCardFrame? {
        val now = System.currentTimeMillis()
        if (now - lastAnalysisTimestamp < MIN_ANALYSIS_INTERVAL_MS) {
            return null
        }
        lastAnalysisTimestamp = now

        val plane = imageProxy.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees

        return analyzeLumaBuffer(buffer, width, height, plane.rowStride, rotation)
    }

    /**
     * Fast luminance buffer analysis.
     */
    fun analyzeLumaBuffer(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        rotationDegrees: Int
    ): DetectedCardFrame {
        // Downsample for fast real-time edge & contour analysis (approx 160x100 grid)
        val sampleW = 160
        val sampleH = 100
        val lumaMap = FloatArray(sampleW * sampleH)

        val stepX = max(1, width / sampleW)
        val stepY = max(1, height / sampleH)

        var totalLuma = 0f
        var glarePixelCount = 0
        val totalSampled = sampleW * sampleH

        for (sy in 0 until sampleH) {
            val srcY = (sy * stepY).coerceIn(0, height - 1)
            val rowStart = srcY * rowStride
            for (sx in 0 until sampleW) {
                val srcX = (sx * stepX).coerceIn(0, width - 1)
                val pixelIndex = rowStart + srcX
                val rawLuma = if (pixelIndex < buffer.capacity()) {
                    (buffer.get(pixelIndex).toInt() and 0xFF).toFloat()
                } else 128f

                lumaMap[sy * sampleW + sx] = rawLuma
                totalLuma += rawLuma
                if (rawLuma > 242f) glarePixelCount++
            }
        }

        val avgLuma = totalLuma / totalSampled
        val glareRatio = glarePixelCount.toFloat() / totalSampled
        val hasGlare = glareRatio > 0.12f

        // Estimate sharpness via gradient variance (Laplacian-like)
        var gradSum = 0f
        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val idx = y * sampleW + x
                val center = lumaMap[idx]
                val diffX = abs(center - lumaMap[idx + 1])
                val diffY = abs(center - lumaMap[idx + sampleW])
                gradSum += (diffX + diffY)
            }
        }
        val avgGrad = gradSum / ((sampleW - 2) * (sampleH - 2))
        val isBlurry = avgGrad < 7.5f

        // Detect Card Boundary Quads
        val detectedQuad = findCardQuadFromLuma(lumaMap, sampleW, sampleH, avgLuma)

        // Adjust for camera rotation if necessary (CameraX coordinates)
        val normalizedCorners = adjustCornersForRotation(detectedQuad, rotationDegrees)

        // Calculate card aspect ratio
        val topW = distance(normalizedCorners.topLeft, normalizedCorners.topRight)
        val botW = distance(normalizedCorners.bottomLeft, normalizedCorners.bottomRight)
        val leftH = distance(normalizedCorners.topLeft, normalizedCorners.bottomLeft)
        val rightH = distance(normalizedCorners.topRight, normalizedCorners.bottomRight)

        val cardWidth = (topW + botW) / 2f
        val cardHeight = (leftH + rightH) / 2f
        val aspectRatio = if (cardHeight > 0.05f) (cardWidth / cardHeight).coerceIn(1.2f, 2.3f) else 1.75f

        // Check Stability across consecutive frames
        val isSteady = checkStability(normalizedCorners)
        if (isSteady && detectedQuad.confidence >= 0.70f) {
            consecutiveStableFrames++
        } else {
            consecutiveStableFrames = max(0, consecutiveStableFrames - 1)
        }

        val isFullyStable = consecutiveStableFrames >= REQUIRED_STABLE_FRAMES

        // Determine State
        val state = when {
            detectedQuad.confidence < 0.50f -> CardDetectionState.SEARCHING
            !isFullyStable -> CardDetectionState.CARD_DETECTED
            else -> CardDetectionState.CARD_STABLE
        }

        previousCorners = normalizedCorners

        return DetectedCardFrame(
            corners = normalizedCorners,
            confidence = detectedQuad.confidence,
            isStable = isFullyStable,
            stableFrameCount = consecutiveStableFrames,
            detectionState = state,
            estimatedAspectRatio = aspectRatio,
            hasGlare = hasGlare,
            isBlurry = isBlurry,
            averageLuminance = avgLuma
        )
    }

    /**
     * Fast card boundary search by scanning inward from edges to find strongest gradient contrast.
     */
    private fun findCardQuadFromLuma(
        luma: FloatArray,
        sampleW: Int,
        sampleH: Int,
        avgLuma: Float
    ): CardQuadCorners {
        val grad = FloatArray(sampleW * sampleH)
        var totalGrad = 0f
        for (y in 1 until sampleH - 1) {
            val yOffset = y * sampleW
            for (x in 1 until sampleW - 1) {
                val idx = yOffset + x
                val gx = abs(luma[idx + 1] - luma[idx - 1])
                val gy = abs(luma[idx + sampleW] - luma[idx - sampleW])
                val gVal = gx + gy
                grad[idx] = gVal
                totalGrad += gVal
            }
        }
        val avgGrad = totalGrad / (sampleW * sampleH)
        val gradThreshold = (avgGrad * 1.5f).coerceIn(14f, 40f)

        // 1. Scan Top Edge (from top downwards up to 60% of frame)
        var topY = (sampleH * 0.06f).toInt()
        var maxTopEnergy = 0f
        val xStart = (sampleW * 0.15f).toInt()
        val xEnd = (sampleW * 0.85f).toInt()
        val spanX = (xEnd - xStart).coerceAtLeast(1)

        for (y in 2 until (sampleH * 0.60f).toInt()) {
            var rowGradSum = 0f
            for (x in xStart..xEnd) {
                rowGradSum += grad[y * sampleW + x]
            }
            val avgRowGrad = rowGradSum / spanX
            if (avgRowGrad > gradThreshold && avgRowGrad > maxTopEnergy) {
                maxTopEnergy = avgRowGrad
                topY = y
            }
        }

        // 2. Scan Bottom Edge (from bottom upwards down to 40% of frame)
        var botY = (sampleH * 0.94f).toInt()
        var maxBotEnergy = 0f
        for (y in sampleH - 3 downTo (sampleH * 0.40f).toInt()) {
            if (y <= topY + 20) break
            var rowGradSum = 0f
            for (x in xStart..xEnd) {
                rowGradSum += grad[y * sampleW + x]
            }
            val avgRowGrad = rowGradSum / spanX
            if (avgRowGrad > gradThreshold && avgRowGrad > maxBotEnergy) {
                maxBotEnergy = avgRowGrad
                botY = y
            }
        }

        val cardSpanY = (botY - topY).coerceAtLeast(20)
        val yStart = topY + (cardSpanY * 0.15f).toInt()
        val yEnd = botY - (cardSpanY * 0.15f).toInt()
        val spanY = (yEnd - yStart).coerceAtLeast(1)

        // 3. Scan Left Edge (from left inward up to 55% of frame)
        var leftX = (sampleW * 0.06f).toInt()
        var maxLeftEnergy = 0f
        for (x in 2 until (sampleW * 0.55f).toInt()) {
            var colGradSum = 0f
            for (y in yStart..yEnd) {
                colGradSum += grad[y * sampleW + x]
            }
            val avgColGrad = colGradSum / spanY
            if (avgColGrad > gradThreshold && avgColGrad > maxLeftEnergy) {
                maxLeftEnergy = avgColGrad
                leftX = x
            }
        }

        // 4. Scan Right Edge (from right inward down to 45% of frame)
        var rightX = (sampleW * 0.94f).toInt()
        var maxRightEnergy = 0f
        for (x in sampleW - 3 downTo (sampleW * 0.45f).toInt()) {
            if (x <= leftX + 25) break
            var colGradSum = 0f
            for (y in yStart..yEnd) {
                colGradSum += grad[y * sampleW + x]
            }
            val avgColGrad = colGradSum / spanY
            if (avgColGrad > gradThreshold && avgColGrad > maxRightEnergy) {
                maxRightEnergy = avgColGrad
                rightX = x
            }
        }

        // Confidence scoring based on card size coverage & gradient strength
        val detectedW = (rightX - leftX).toFloat()
        val detectedH = (botY - topY).toFloat()
        val cardAreaRatio = (detectedW * detectedH) / (sampleW * sampleH)
        val aspectRatio = if (detectedH > 0) detectedW / detectedH else 1.75f

        val isValid = cardAreaRatio in 0.12f..0.98f && aspectRatio in 0.40f..3.0f

        val normTL: PointF
        val normTR: PointF
        val normBR: PointF
        val normBL: PointF
        val confidence: Float

        if (isValid) {
            val padX = (detectedW * 0.01f).toInt()
            val padY = (detectedH * 0.01f).toInt()

            val normMinX = ((leftX - padX).toFloat() / sampleW).coerceIn(0.01f, 0.45f)
            val normMaxX = ((rightX + padX).toFloat() / sampleW).coerceIn(0.55f, 0.99f)
            val normMinY = ((topY - padY).toFloat() / sampleH).coerceIn(0.01f, 0.45f)
            val normMaxY = ((botY + padY).toFloat() / sampleH).coerceIn(0.55f, 0.99f)

            normTL = PointF(normMinX, normMinY)
            normTR = PointF(normMaxX, normMinY)
            normBR = PointF(normMaxX, normMaxY)
            normBL = PointF(normMinX, normMaxY)
            confidence = if (maxTopEnergy > gradThreshold && maxBotEnergy > gradThreshold && maxLeftEnergy > gradThreshold && maxRightEnergy > gradThreshold) 0.92f else 0.78f
        } else {
            normTL = PointF(0.05f, 0.12f)
            normTR = PointF(0.95f, 0.12f)
            normBR = PointF(0.95f, 0.88f)
            normBL = PointF(0.05f, 0.88f)
            confidence = 0.50f
        }

        return CardQuadCorners(
            topLeft = normTL,
            topRight = normTR,
            bottomRight = normBR,
            bottomLeft = normBL,
            confidence = confidence
        )
    }

    /**
     * Checks if current corners are within the stability threshold compared to previous frame.
     */
    private fun checkStability(current: CardQuadCorners): Boolean {
        val prev = previousCorners ?: return false
        val dTL = distance(current.topLeft, prev.topLeft)
        val dTR = distance(current.topRight, prev.topRight)
        val dBR = distance(current.bottomRight, prev.bottomRight)
        val dBL = distance(current.bottomLeft, prev.bottomLeft)

        val maxMovement = max(max(dTL, dTR), max(dBR, dBL))
        return maxMovement < STABILITY_DISTANCE_THRESHOLD
    }

    private fun adjustCornersForRotation(quad: CardQuadCorners, rotationDegrees: Int): CardQuadCorners {
        return when (rotationDegrees) {
            90 -> CardQuadCorners(
                topLeft = PointF(1f - quad.bottomLeft.y, quad.bottomLeft.x),
                topRight = PointF(1f - quad.topLeft.y, quad.topLeft.x),
                bottomRight = PointF(1f - quad.topRight.y, quad.topRight.x),
                bottomLeft = PointF(1f - quad.bottomRight.y, quad.bottomRight.x),
                confidence = quad.confidence
            )
            180 -> CardQuadCorners(
                topLeft = PointF(1f - quad.bottomRight.x, 1f - quad.bottomRight.y),
                topRight = PointF(1f - quad.bottomLeft.x, 1f - quad.bottomLeft.y),
                bottomRight = PointF(1f - quad.topLeft.x, 1f - quad.topLeft.y),
                bottomLeft = PointF(1f - quad.topRight.x, 1f - quad.topRight.y),
                confidence = quad.confidence
            )
            270 -> CardQuadCorners(
                topLeft = PointF(quad.topRight.y, 1f - quad.topRight.x),
                topRight = PointF(quad.bottomRight.y, 1f - quad.bottomRight.x),
                bottomRight = PointF(quad.bottomLeft.y, 1f - quad.bottomLeft.x),
                bottomLeft = PointF(quad.topLeft.y, 1f - quad.topLeft.x),
                confidence = quad.confidence
            )
            else -> quad
        }
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
}
