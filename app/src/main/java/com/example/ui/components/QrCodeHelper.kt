package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.BusinessCard
import com.example.data.model.UserProfile
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.EnumMap

/**
 * Data representation of contact details for generating dynamic QR codes.
 */
data class ContactQrData(
    val fullName: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val phone: String = "",
    val secondaryPhone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val notes: String = ""
) {
    val isBlank: Boolean
        get() = fullName.isBlank() && phone.isBlank() && email.isBlank() && company.isBlank()

    val displaySubtitle: String
        get() = when {
            jobTitle.isNotBlank() && company.isNotBlank() -> "$jobTitle • $company"
            jobTitle.isNotBlank() -> jobTitle
            company.isNotBlank() -> company
            else -> ""
        }
}

/**
 * Format options for encoding contact details into QR payloads.
 */
enum class QrContactFormat(val label: String, val shortDesc: String) {
    VCARD_3_0("vCard 3.0", "Full digital contact card (Auto-saves to Contacts)"),
    MECARD("MECARD", "Compact format for ultra-fast scanning"),
    PLAIN_TEXT("Plain Text", "Simple readable text contact card")
}

/**
 * Modern color themes for custom QR codes.
 */
enum class QrThemeColor(val title: String, val hexLong: Long, val darkInt: Int) {
    CLASSIC_INK("Classic Ink", 0xFF0F172A, 0xFF0F172A.toInt()),
    CARDMATE_TEAL("CardMate Teal", 0xFF0D9488, 0xFF0D9488.toInt()),
    ELECTRIC_CYAN("Electric Cyan", 0xFF0891B2, 0xFF0891B2.toInt()),
    ROYAL_INDIGO("Royal Indigo", 0xFF4338CA, 0xFF4338CA.toInt()),
    EMERALD_GREEN("Emerald", 0xFF059669, 0xFF059669.toInt()),
    RUBY_BURGUNDY("Ruby Burgundy", 0xFFBE123C, 0xFFBE123C.toInt()),
    DEEP_VIOLET("Deep Violet", 0xFF7C3AED, 0xFF7C3AED.toInt()),
    MIDNIGHT_NAVY("Midnight Navy", 0xFF1E3A8A, 0xFF1E3A8A.toInt())
}

/**
 * Center badge / logo overlay options.
 */
enum class QrCenterBadge(val title: String) {
    NONE("None"),
    CARDMATE_LOGO("CardMate Emblem"),
    USER_INITIALS("Contact Initials"),
    CONTACT_ICON("User Icon")
}

fun BusinessCard.toContactQrData(): ContactQrData = ContactQrData(
    fullName = this.fullName,
    jobTitle = this.jobTitle,
    company = this.company,
    phone = this.phone,
    secondaryPhone = this.secondaryPhone,
    email = this.email,
    website = this.website,
    address = this.address,
    notes = this.notes
)

fun UserProfile.toContactQrData(): ContactQrData = ContactQrData(
    fullName = this.fullName,
    jobTitle = this.jobTitle,
    company = this.company,
    phone = this.phone,
    secondaryPhone = this.secondaryPhone,
    email = this.email,
    website = this.website,
    address = this.address,
    notes = this.bio
)

object QrCodeHelper {

    /**
     * Standard backward-compatible QR bitmap generator.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = android.graphics.Color.BLACK,
        lightColor: Int = android.graphics.Color.WHITE
    ): Bitmap? {
        return generateCustomQrBitmap(
            content = content,
            sizePx = sizePx,
            darkColor = darkColor,
            lightColor = lightColor,
            errorCorrection = ErrorCorrectionLevel.M,
            margin = 1
        )
    }

    /**
     * Builds standard QR payload for given contact data and format.
     */
    fun buildContactPayload(data: ContactQrData, format: QrContactFormat): String {
        return when (format) {
            QrContactFormat.VCARD_3_0 -> buildVCard3(data)
            QrContactFormat.MECARD -> buildMeCard(data)
            QrContactFormat.PLAIN_TEXT -> buildPlainText(data)
        }
    }

