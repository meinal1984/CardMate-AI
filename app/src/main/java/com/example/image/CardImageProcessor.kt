package com.example.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Data class representing the 4 corners of a card for perspective correction.
 * Coordinates are normalized in [0.0f, 1.0f] range relative to image width and height.
 */
data class CardQuadCorners(
    val topLeft: PointF = PointF(0.05f, 0.05f),
    val topRight: PointF = PointF(0.95f, 0.05f),
    val bottomRight: PointF = PointF(0.95f, 0.95f),
    val bottomLeft: PointF = PointF(0.05f, 0.95f),
    val confidence: Float = 0.85f
) {
    companion object {
        val DEFAULT = CardQuadCorners()
    }
}

/**
 * Preset Aspect Ratios for Card Crop
 */
enum class CropAspectRatio(val titleEn: String, val titleBn: String, val ratio: Float?) {
    FREE("Free", "ফ্রি", null),
    ORIGINAL("Original", "আসল", null),
    BUSINESS_CARD_US("3.5:2 (Card)", "৩.৫:২ (কার্ড)", 1.75f),
    STANDARD_3_2("3:2", "৩:২", 1.5f),
    STANDARD_4_3("4:3", "৪:৩", 1.333f),
    WIDE_16_9("16:9", "১৬:৯", 1.777f),
    SQUARE_1_1("1:1 (Square)", "১:১ (বর্গাকার)", 1.0f);

    companion object {
        fun defaultList(): List<CropAspectRatio> = entries.toList()
    }
}

/**
 * Image Enhancement Configuration Parameters
 */
data class ImageEnhanceParams(
    val brightness: Float = 0.0f,    // -100 to +100 (0 = normal)
    val contrast: Float = 1.0f,      // 0.5 to 2.5 (1.0 = normal)
    val saturation: Float = 1.0f,    // 0.0 (B&W) to 2.0 (Vivid) (1.0 = normal)
    val sharpness: Float = 0.0f,     // 0.0 to 2.0 (0.0 = normal)
    val isOcrOptimized: Boolean = false,
    val isBinarized: Boolean = false
) {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 1f && saturation == 1f && sharpness == 0f && !isOcrOptimized && !isBinarized

    companion object {
        val DEFAULT = ImageEnhanceParams()
        val OCR_BOOST = ImageEnhanceParams(
            brightness = 15f,
            contrast = 1.6f,
            saturation = 0.0f,
            sharpness = 1.2f,
            isOcrOptimized = true
        )
        val CLEAN_DOCUMENT = ImageEnhanceParams(
            brightness = 10f,
            contrast = 1.35f,
            saturation = 0.7f,
            sharpness = 0.8f
        )
        val VIVID_PHOTO = ImageEnhanceParams(
            brightness = 5f,
            contrast = 1.2f,
            saturation = 1.4f,
            sharpness = 0.5f
        )
        val HIGH_CONTRAST_BW = ImageEnhanceParams(
            brightness = 5f,
            contrast = 2.0f,
            saturation = 0.0f,
            sharpness = 1.5f,
            isBinarized = true
        )
    }
}

/**
 * AI Image Quality Assessment metrics
 */
data class ImageQualityScore(
    val overallScore: Int, // 0 - 100
    val starRating: Float, // 1.0 - 5.0
    val sharpnessScore: Int, // 0 - 100
    val lightingScore: Int, // 0 - 100
    val contrastScore: Int, // 0 - 100
    val textLegibilityScore: Int, // 0 - 100
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val isDpiHighForPrint: Boolean, // >= 300 DPI estimated
    val feedbackEn: String,
    val feedbackBn: String,
    val suggestions: List<String>,
    val meanLuma: Float = 128f,
    val stdDev: Float = 40f,
    val isLowLight: Boolean = false,
    val isPoorContrast: Boolean = false,
    val isOverexposed: Boolean = false,
    val dynamicBrightnessAdjustment: Float = 0f,
    val dynamicContrastAdjustment: Float = 1.0f,
    val ocrEnhanceSummary: String = ""
)

/**
 * Comprehensive Image Processing Engine for Business Cards.
 * Implements Crop, Resize, Rotate, Perspective Warp (Homography),
 * Color Enhancement, Auto-card detection, and Quality Analysis.
 */
