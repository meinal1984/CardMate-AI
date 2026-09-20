package com.example.image

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Result of physical card visual theme extraction.
 */
data class ExtractedCardTheme(
    val primaryBgColor: Long,
    val secondaryBgColor: Long,
    val accentColor: Long,
    val textColor: Long,
    val layoutStyle: String,
    val bgPattern: String,
    val matchedTemplate: String,
    val themeDescriptionEn: String,
    val themeDescriptionBn: String,
    val isLightCard: Boolean
)

/**
 * Intelligent analyzer that samples physical business card photos and extracts
 * exact background colors, brand accents, contrast text colors, and matching themes.
 */
object CardThemeColorExtractor {

    /**
     * Extracts exact colors and design aesthetic directly from a business card bitmap.
     */
    fun extractThemeFromBitmap(bitmap: Bitmap): ExtractedCardTheme {
        // Downscale for fast and noise-resistant pixel sampling
        val targetWidth = 240
        val targetHeight = max(80, (targetWidth.toFloat() * bitmap.height / bitmap.width).roundToInt())
        val scaled = if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val width = scaled.width
        val height = scaled.height

        // 1. Sample perimeter and corner margins (where business cards never have body text)
        val bgSamples = mutableListOf<Int>()

        // 4 Corners with insets
        val cornerInsetsX = listOf(0.08f, 0.12f, 0.16f)
        val cornerInsetsY = listOf(0.08f, 0.12f, 0.16f)
        for (ix in cornerInsetsX) {
            for (iy in cornerInsetsY) {
                // Top-Left
                bgSamples.add(scaled.getPixel((width * ix).toInt().coerceIn(0, width - 1), (height * iy).toInt().coerceIn(0, height - 1)))
                // Top-Right
                bgSamples.add(scaled.getPixel((width * (1f - ix)).toInt().coerceIn(0, width - 1), (height * iy).toInt().coerceIn(0, height - 1)))
                // Bottom-Left
                bgSamples.add(scaled.getPixel((width * ix).toInt().coerceIn(0, width - 1), (height * (1f - iy)).toInt().coerceIn(0, height - 1)))
                // Bottom-Right
                bgSamples.add(scaled.getPixel((width * (1f - ix)).toInt().coerceIn(0, width - 1), (height * (1f - iy)).toInt().coerceIn(0, height - 1)))
            }
        }

        // Top and bottom border margins
        for (step in 2..8) {
            val ratio = step / 10f
            bgSamples.add(scaled.getPixel((width * ratio).toInt().coerceIn(0, width - 1), (height * 0.06f).toInt().coerceIn(0, height - 1)))
            bgSamples.add(scaled.getPixel((width * ratio).toInt().coerceIn(0, width - 1), (height * 0.94f).toInt().coerceIn(0, height - 1)))
            bgSamples.add(scaled.getPixel((width * 0.06f).toInt().coerceIn(0, width - 1), (height * ratio).toInt().coerceIn(0, height - 1)))
            bgSamples.add(scaled.getPixel((width * 0.94f).toInt().coerceIn(0, width - 1), (height * ratio).toInt().coerceIn(0, height - 1)))
        }

        // Determine dominant background color from samples using clustering
        val dominantBgPixel = findDominantColor(bgSamples)
        val bgR = Color.red(dominantBgPixel)
        val bgG = Color.green(dominantBgPixel)
        val bgB = Color.blue(dominantBgPixel)

        // Calculate background luminance
        val bgLum = (0.299 * bgR + 0.587 * bgG + 0.114 * bgB) / 255.0
        val isLightCard = bgLum > 0.55

        // Refine primary background color
        val primaryBgColor: Long = when {
            // Pristine White / Near-White card (standard corporate card)
            bgLum > 0.88 && isColorNeutral(bgR, bgG, bgB, tolerance = 25) -> 0xFFFFFFFFL
            // Ivory / Cream warm card
            bgLum > 0.78 && (bgR >= bgB + 20) -> 0xFFFFFBEBL
            // Ultra-dark / Pitch Black card
            bgLum < 0.12 && isColorNeutral(bgR, bgG, bgB, tolerance = 18) -> 0xFF0F172AL
            // Dark Slate / Charcoal card
            bgLum < 0.22 -> (0xFF000000L or (bgR.toLong() shl 16) or (bgG.toLong() shl 8) or bgB.toLong())
            // Retain exact sampled color with full opacity
            else -> (0xFF000000L or (bgR.toLong() shl 16) or (bgG.toLong() shl 8) or bgB.toLong())
        }

        // 2. Scan central 80% of the card for brand accent colors (logos, highlight text, brand marks)
        val startX = (width * 0.10f).toInt()
        val endX = (width * 0.90f).toInt()
        val startY = (height * 0.10f).toInt()
        val endY = (height * 0.90f).toInt()

        val chromaticPixels = mutableListOf<Int>()
        val hsv = FloatArray(3)

        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val pixel = scaled.getPixel(x, y)
                val pr = Color.red(pixel)
                val pg = Color.green(pixel)
                val pb = Color.blue(pixel)

                // Skip background-like pixels
                val distToBg = colorDistance(pr, pg, pb, bgR, bgG, bgB)
                if (distToBg > 40) {
                    Color.colorToHSV(pixel, hsv)
                    val sat = hsv[1]
                    val value = hsv[2]

                    // A chromatic brand accent has notable saturation and is neither pitch black nor pure white
                    if (sat >= 0.22f && value in 0.20f..0.98f) {
                        chromaticPixels.add(pixel)
                    }
                }
                x += 3
            }
            y += 3
        }

        // Determine accent color
        val accentColor: Long
        val hasDistinctBrandColor = chromaticPixels.isNotEmpty()

        if (hasDistinctBrandColor) {
            // Group chromatic pixels into 12 hue bins to find prominent brand color
            val dominantAccentPixel = findDominantHueColor(chromaticPixels)
            var ar = Color.red(dominantAccentPixel)
            var ag = Color.green(dominantAccentPixel)
            var ab = Color.blue(dominantAccentPixel)

            // Ensure accent has good contrast against the background
            val accentLum = (0.299 * ar + 0.587 * ag + 0.114 * ab) / 255.0
            if (isLightCard && accentLum > 0.70) {
                // Darken slightly for visibility on light card
                ar = (ar * 0.75f).toInt().coerceIn(0, 255)
                ag = (ag * 0.75f).toInt().coerceIn(0, 255)
                ab = (ab * 0.75f).toInt().coerceIn(0, 255)
            } else if (!isLightCard && accentLum < 0.35) {
                // Brighten slightly for visibility on dark card
                ar = min(255, (ar * 1.45f).toInt())
                ag = min(255, (ag * 1.45f).toInt())
                ab = min(255, (ab * 1.45f).toInt())
            }

            accentColor = (0xFF000000L or (ar.toLong() shl 16) or (ag.toLong() shl 8) or ab.toLong())
        } else {
            // Monochrome card without saturated colors
            accentColor = if (isLightCard) {
                0xFF1E40AFL // Deep Royal Blue for crisp professional contrast
            } else {
                0xFFEAB308L // Elegant Gold for luxury dark contrast
            }
        }

        // 3. Contrast Text Color
        val textColor: Long = if (isLightCard) {
            0xFF0F172AL // High-contrast deep slate for light background
        } else {
            0xFFF8FAFCL // High-contrast crisp off-white for dark background
        }

        // 4. Secondary Background Color (harmonious gradient or panel shade)
        val secondaryBgColor: Long = if (isLightCard) {
            if (primaryBgColor == 0xFFFFFBEBL) {
                0xFFFEF3C7L // Warmer ivory secondary
            } else {
                0xFFF1F5F9L // Crisp clean light secondary
            }
        } else {
            // Dark card secondary
            val sr = min(255, (bgR * 1.25f + 15).toInt())
            val sg = min(255, (bgG * 1.25f + 20).toInt())
            val sb = min(255, (bgB * 1.25f + 25).toInt())
            (0xFF000000L or (sr.toLong() shl 16) or (sg.toLong() shl 8) or sb.toLong())
        }

        // 5. Template and Layout Aesthetic Matching
        val matchedTemplate: String
        val layoutStyle: String
        val bgPattern: String
        val themeDescEn: String
        val themeDescBn: String

        if (isLightCard) {
            if (primaryBgColor == 0xFFFFFBEBL) {
                matchedTemplate = "royal_cream"
                layoutStyle = "executive_classic"
                bgPattern = "solid"
                themeDescEn = "Ivory Elegance"
                themeDescBn = "আইভরি গোল্ড থিম"
            } else {
                matchedTemplate = "pristine_white"
                layoutStyle = "minimalist_clean"
                bgPattern = "solid"
                themeDescEn = "Pristine White Minimal"
                themeDescBn = "হোয়াইট মিনিমাল থিম"
            }
        } else {
            // Dark card
            val aHsv = FloatArray(3)
            val acInt = accentColor.toInt()
            Color.colorToHSV(acInt, aHsv)
            val hue = aHsv[0]

            when {
                // Gold / Yellow / Amber
                hue in 35f..65f -> {
                    matchedTemplate = "executive_gold"
                    layoutStyle = "executive_classic"
                    bgPattern = "gradient"
                    themeDescEn = "Executive Gold Luxury"
                    themeDescBn = "এক্সিকিউটিভ গোল্ড থিম"
                }
                // Cyan / Neon / Purple
                hue in 170f..285f && aHsv[1] > 0.5f -> {
                    matchedTemplate = "cyber_neon"
                    layoutStyle = "cyber_badge"
                    bgPattern = "mesh"
                    themeDescEn = "Cyber Neon Tech"
                    themeDescBn = "সাইবার নিয়ন টেক থিম"
                }
                // Teal / Green
                hue in 130f..175f -> {
                    matchedTemplate = "ocean_gradient"
                    layoutStyle = "modern_floating"
                    bgPattern = "gradient"
                    themeDescEn = "Ocean Breeze Teal"
                    themeDescBn = "ওশান ব্রিজ টিল থিম"
                }
                // Brown / Bronze / Warm Orange
                hue in 15f..35f -> {
                    matchedTemplate = "vintage_kraft"
                    layoutStyle = "executive_classic"
                    bgPattern = "gradient"
                    themeDescEn = "Executive Bronze"
                    themeDescBn = "ব্রোঞ্জ প্রিমিয়াম থিম"
                }
                // Default Dark
                else -> {
                    matchedTemplate = "modern_slate"
                    layoutStyle = "modern_floating"
                    bgPattern = "gradient"
                    themeDescEn = "Modern Slate"
                    themeDescBn = "মডার্ন স্লেট থিম"
                }
            }
        }

        return ExtractedCardTheme(
            primaryBgColor = primaryBgColor,
            secondaryBgColor = secondaryBgColor,
            accentColor = accentColor,
            textColor = textColor,
            layoutStyle = layoutStyle,
            bgPattern = bgPattern,
            matchedTemplate = matchedTemplate,
            themeDescriptionEn = themeDescEn,
            themeDescriptionBn = themeDescBn,
            isLightCard = isLightCard
        )
    }

    private fun isColorNeutral(r: Int, g: Int, b: Int, tolerance: Int): Boolean {
        val maxDiff = max(abs(r - g), max(abs(r - b), abs(g - b)))
        return maxDiff <= tolerance
    }

    private fun colorDistance(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
        val dr = (r1 - r2).toDouble()
        val dg = (g1 - g2).toDouble()
        val db = (b1 - b2).toDouble()
        return sqrt(dr * dr + dg * dg + db * db)
    }

    private fun findDominantColor(pixels: List<Int>): Int {
        if (pixels.isEmpty()) return Color.WHITE
        val clusters = mutableListOf<Pair<Int, MutableList<Int>>>()

        for (pix in pixels) {
            val r = Color.red(pix)
            val g = Color.green(pix)
            val b = Color.blue(pix)

            var matched = false
            for (cluster in clusters) {
                val cr = Color.red(cluster.first)
                val cg = Color.green(cluster.first)
                val cb = Color.blue(cluster.first)
                if (colorDistance(r, g, b, cr, cg, cb) < 32) {
                    cluster.second.add(pix)
                    matched = true
                    break
                }
            }

            if (!matched) {
                clusters.add(Pair(pix, mutableListOf(pix)))
            }
        }

        // Find largest cluster
        val largest = clusters.maxByOrNull { it.second.size } ?: return pixels.first()
        val members = largest.second
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        for (m in members) {
            totalR += Color.red(m)
            totalG += Color.green(m)
            totalB += Color.blue(m)
        }
        val count = members.size
        return Color.rgb((totalR / count).toInt(), (totalG / count).toInt(), (totalB / count).toInt())
    }

    private fun findDominantHueColor(pixels: List<Int>): Int {
        if (pixels.isEmpty()) return Color.rgb(37, 99, 235) // Fallback Blue
        val hueBins = Array(12) { mutableListOf<Int>() }
        val hsv = FloatArray(3)

        for (pix in pixels) {
            Color.colorToHSV(pix, hsv)
            val bin = ((hsv[0] % 360f) / 30f).toInt().coerceIn(0, 11)
            hueBins[bin].add(pix)
        }

        val largestBin = hueBins.maxByOrNull { it.size } ?: return pixels.first()
        if (largestBin.isEmpty()) return pixels.first()

        // Pick the most saturated pixel in the dominant hue bin
        return largestBin.maxByOrNull { pix ->
            Color.colorToHSV(pix, hsv)
            hsv[1] * 0.7f + hsv[2] * 0.3f
        } ?: largestBin.first()
    }

    /**
     * Parses a hex color string like "#FFFFFF" or "0xFFFFFFFF" to Long with fallback.
     */
    fun parseHexColor(hex: String?, fallback: Long): Long {
        if (hex.isNullOrBlank()) return fallback
        val clean = hex.trim().removePrefix("#").removePrefix("0x")
        return try {
            when (clean.length) {
                6 -> 0xFF000000L or clean.toLong(16)
                8 -> clean.toLong(16)
                else -> fallback
            }
        } catch (e: Exception) {
            fallback
        }
    }
}