    private fun buildVCard3(data: ContactQrData): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("FN:${data.fullName.trim()}")
        if (data.jobTitle.isNotBlank()) appendLine("TITLE:${data.jobTitle.trim()}")
        if (data.company.isNotBlank()) appendLine("ORG:${data.company.trim()}")
        if (data.phone.isNotBlank()) appendLine("TEL;TYPE=WORK,VOICE:${data.phone.trim()}")
        if (data.secondaryPhone.isNotBlank()) appendLine("TEL;TYPE=CELL:${data.secondaryPhone.trim()}")
        if (data.email.isNotBlank()) appendLine("EMAIL;TYPE=WORK,INTERNET:${data.email.trim()}")
        if (data.website.isNotBlank()) appendLine("URL:${data.website.trim()}")
        if (data.address.isNotBlank()) appendLine("ADR;TYPE=WORK:;;${data.address.replace("\n", " ").trim()};;;;")
        if (data.notes.isNotBlank()) appendLine("NOTE:${data.notes.trim()}")
        appendLine("PRODID:-//CardMate AI//Smart QR Generator//EN")
        appendLine("END:VCARD")
    }

    private fun buildMeCard(data: ContactQrData): String = buildString {
        append("MECARD:")
        if (data.fullName.isNotBlank()) append("N:${escapeMeCard(data.fullName)};")
        if (data.phone.isNotBlank()) append("TEL:${escapeMeCard(data.phone)};")
        if (data.email.isNotBlank()) append("EMAIL:${escapeMeCard(data.email)};")
        if (data.company.isNotBlank()) append("ORG:${escapeMeCard(data.company)};")
        if (data.jobTitle.isNotBlank()) append("TIL:${escapeMeCard(data.jobTitle)};")
        if (data.website.isNotBlank()) append("URL:${escapeMeCard(data.website)};")
        if (data.address.isNotBlank()) append("ADR:${escapeMeCard(data.address.replace("\n", " "))};")
        if (data.notes.isNotBlank()) append("NOTE:${escapeMeCard(data.notes)};")
        append(";")
    }

    private fun escapeMeCard(value: String): String {
        return value.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(":", "\\:")
            .replace(",", "\\,")
    }

    private fun buildPlainText(data: ContactQrData): String = buildString {
        appendLine("Contact: ${data.fullName}")
        if (data.jobTitle.isNotBlank()) appendLine("Title: ${data.jobTitle}")
        if (data.company.isNotBlank()) appendLine("Company: ${data.company}")
        if (data.phone.isNotBlank()) appendLine("Phone: ${data.phone}")
        if (data.email.isNotBlank()) appendLine("Email: ${data.email}")
        if (data.website.isNotBlank()) appendLine("Website: ${data.website}")
        if (data.address.isNotBlank()) appendLine("Address: ${data.address}")
    }

    /**
     * Generates a fully customizable QR Code bitmap with custom colors, margin, and error correction.
     */
    fun generateCustomQrBitmap(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = android.graphics.Color.BLACK,
        lightColor: Int = android.graphics.Color.WHITE,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
        margin: Int = 1
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, errorCorrection)
                put(EncodeHintType.MARGIN, margin)
            }
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a dynamic QR Code with optional center badge / initials overlay.
     */
    fun generateDynamicContactQr(
        data: ContactQrData,
        format: QrContactFormat = QrContactFormat.VCARD_3_0,
        sizePx: Int = 600,
        themeColor: QrThemeColor = QrThemeColor.CARDMATE_TEAL,
        centerBadge: QrCenterBadge = QrCenterBadge.NONE,
        centerLogoBitmap: Bitmap? = null
    ): Bitmap? {
        val payload = buildContactPayload(data, format)
        if (payload.isBlank()) return null

        // If a center badge is used, use High (H) error correction to preserve scan reliability
        val ecLevel = if (centerBadge != QrCenterBadge.NONE) ErrorCorrectionLevel.H else ErrorCorrectionLevel.M
        val baseQr = generateCustomQrBitmap(
            content = payload,
            sizePx = sizePx,
            darkColor = themeColor.darkInt,
            lightColor = android.graphics.Color.WHITE,
            errorCorrection = ecLevel,
            margin = 1
        ) ?: return null

        if (centerBadge == QrCenterBadge.NONE) {
            return baseQr
        }

        // Overlay center emblem/initials/logo
        return overlayCenterBadge(
            baseQr = baseQr,
            badgeType = centerBadge,
            data = data,
            themeColor = themeColor,
            customLogo = centerLogoBitmap
        )
    }

    private fun overlayCenterBadge(
        baseQr: Bitmap,
        badgeType: QrCenterBadge,
        data: ContactQrData,
        themeColor: QrThemeColor,
        customLogo: Bitmap?
    ): Bitmap {
        val width = baseQr.width
        val height = baseQr.height
        val mutableBitmap = baseQr.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // Center badge dimensions: ~20% of QR width for optimal scan safety
        val badgeSize = (width * 0.22f).toInt()
        val centerX = width / 2f
        val centerY = height / 2f
        val halfBadge = badgeSize / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 2f, 0x33000000)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = themeColor.darkInt
            style = Paint.Style.STROKE
            strokeWidth = badgeSize * 0.08f
        }

        // Draw white background circle with primary border
        canvas.drawCircle(centerX, centerY, halfBadge, bgPaint)
        canvas.drawCircle(centerX, centerY, halfBadge, borderPaint)

        val innerRadius = halfBadge - borderPaint.strokeWidth

        when (badgeType) {
            QrCenterBadge.USER_INITIALS -> {
                val initials = getInitials(data.fullName)
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = themeColor.darkInt
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(centerX, centerY, innerRadius, fillPaint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textSize = innerRadius * 0.9f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val textBounds = Rect()
                textPaint.getTextBounds(initials, 0, initials.length, textBounds)
                val textY = centerY + (textBounds.height() / 2f)
                canvas.drawText(initials, centerX, textY, textPaint)
            }
            QrCenterBadge.CARDMATE_LOGO -> {
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = themeColor.darkInt
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(centerX, centerY, innerRadius, fillPaint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textSize = innerRadius * 0.85f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val emblem = "CM"
                val textBounds = Rect()
                textPaint.getTextBounds(emblem, 0, emblem.length, textBounds)
                val textY = centerY + (textBounds.height() / 2f)
                canvas.drawText(emblem, centerX, textY, textPaint)
            }
            QrCenterBadge.CONTACT_ICON -> {
                if (customLogo != null) {
                    val destRect = RectF(
                        centerX - innerRadius,
                        centerY - innerRadius,
                        centerX + innerRadius,
                        centerY + innerRadius
                    )
                    canvas.drawBitmap(customLogo, null, destRect, null)
                } else {
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = themeColor.darkInt
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(centerX, centerY, innerRadius, fillPaint)

                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = innerRadius * 0.9f
                        isFakeBoldText = true
                        textAlign = Paint.Align.CENTER
                    }
                    val iconText = "📇"
                    val textBounds = Rect()
                    textPaint.getTextBounds(iconText, 0, iconText.length, textBounds)
                    val textY = centerY + (textBounds.height() / 2f)
                    canvas.drawText(iconText, centerX, textY, textPaint)
                }
            }
            QrCenterBadge.NONE -> Unit
        }

        return mutableBitmap
    }

    private fun getInitials(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return "CM"
        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.isNotEmpty() && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.isNotEmpty() -> parts[0].take(1).uppercase()
            else -> "CM"
        }
    }

    /**
     * Renders a high-resolution, presentation-grade card bitmap that frames the dynamic QR code
     * along with contact information and branding for clean sharing.
     */
    fun renderSharableQrCard(
        data: ContactQrData,
        qrBitmap: Bitmap,
        themeColor: QrThemeColor = QrThemeColor.CARDMATE_TEAL
    ): Bitmap {
        val cardWidth = 1000
        val cardHeight = 1400
        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt() // Dark premium slate background
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)

        // Accent top banner
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = themeColor.darkInt
            style = Paint.Style.FILL
        }
        val bannerHeight = 240f
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), bannerHeight, bannerPaint)

        // App watermark / header in banner
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CARDMATE AI • DIGITAL BUSINESS QR", cardWidth / 2f, 90f, headerPaint)

        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xDDFFFFFF.toInt()
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Scan to instantly import and save contact", cardWidth / 2f, 140f, subHeaderPaint)

        // White elevated QR container card
        val qrContainerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(24f, 0f, 8f, 0x66000000)
        }
        val qrBoxSize = 640f
        val qrBoxLeft = (cardWidth - qrBoxSize) / 2f
        val qrBoxTop = 200f
        val qrBoxRect = RectF(qrBoxLeft, qrBoxTop, qrBoxLeft + qrBoxSize, qrBoxTop + qrBoxSize)
        canvas.drawRoundRect(qrBoxRect, 32f, 32f, qrContainerPaint)

        // Draw QR bitmap inside white container
        val qrPadding = 32f
        val qrDestRect = RectF(
            qrBoxLeft + qrPadding,
            qrBoxTop + qrPadding,
            qrBoxLeft + qrBoxSize - qrPadding,
            qrBoxTop + qrBoxSize - qrPadding
        )
        canvas.drawBitmap(qrBitmap, null, qrDestRect, null)

        // Contact Information Section
        var textY = qrBoxTop + qrBoxSize + 80f

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 52f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val name = data.fullName.ifBlank { "Smart Contact Card" }
        canvas.drawText(name, cardWidth / 2f, textY, namePaint)
        textY += 60f

        if (data.displaySubtitle.isNotBlank()) {
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = themeColor.darkInt
                textSize = 32f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(data.displaySubtitle, cardWidth / 2f, textY, subtitlePaint)
            textY += 65f
        }

        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt() // slate-400
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }

        if (data.phone.isNotBlank()) {
            canvas.drawText("📞  ${data.phone}", cardWidth / 2f, textY, detailPaint)
            textY += 45f
        }
        if (data.email.isNotBlank()) {
            canvas.drawText("✉️  ${data.email}", cardWidth / 2f, textY, detailPaint)
            textY += 45f
        }
        if (data.website.isNotBlank()) {
            canvas.drawText("🌐  ${data.website}", cardWidth / 2f, textY, detailPaint)
            textY += 45f
        }

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Scannable with any iOS & Android Camera", cardWidth / 2f, cardHeight - 60f, footerPaint)

        return bitmap
    }

    /**
     * Shares the QR code image via Android Intent Chooser.
     */
    fun shareQrCodeImage(
        context: Context,
        qrCardBitmap: Bitmap,
        title: String = "Digital Business Card QR"
    ): Boolean {
        return try {
            val exportDir = File(context.cacheDir, "export_qrcodes").apply { mkdirs() }
            val fileName = "cardmate_qr_${System.currentTimeMillis()}.png"
            val imageFile = File(exportDir, fileName)

            FileOutputStream(imageFile).use { out ->
                qrCardBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Scan this QR code to save my digital business card.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, title))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves the high-resolution QR code image to the device gallery / Pictures folder.
     */
    fun saveQrCodeToGallery(
        context: Context,
        qrBitmap: Bitmap,
        contactName: String
    ): Boolean {
        return try {
            val safeName = contactName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "contact" }
            val filename = "QR_${safeName}_${System.currentTimeMillis()}.png"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CardMate")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false

            resolver.openOutputStream(uri)?.use { stream: OutputStream ->
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