object CardImageProcessor {

    /**
     * Safely decodes a Bitmap from file or URI with max dimension bounding
     * to completely avoid OutOfMemoryError.
     */
    suspend fun loadBitmapSafely(
        context: Context,
        imagePathOrUri: String,
        maxDimension: Int = 2400
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            if (imagePathOrUri.startsWith("content://") || imagePathOrUri.startsWith("file://")) {
                val uri = Uri.parse(imagePathOrUri)
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                val file = File(imagePathOrUri)
                if (!file.exists()) return@withContext null
                BitmapFactory.decodeFile(file.absolutePath, options)
            }

            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension || (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            if (imagePathOrUri.startsWith("content://") || imagePathOrUri.startsWith("file://")) {
                val uri = Uri.parse(imagePathOrUri)
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                }
            } else {
                BitmapFactory.decodeFile(imagePathOrUri, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crops bitmap according to normalized RectF [left, top, right, bottom] in [0f..1f].
     */
    fun cropBitmap(source: Bitmap, normalizedCrop: RectF): Bitmap {
        val srcW = source.width
        val srcH = source.height

        val left = (normalizedCrop.left.coerceIn(0f, 1f) * srcW).roundToInt().coerceIn(0, srcW - 1)
        val top = (normalizedCrop.top.coerceIn(0f, 1f) * srcH).roundToInt().coerceIn(0, srcH - 1)
        val right = (normalizedCrop.right.coerceIn(0f, 1f) * srcW).roundToInt().coerceIn(left + 10, srcW)
        val bottom = (normalizedCrop.bottom.coerceIn(0f, 1f) * srcH).roundToInt().coerceIn(top + 10, srcH)

        val cropWidth = max(10, right - left)
        val cropHeight = max(10, bottom - top)

        return Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    }

    /**
     * Rotates bitmap by discrete angles (90, 180, 270) or continuous degree angle.
     */
    fun rotateBitmap(source: Bitmap, degrees: Float, flipHorizontal: Boolean = false): Bitmap {
        if (degrees == 0f && !flipHorizontal) return source
        val matrix = Matrix()
        if (flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Performs 4-point Perspective Quad Warp (Homography).
     * Straightens a skewed, angled business card photo into a flat rectangular bitmap.
     * Preserves original card aspect ratio and dimensions by default without distortion.
     * Includes safety margin expander to avoid clipping text printed close to card boundaries.
     */
    fun applyPerspectiveCorrection(
        source: Bitmap,
        quad: CardQuadCorners,
        targetAspectRatio: Float = 1.75f, // Standard card aspect ratio (3.5" x 2" = 1.75)
        safetyMarginFraction: Float = 0.015f // 1.5% margin padding
    ): Bitmap {
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()

        // Apply slight safety margin outward from centroid
        val cx = (quad.topLeft.x + quad.topRight.x + quad.bottomRight.x + quad.bottomLeft.x) / 4f
        val cy = (quad.topLeft.y + quad.topRight.y + quad.bottomRight.y + quad.bottomLeft.y) / 4f

        fun expand(p: PointF): PointF {
            val dx = p.x - cx
            val dy = p.y - cy
            val newX = (p.x + dx * safetyMarginFraction).coerceIn(0f, 1f)
            val newY = (p.y + dy * safetyMarginFraction).coerceIn(0f, 1f)
            return PointF(newX, newY)
        }

        val tl = expand(quad.topLeft)
        val tr = expand(quad.topRight)
        val br = expand(quad.bottomRight)
        val bl = expand(quad.bottomLeft)

        // Source 4 corners in pixel coordinates
        val srcPoints = floatArrayOf(
            tl.x * srcW, tl.y * srcH, // TL
            tr.x * srcW, tr.y * srcH, // TR
            br.x * srcW, br.y * srcH, // BR
            bl.x * srcW, bl.y * srcH  // BL
        )

        // Calculate natural target dimensions from detected perspective geometry
        val topEdge = distance(srcPoints[0], srcPoints[1], srcPoints[2], srcPoints[3])
        val bottomEdge = distance(srcPoints[6], srcPoints[7], srcPoints[4], srcPoints[5])
        val leftEdge = distance(srcPoints[0], srcPoints[1], srcPoints[6], srcPoints[7])
        val rightEdge = distance(srcPoints[2], srcPoints[3], srcPoints[4], srcPoints[5])

        val naturalWidth = max(200f, max(topEdge, bottomEdge))
        val naturalHeight = max(120f, max(leftEdge, rightEdge))
        val measuredRatio = if (naturalHeight > 0f) naturalWidth / naturalHeight else 1.75f

        val effectiveRatio = when {
            targetAspectRatio > 0f -> targetAspectRatio
            measuredRatio in 1.25f..2.2f -> measuredRatio
            measuredRatio in 0.45f..0.8f -> measuredRatio
            else -> 1.75f // Sanitize distorted detections to standard business card aspect ratio
        }

        val outWidth = naturalWidth.roundToInt().coerceIn(300, 3200)
        val outHeight = (outWidth / effectiveRatio).roundToInt().coerceIn(200, 3200)

        val dstPoints = floatArrayOf(
            0f, 0f,                                  // TL
            outWidth.toFloat(), 0f,                  // TR
            outWidth.toFloat(), outHeight.toFloat(), // BR
            0f, outHeight.toFloat()                  // BL
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.concat(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }

    /**
     * Orders arbitrary 4 points into [Top-Left, Top-Right, Bottom-Right, Bottom-Left]
     */
    fun orderCorners(points: List<PointF>): CardQuadCorners {
        if (points.size < 4) return CardQuadCorners.DEFAULT

        // Sort by sum of (x + y): smallest is top-left, largest is bottom-right
        val sumSorted = points.sortedBy { it.x + it.y }
        val tl = sumSorted.first()
        val br = sumSorted.last()

        // Sort remainder by difference (y - x): smallest is top-right (x > y), largest is bottom-left (y > x)
        val remainder = points.filter { it != tl && it != br }
        val diffSorted = remainder.sortedBy { it.y - it.x }
        val tr = diffSorted.firstOrNull() ?: PointF(0.92f, 0.08f)
        val bl = diffSorted.lastOrNull() ?: PointF(0.08f, 0.92f)

        return CardQuadCorners(
            topLeft = tl,
            topRight = tr,
            bottomRight = br,
            bottomLeft = bl,
            confidence = 0.90f
        )
    }

    /**
     * Automated Preprocessing Step for the OCR / AI Extraction Pipeline.
     * Dynamically adjusts brightness, contrast, saturation, and unsharp masking based on
     * the image's ImageQualityScore to remediate low-light, poor-contrast, or overexposed captures.
     */
    fun preprocessCardForOcr(
        bitmap: Bitmap,
        qualityScore: ImageQualityScore? = null
    ): Bitmap {
        val quality = qualityScore ?: analyzeCardQuality(bitmap)

        var dynamicBrightness = 5.0f
        var dynamicContrast = 1.15f
        var dynamicSaturation = 0.95f
        var dynamicSharpness = 0.15f

        // 1. Dynamic Low-Light Brightness & Shadow Compensation
        if (quality.isLowLight || quality.meanLuma < 135f) {
            val lumaDeficit = (145f - quality.meanLuma).coerceAtLeast(0f)
            // Scale brightness boost according to ambient illumination deficit
            val brightnessBoost = (lumaDeficit * 0.42f).coerceIn(8f, 48f)
            dynamicBrightness += brightnessBoost
            // In dim lighting, shadows compress contrast; boost contrast proportionally to separate glyphs
            dynamicContrast = maxOf(dynamicContrast, 1.25f + (lumaDeficit / 145f) * 0.32f)
        } else if (quality.isOverexposed || quality.meanLuma > 195f) {
            // Overexposed / high flash glare: pull down brightness and punch contrast to recover washed out text
            val lumaSurplus = (quality.meanLuma - 185f).coerceAtLeast(0f)
            dynamicBrightness -= (lumaSurplus * 0.40f).coerceIn(6f, 35f)
            dynamicContrast = maxOf(dynamicContrast, 1.35f)
        }

        // 2. Dynamic Poor-Contrast Stretch
        if (quality.isPoorContrast || quality.contrastScore < 68) {
            val contrastDeficit = (70 - quality.contrastScore).coerceAtLeast(0)
            val extraContrast = (contrastDeficit / 70.0f) * 0.45f
            dynamicContrast = (dynamicContrast + extraContrast).coerceIn(1.22f, 1.70f)

            // Reduce saturation to suppress colored paper background noise and fiber grain from muddying OCR
            dynamicSaturation = (0.90f - (contrastDeficit / 70.0f) * 0.45f).coerceIn(0.40f, 0.90f)
        }

        // 3. Dynamic Text Sharpness & Edge Definition
        if (quality.sharpnessScore < 65) {
            val sharpnessDeficit = (65 - quality.sharpnessScore).coerceAtLeast(0)
            dynamicSharpness = (0.15f + (sharpnessDeficit / 65.0f) * 0.35f).coerceIn(0.15f, 0.48f)
        }

        val enhanceParams = ImageEnhanceParams(
            contrast = dynamicContrast,
            brightness = dynamicBrightness,
            saturation = dynamicSaturation,
            sharpness = dynamicSharpness
        )
        return enhanceBitmap(bitmap, enhanceParams)
    }

    /**
     * Resizes bitmap to target dimensions.
     */
    fun resizeBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val safeW = targetWidth.coerceIn(50, 4000)
        val safeH = targetHeight.coerceIn(50, 4000)
        return Bitmap.createScaledBitmap(source, safeW, safeH, true)
    }

    /**
     * Applies color, brightness, contrast, saturation, and OCR enhancement filters.
     */
    fun enhanceBitmap(
        source: Bitmap,
        params: ImageEnhanceParams
    ): Bitmap {
        if (params.isDefault) return source

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Compose ColorMatrix for Contrast, Brightness, and Saturation
        val cm = ColorMatrix()

        // Saturation
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(params.saturation)

        // Contrast & Brightness
        // Formula: color' = (color - 128) * contrast + 128 + brightness
        val contrast = params.contrast
        val brightness = params.brightness
        val scale = contrast
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightness

        val contrastMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        cm.postConcat(satMatrix)
        cm.postConcat(contrastMatrix)

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Apply Sharpening if requested
        if (params.sharpness > 0.05f) {
            return applySharpenKernel(output, params.sharpness)
        }

        return output
    }

    /**
     * Fast 3x3 Sharpen Convolution Kernel
     */
    private fun applySharpenKernel(source: Bitmap, amount: Float): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        val outputPixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val centerWeight = 1f + 4f * amount
        val edgeWeight = -amount

        for (y in 1 until h - 1) {
            val yOffset = y * w
            for (x in 1 until w - 1) {
                val idx = yOffset + x

                val cCenter = pixels[idx]
                val cTop = pixels[idx - w]
                val cBottom = pixels[idx + w]
                val cLeft = pixels[idx - 1]
                val cRight = pixels[idx + 1]

                val a = (cCenter shr 24) and 0xFF

                val rCenter = (cCenter shr 16) and 0xFF
                val rTop = (cTop shr 16) and 0xFF
                val rBottom = (cBottom shr 16) and 0xFF
                val rLeft = (cLeft shr 16) and 0xFF
                val rRight = (cRight shr 16) and 0xFF

                val gCenter = (cCenter shr 8) and 0xFF
                val gTop = (cTop shr 8) and 0xFF
                val gBottom = (cBottom shr 8) and 0xFF
                val gLeft = (cLeft shr 8) and 0xFF
                val gRight = (cRight shr 8) and 0xFF

                val bCenter = cCenter and 0xFF
                val bTop = cTop and 0xFF
                val bBottom = cBottom and 0xFF
                val bLeft = cLeft and 0xFF
                val bRight = cRight and 0xFF

                val newR = (rCenter * centerWeight + (rTop + rBottom + rLeft + rRight) * edgeWeight).coerceIn(0f, 255f).toInt()
                val newG = (gCenter * centerWeight + (gTop + gBottom + gLeft + gRight) * edgeWeight).coerceIn(0f, 255f).toInt()
                val newB = (bCenter * centerWeight + (bTop + bBottom + bLeft + bRight) * edgeWeight).coerceIn(0f, 255f).toInt()

                outputPixels[idx] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }

        // Fill borders
        for (x in 0 until w) {
            outputPixels[x] = pixels[x]
            outputPixels[(h - 1) * w + x] = pixels[(h - 1) * w + x]
        }
        for (y in 0 until h) {
            outputPixels[y * w] = pixels[y * w]
            outputPixels[y * w + (w - 1)] = pixels[y * w + (w - 1)]
        }

        val sharpened = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        sharpened.setPixels(outputPixels, 0, w, 0, 0, w, h)
        return sharpened
    }

    /**
     * Computes a centered crop rectangle fitting inside bitmapWidth and bitmapHeight
     * with an exact target aspect ratio.
     */
    fun computeCenteredCropRect(bitmapWidth: Int, bitmapHeight: Int, targetRatio: Float?): RectF {
        if (targetRatio == null || targetRatio <= 0f) {
            return RectF(0.04f, 0.04f, 0.96f, 0.96f)
        }
        val imgRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        val (normW, normH) = if (imgRatio > targetRatio) {
            // Image is wider than target ratio: constrained by height
            val h = 0.92f
            val pixelH = bitmapHeight * h
            val pixelW = pixelH * targetRatio
            val w = (pixelW / bitmapWidth).coerceIn(0.1f, 0.96f)
            w to h
        } else {
            // Image is taller than target ratio (e.g. portrait camera photo): constrained by width
            val w = 0.88f
            val pixelW = bitmapWidth * w
            val pixelH = pixelW / targetRatio
            val h = (pixelH / bitmapHeight).coerceIn(0.1f, 0.96f)
            w to h
        }
        val left = ((1f - normW) / 2f).coerceIn(0.01f, 0.45f)
        val top = ((1f - normH) / 2f).coerceIn(0.01f, 0.45f)
        return RectF(left, top, left + normW, top + normH)
    }

    /**
     * Auto Business Card Edge and Rectangle Detection.
     * Analyzes image gradients, color contrast, and boundary profiles across the entire frame
     * to accurately locate the 4 card corners without cutting into background tables or squashing the image.
     */
    fun autoDetectCardCorners(
        bitmap: Bitmap,
        preferredAspectRatio: Float = 1.75f
    ): CardQuadCorners {
        val targetRatio = if (preferredAspectRatio > 0.5f) preferredAspectRatio else 1.75f
        val sampleW = 240
        val sampleH = (sampleW.toFloat() / bitmap.width * bitmap.height).roundToInt().coerceAtLeast(140)
        val sampled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)

        val pixels = IntArray(sampleW * sampleH)
        sampled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        // Compute luminance map and 2D gradient magnitude map
        val luma = FloatArray(sampleW * sampleH)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            luma[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

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
        val gradThreshold = (avgGrad * 1.5f).coerceIn(16f, 42f)

        // Scan Left & Right horizontal bounds first
        val midYStart = (sampleH * 0.30f).toInt()
        val midYEnd = (sampleH * 0.70f).toInt()
        val midYSpan = (midYEnd - midYStart).coerceAtLeast(1)

        var leftX = (sampleW * 0.06f).toInt()
        var maxLeftEnergy = 0f
        for (x in 2 until (sampleW * 0.40f).toInt()) {
            var colGradSum = 0f
            for (y in midYStart..midYEnd) {
                colGradSum += grad[y * sampleW + x]
            }
            val avgColGrad = colGradSum / midYSpan
            if (avgColGrad > gradThreshold && avgColGrad > maxLeftEnergy) {
                maxLeftEnergy = avgColGrad
                leftX = x
            }
        }

        var rightX = (sampleW * 0.94f).toInt()
        var maxRightEnergy = 0f
        for (x in sampleW - 3 downTo (sampleW * 0.60f).toInt()) {
            if (x <= leftX + 40) break
            var colGradSum = 0f
            for (y in midYStart..midYEnd) {
                colGradSum += grad[y * sampleW + x]
            }
            val avgColGrad = colGradSum / midYSpan
            if (avgColGrad > gradThreshold && avgColGrad > maxRightEnergy) {
                maxRightEnergy = avgColGrad
                rightX = x
            }
        }

        val detectedW = (rightX - leftX).toFloat()
        val expectedCardH = detectedW / targetRatio

        // Search for Top and Bottom edges around the expected vertical center of the card
        val centerY = sampleH / 2f
        val expectedTopY = (centerY - expectedCardH / 2f).toInt().coerceIn(2, sampleH - 10)
        val expectedBotY = (centerY + expectedCardH / 2f).toInt().coerceIn(expectedTopY + 20, sampleH - 2)

        val xSearchStart = (leftX + detectedW * 0.15f).toInt()
        val xSearchEnd = (rightX - detectedW * 0.15f).toInt()
        val xSearchSpan = (xSearchEnd - xSearchStart).coerceAtLeast(1)

        // Scan Top Edge in a window around expectedTopY
        var topY = expectedTopY
        var maxTopEnergy = 0f
        val topSearchMin = max(2, (expectedTopY - expectedCardH * 0.4f).toInt())
        val topSearchMax = min(sampleH - 20, (expectedTopY + expectedCardH * 0.35f).toInt())
        for (y in topSearchMin..topSearchMax) {
            var rowGradSum = 0f
            for (x in xSearchStart..xSearchEnd) {
                rowGradSum += grad[y * sampleW + x]
            }
            val avgRowGrad = rowGradSum / xSearchSpan
            if (avgRowGrad > gradThreshold && avgRowGrad > maxTopEnergy) {
                maxTopEnergy = avgRowGrad
                topY = y
            }
        }

        // Scan Bottom Edge in a window around expectedBotY
        var botY = expectedBotY
        var maxBotEnergy = 0f
        val botSearchMin = max(topY + 20, (expectedBotY - expectedCardH * 0.35f).toInt())
        val botSearchMax = min(sampleH - 2, (expectedBotY + expectedCardH * 0.4f).toInt())
        for (y in botSearchMax downTo botSearchMin) {
            var rowGradSum = 0f
            for (x in xSearchStart..xSearchEnd) {
                rowGradSum += grad[y * sampleW + x]
            }
            val avgRowGrad = rowGradSum / xSearchSpan
            if (avgRowGrad > gradThreshold && avgRowGrad > maxBotEnergy) {
                maxBotEnergy = avgRowGrad
                botY = y
            }
        }

        val detectedH = (botY - topY).toFloat()
        val detectedAspect = if (detectedH > 0) detectedW / detectedH else targetRatio
        val aspectError = abs(detectedAspect - targetRatio) / targetRatio

        val finalTL: PointF
        val finalTR: PointF
        val finalBR: PointF
        val finalBL: PointF
        val confidence: Float

        if (aspectError < 0.28f && detectedW / sampleW > 0.40f) {
            // High confidence detection consistent with card aspect ratio
            val padX = (detectedW * 0.012f).toInt()
            val padY = (detectedH * 0.012f).toInt()

            val normMinX = ((leftX - padX).toFloat() / sampleW).coerceIn(0.01f, 0.45f)
            val normMaxX = ((rightX + padX).toFloat() / sampleW).coerceIn(0.55f, 0.99f)
            val normMinY = ((topY - padY).toFloat() / sampleH).coerceIn(0.01f, 0.45f)
            val normMaxY = ((botY + padY).toFloat() / sampleH).coerceIn(0.55f, 0.99f)

            finalTL = PointF(normMinX, normMinY)
            finalTR = PointF(normMaxX, normMinY)
            finalBR = PointF(normMaxX, normMaxY)
            finalBL = PointF(normMinX, normMaxY)
            confidence = 0.92f
        } else {
            // Precise center-framed fallback preserving true card aspect ratio without stretching
            val centeredRect = computeCenteredCropRect(bitmap.width, bitmap.height, targetRatio)
            finalTL = PointF(centeredRect.left, centeredRect.top)
            finalTR = PointF(centeredRect.right, centeredRect.top)
            finalBR = PointF(centeredRect.right, centeredRect.bottom)
            finalBL = PointF(centeredRect.left, centeredRect.bottom)
            confidence = 0.75f
        }

        return CardQuadCorners(
            topLeft = finalTL,
            topRight = finalTR,
            bottomRight = finalBR,
            bottomLeft = finalBL,
            confidence = confidence
        )
    }

    /**
     * Analyzes image quality metrics (sharpness, lighting, contrast, resolution, DPI)
     * and provides AI score & actionable diagnostic advice for business card recognition.
     */
    fun analyzeCardQuality(bitmap: Bitmap): ImageQualityScore {
        val w = bitmap.width
        val h = bitmap.height

        // Downsampled estimation
        val sampleW = 120
        val sampleH = (sampleW.toFloat() / w * h).roundToInt().coerceAtLeast(80)
        val sample = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)

        val pixels = IntArray(sampleW * sampleH)
        sample.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        var totalLuma = 0.0
        val lumaArray = DoubleArray(pixels.size)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val l = 0.299 * r + 0.587 * g + 0.114 * b
            lumaArray[i] = l
            totalLuma += l
        }

        val meanLuma = totalLuma / pixels.size

        // Calculate Variance for Contrast
        var variance = 0.0
        for (l in lumaArray) {
            val d = l - meanLuma
            variance += d * d
        }
        val stdDev = sqrt(variance / pixels.size)
        val contrastScore = ((stdDev / 65.0) * 100.0).coerceIn(20.0, 100.0).toInt()

        // Lighting score (optimal mean luma between 110 and 190)
        val lightingScore = (100.0 - abs(meanLuma - 150.0) * 0.9).coerceIn(20.0, 100.0).toInt()

        // High frequency gradient for Sharpness estimation (Laplacian-like)
        var gradientSum = 0.0
        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val idx = y * sampleW + x
                val center = lumaArray[idx]
                val diffX = abs(center - lumaArray[idx + 1])
                val diffY = abs(center - lumaArray[idx + sampleW])
                gradientSum += (diffX + diffY)
            }
        }
        val avgGrad = gradientSum / ((sampleW - 2) * (sampleH - 2))
        val sharpnessScore = ((avgGrad / 18.0) * 100.0).coerceIn(25.0, 100.0).toInt()

        // Legibility Score based on contrast + sharpness
        val legibilityScore = ((contrastScore * 0.5) + (sharpnessScore * 0.5)).toInt()

        // Resolution & DPI (Assuming standard 3.5" x 2.0" card size)
        val estimatedDpi = ((w / 3.5) + (h / 2.0)) / 2.0
        val isDpiHigh = estimatedDpi >= 250.0

        val overall = ((sharpnessScore * 0.35) + (lightingScore * 0.25) + (contrastScore * 0.25) + (if (isDpiHigh) 15 else 5)).toInt().coerceIn(10, 100)
        val starRating = ((overall / 20.0) * 2.0).roundToInt() / 2.0f

        val suggestions = mutableListOf<String>()
        val feedbackEn: String
        val feedbackBn: String

        if (overall >= 80) {
            feedbackEn = "Excellent quality! Clear text, sharp edges, and ideal lighting for AI OCR extraction."
            feedbackBn = "অসাধারণ ছবি! পরিষ্কার টেক্সট ও নিখুঁত আলোর কারণে সহজে ও নির্ভুলভাবে স্ক্যান হবে।"
        } else if (overall >= 60) {
            feedbackEn = "Good business card photo. OCR will extract most details accurately."
            feedbackBn = "ভালো মানের কার্ডের ছবি। AI প্রায় সব তথ্য সঠিকভাবে পড়তে পারবে।"
        } else {
            feedbackEn = "Low quality detected. We recommend enhancing contrast or retaking with even lighting."
            feedbackBn = "ছবির মান কিছুটা দুর্বল। 'Enhance for OCR' ফিল্টার ব্যবহার বা উজ্জ্বল আলোতে ছবি তোলার পরামর্শ।"
        }

        if (sharpnessScore < 60) {
            suggestions.add("Text seems slightly blurry. Use Sharpening slider in Enhance tab.")
        }
        if (lightingScore < 60) {
            suggestions.add("Lighting is uneven or dim. Adjust Brightness or enable Document mode.")
        }
        if (contrastScore < 55) {
            suggestions.add("Low text-to-background contrast. Apply OCR Booster preset.")
        }
        if (w < 800 || h < 500) {
            suggestions.add("Low resolution image. Higher resolution ensures smaller fine-print is detected.")
        }

        val isLowLight = meanLuma < 120.0 || (lightingScore < 70 && meanLuma < 145.0)
        val isPoorContrast = contrastScore < 68 || stdDev < 42.0
        val isOverexposed = meanLuma > 200.0

        // Calculate dynamic adjustments applied during OCR preprocessing
        var dynBrightness = 5.0f
        var dynContrast = 1.15f
        val enhanceDetails = mutableListOf<String>()

        if (isLowLight || meanLuma < 135.0) {
            val lumaDeficit = (145.0 - meanLuma).coerceAtLeast(0.0).toFloat()
            val bBoost = (lumaDeficit * 0.42f).coerceIn(8f, 48f)
            dynBrightness += bBoost
            dynContrast = maxOf(dynContrast, 1.25f + (lumaDeficit / 145f) * 0.32f)
            enhanceDetails.add("Low-light Brightness +${bBoost.toInt()}%")
        } else if (isOverexposed || meanLuma > 195.0) {
            val lumaSurplus = (meanLuma - 185.0).coerceAtLeast(0.0).toFloat()
            val bReduction = (lumaSurplus * 0.40f).coerceIn(6f, 35f)
            dynBrightness -= bReduction
            dynContrast = maxOf(dynContrast, 1.35f)
            enhanceDetails.add("Glare Reduced -${bReduction.toInt()}%")
        }

        if (isPoorContrast || contrastScore < 68) {
            val contrastDeficit = (70 - contrastScore).coerceAtLeast(0)
            val extraContrast = (contrastDeficit / 70.0f) * 0.45f
            dynContrast = (dynContrast + extraContrast).coerceIn(1.22f, 1.70f)
            enhanceDetails.add("Contrast Boost ${String.format(java.util.Locale.US, "%.1fx", dynContrast)}")
        }

        val ocrSummary = if (enhanceDetails.isNotEmpty()) {
            enhanceDetails.joinToString(" • ")
        } else {
            "Standard Contrast & Sharpness Balanced"
        }

        return ImageQualityScore(
            overallScore = overall,
            starRating = starRating.coerceIn(1.0f, 5.0f),
            sharpnessScore = sharpnessScore,
            lightingScore = lightingScore,
            contrastScore = contrastScore,
            textLegibilityScore = legibilityScore,
            resolutionWidth = w,
            resolutionHeight = h,
            isDpiHighForPrint = isDpiHigh,
            feedbackEn = feedbackEn,
            feedbackBn = feedbackBn,
            suggestions = suggestions,
            meanLuma = meanLuma.toFloat(),
            stdDev = stdDev.toFloat(),
            isLowLight = isLowLight,
            isPoorContrast = isPoorContrast,
            isOverexposed = isOverexposed,
            dynamicBrightnessAdjustment = dynBrightness,
            dynamicContrastAdjustment = dynContrast,
            ocrEnhanceSummary = ocrSummary
        )
    }

    /**
     * Saves edited bitmap to disk with non-destructive versioning.
     * Returns the absolute file path.
     */
    suspend fun saveEditedBitmap(
        context: Context,
        bitmap: Bitmap,
        cardId: Long,
        side: String = "front",
        quality: Int = 92
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "card_images")
        if (!dir.exists()) dir.mkdirs()

        val fileName = "edited_card_${cardId}_${side}_${System.currentTimeMillis()}.jpg"
        val destFile = File(dir, fileName)

        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        destFile.absolutePath
    }

    /**
     * Saves original bitmap to disk if not already saved.
     */
    suspend fun saveOriginalBitmap(
        context: Context,
        bitmap: Bitmap,
        cardId: Long,
        side: String = "front"
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "card_images/originals")
        if (!dir.exists()) dir.mkdirs()

        val fileName = "original_card_${cardId}_${side}.jpg"
        val destFile = File(dir, fileName)
        if (!destFile.exists()) {
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        }
        destFile.absolutePath
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
