package com.example.sync

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.BusinessCard
import com.example.data.model.CardTemplate
import com.example.ui.components.QrCodeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object CardImageGenerator {

    enum class ShareImageStyle {
        FRONT_ONLY,
        BACK_QR_ONLY,
        DUAL_CARD
    }

    /**
     * Generates a high resolution Bitmap of the business card.
     */
    fun generateCardBitmap(
        context: Context,
        card: BusinessCard,
        style: ShareImageStyle = ShareImageStyle.FRONT_ONLY
    ): Bitmap {
        val template = CardTemplate.fromId(card.cardLayoutTemplate)
        return when (style) {
            ShareImageStyle.FRONT_ONLY -> drawFrontCard(card, template, width = 1200, height = 680)
            ShareImageStyle.BACK_QR_ONLY -> drawBackCard(card, template, width = 1200, height = 680)
            ShareImageStyle.DUAL_CARD -> drawDualCardMockup(card, template, width = 1200, height = 1380)
        }
    }

    private fun drawFrontCard(
        card: BusinessCard,
        template: CardTemplate,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val primaryColor = card.effectivePrimaryBgColor.toInt()
        val secondaryColor = card.effectiveSecondaryBgColor.toInt()
        val accentColor = card.effectiveAccentColor.toInt()
        val textColor = card.effectiveTextColor.toInt()

        // Background Gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                primaryColor, secondaryColor,
                Shader.TileMode.CLAMP
            )
        }
        val cardRect = RectF(20f, 20f, width - 20f, height - 20f)
        canvas.drawRoundRect(cardRect, 36f, 36f, bgPaint)

        // Subtle Card Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            color = accentColor
            alpha = 90
        }
        canvas.drawRoundRect(cardRect, 36f, 36f, borderPaint)

        // Top Category Badge
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            alpha = 45
        }
        val categoryText = card.category.uppercase()
        val categoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        val categoryWidth = categoryPaint.measureText(categoryText)
        val badgeRect = RectF(60f, 55f, 60f + categoryWidth + 36f, 105f)
        canvas.drawRoundRect(badgeRect, 14f, 14f, badgeBgPaint)
        canvas.drawText(categoryText, 78f, 90f, categoryPaint)

        // Top Right: NFC & Smart Badge
        val smartBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = 180
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("✦ SMART NFC CARD", width - 65f, 88f, smartBadgePaint)

        // Left Avatar / Monogram Circle
        val avatarSize = 130f
        val avatarX = 65f
        val avatarY = 145f
        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            alpha = 60
        }
        canvas.drawRoundRect(
            RectF(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize),
            30f, 30f, avatarPaint
        )

        val initial = card.fullName.trim().firstOrNull()?.uppercase() ?: "C"
        val avatarTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textBounds = Rect()
        avatarTextPaint.getTextBounds(initial, 0, initial.length, textBounds)
        canvas.drawText(
            initial,
            avatarX + avatarSize / 2f,
            avatarY + avatarSize / 2f + textBounds.height() / 2f,
            avatarTextPaint
        )

        // Name & Title next to Avatar
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val safeName = card.fullName.ifBlank { "Card Holder" }
        canvas.drawText(safeName, avatarX + avatarSize + 30f, avatarY + 58f, namePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitle = buildString {
            if (card.jobTitle.isNotBlank()) append(card.jobTitle)
            if (card.jobTitle.isNotBlank() && card.company.isNotBlank()) append(" • ")
            if (card.company.isNotBlank()) append(card.company)
        }
        if (subtitle.isNotBlank()) {
            canvas.drawText(subtitle, avatarX + avatarSize + 30f, avatarY + 105f, titlePaint)
        }

        // Horizontal Divider
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = 30
            strokeWidth = 1.5f
        }
        canvas.drawLine(65f, 310f, width - 65f, 310f, dividerPaint)

        // Contact Details Section
        val infoIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val infoLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = 230
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        var startY = 365f
        val lineSpacing = 48f

        if (card.phone.isNotBlank()) {
            canvas.drawText("📞", 65f, startY, infoIconPaint)
            canvas.drawText(card.phone, 115f, startY, infoLabelPaint)
            startY += lineSpacing
        }

        if (card.email.isNotBlank()) {
            canvas.drawText("✉️", 65f, startY, infoIconPaint)
            canvas.drawText(card.email, 115f, startY, infoLabelPaint)
            startY += lineSpacing
        }

        if (card.website.isNotBlank()) {
            canvas.drawText("🌐", 65f, startY, infoIconPaint)
            canvas.drawText(card.website, 115f, startY, infoLabelPaint)
            startY += lineSpacing
        }

        if (card.address.isNotBlank() && startY <= 530f) {
            canvas.drawText("📍", 65f, startY, infoIconPaint)
            val shortAddr = card.address.replace("\n", ", ")
            val truncatedAddr = if (shortAddr.length > 55) shortAddr.take(52) + "..." else shortAddr
            canvas.drawText(truncatedAddr, 115f, startY, infoLabelPaint)
        }

        // Bottom Footer Watermark
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = 110
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("CARDMATE AI • SMART DIGITAL CARD", 65f, height - 55f, footerPaint)

        val formatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            alpha = 140
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(card.cardStandardName, width - 65f, height - 55f, formatPaint)

        return bitmap
    }

    private fun drawBackCard(
        card: BusinessCard,
        template: CardTemplate,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val primaryColor = card.effectiveSecondaryBgColor.toInt()
        val secondaryColor = card.effectivePrimaryBgColor.toInt()
        val accentColor = card.effectiveAccentColor.toInt()
        val textColor = card.effectiveTextColor.toInt()

        // Background Gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                primaryColor, secondaryColor,
                Shader.TileMode.CLAMP
            )
        }
        val cardRect = RectF(20f, 20f, width - 20f, height - 20f)
        canvas.drawRoundRect(cardRect, 36f, 36f, bgPaint)

        // Subtle Card Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            color = accentColor
            alpha = 90
        }
        canvas.drawRoundRect(cardRect, 36f, 36f, borderPaint)

        // Generate QR Code
        val vCardString = VCardExporter.toVCard3String(card)
        val qrBitmap = QrCodeHelper.generateQrBitmap(
            content = vCardString,
            sizePx = 380,
            darkColor = android.graphics.Color.BLACK,
            lightColor = android.graphics.Color.WHITE
        )

        // Draw White QR Container on Right
        val qrSize = 390f
        val qrRight = width - 75f
        val qrLeft = qrRight - qrSize
        val qrTop = (height - qrSize) / 2f
        val qrBottom = qrTop + qrSize

        val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        }
        val qrBgRect = RectF(qrLeft, qrTop, qrRight, qrBottom)
        canvas.drawRoundRect(qrBgRect, 24f, 24f, qrBgPaint)

        if (qrBitmap != null) {
            val qrInnerRect = RectF(qrLeft + 15f, qrTop + 15f, qrRight - 15f, qrBottom - 15f)
            canvas.drawBitmap(qrBitmap, null, qrInnerRect, null)
        }

        // Left Side Content
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("SCAN TO SAVE CONTACT", 65f, 130f, titlePaint)

        val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = 180
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Point camera or QR scanner to add", 65f, 180f, descPaint)
        canvas.drawText("${card.fullName} directly to your phone contacts.", 65f, 215f, descPaint)

        val accentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            strokeWidth = 3f
        }
        canvas.drawLine(65f, 255f, 220f, 255f, accentLinePaint)

        // Contact summary
        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = 220
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        var curY = 310f
        if (card.company.isNotBlank()) {
            canvas.drawText("🏢 ${card.company}", 65f, curY, infoPaint)
            curY += 45f
        }
        if (card.phone.isNotBlank()) {
            canvas.drawText("📞 ${card.phone}", 65f, curY, infoPaint)
            curY += 45f
        }
        if (card.email.isNotBlank()) {
            canvas.drawText("✉️ ${card.email}", 65f, curY, infoPaint)
            curY += 45f
        }
        if (card.website.isNotBlank()) {
            canvas.drawText("🌐 ${card.website}", 65f, curY, infoPaint)
            curY += 45f
        }

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("POWERED BY CARDMATE AI", 65f, height - 55f, footerPaint)

        return bitmap
    }

    private fun drawDualCardMockup(
        card: BusinessCard,
        template: CardTemplate,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Canvas Dark Backdrop
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#090D16")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header Title
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DIGITAL BUSINESS CARD", width / 2f, 65f, headerPaint)

        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = card.effectiveAccentColor.toInt()
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(card.fullName, width / 2f, 105f, subHeaderPaint)

        // Draw Front Card on Top
        val singleHeight = 580
        val singleWidth = 1080
        val frontBitmap = drawFrontCard(card, template, singleWidth, singleHeight)
        val frontX = (width - singleWidth) / 2f
        val frontY = 140f
        canvas.drawBitmap(frontBitmap, frontX, frontY, null)

        // Label Front
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("• FRONT DESIGN •", width / 2f, frontY + singleHeight + 35f, labelPaint)

        // Draw Back Card on Bottom
        val backBitmap = drawBackCard(card, template, singleWidth, singleHeight)
        val backY = frontY + singleHeight + 60f
        canvas.drawBitmap(backBitmap, frontX, backY, null)

        // Label Back
        canvas.drawText("• BACK (V-CARD QR CODE) •", width / 2f, backY + singleHeight + 35f, labelPaint)

        return bitmap
    }

    /**
     * Shares the generated business card image via Android Share Sheet.
     */
    suspend fun shareCardAsImage(
        context: Context,
        card: BusinessCard,
        style: ShareImageStyle = ShareImageStyle.FRONT_ONLY,
        onComplete: (() -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val bitmap = generateCardBitmap(context, card, style)
                val imagesFolder = File(context.cacheDir, "card_images").apply { mkdirs() }
                
                val safeName = card.fullName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Card" }
                val suffix = when (style) {
                    ShareImageStyle.FRONT_ONLY -> "Front"
                    ShareImageStyle.BACK_QR_ONLY -> "QR_Back"
                    ShareImageStyle.DUAL_CARD -> "Full_Portfolio"
                }
                val file = File(imagesFolder, "${safeName}_${suffix}.png")
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Business Card - ${card.fullName}")
                    putExtra(Intent.EXTRA_TEXT, "Digital Business Card: ${card.fullName} (${card.displaySubtitle})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Card Image"))
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to share image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Saves the card bitmap directly to the device's Pictures / Gallery folder.
     */
    suspend fun saveCardImageToGallery(
        context: Context,
        card: BusinessCard,
        style: ShareImageStyle = ShareImageStyle.FRONT_ONLY
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = generateCardBitmap(context, card, style)
                val safeName = card.fullName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Card" }
                val filename = "CardMate_${safeName}_${System.currentTimeMillis()}.png"

                var outputStream: OutputStream? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CardMate")
                    }
                    val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (imageUri != null) {
                        outputStream = context.contentResolver.openOutputStream(imageUri)
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/CardMate"
                    val fileDir = File(imagesDir).apply { mkdirs() }
                    val imageFile = File(fileDir, filename)
                    outputStream = FileOutputStream(imageFile)
                }

                outputStream?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "কার্ড ইমেজ গ্যালারিতে সংরক্ষিত হয়েছে! (Saved to Gallery)", Toast.LENGTH_LONG).show()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
                false
            }
        }
    }
}
