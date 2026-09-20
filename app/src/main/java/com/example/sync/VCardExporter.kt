package com.example.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BusinessCard
import java.io.File
import java.io.FileOutputStream

object VCardExporter {

    /**
     * Converts a single BusinessCard into a standard vCard 3.0 string.
     */
    fun toVCard3String(card: BusinessCard): String {
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:${card.fullName}")
            if (card.jobTitle.isNotBlank()) appendLine("TITLE:${card.jobTitle}")
            if (card.company.isNotBlank()) appendLine("ORG:${card.company}")
            if (card.phone.isNotBlank()) appendLine("TEL;TYPE=WORK,VOICE:${card.phone}")
            if (card.secondaryPhone.isNotBlank()) appendLine("TEL;TYPE=CELL:${card.secondaryPhone}")
            if (card.email.isNotBlank()) appendLine("EMAIL;TYPE=WORK,INTERNET:${card.email}")
            if (card.website.isNotBlank()) appendLine("URL:${card.website}")
            if (card.address.isNotBlank()) appendLine("ADR;TYPE=WORK:;;${card.address.replace("\n", " ")};;;;")
            if (card.notes.isNotBlank()) appendLine("NOTE:${card.notes}")
            appendLine("CATEGORIES:${card.category}")
            appendLine("PRODID:-//CardMate AI//Business Card Scanner//EN")
            appendLine("END:VCARD")
        }
    }

    /**
     * Converts a list of BusinessCards into a multi-contact vCard file.
     */
    fun toMultiVCardString(cards: List<BusinessCard>): String {
        return cards.joinToString("\n") { toVCard3String(it) }
    }

    /**
     * Converts a list of BusinessCards into CSV format.
     */
    fun toCsvString(cards: List<BusinessCard>): String {
        val header = "Full Name,Job Title,Company,Phone,Secondary Phone,Email,Website,Address,Category,Notes,Created Date"
        val rows = cards.joinToString("\n") { card ->
            listOf(
                escapeCsv(card.fullName),
                escapeCsv(card.jobTitle),
                escapeCsv(card.company),
                escapeCsv(card.phone),
                escapeCsv(card.secondaryPhone),
                escapeCsv(card.email),
                escapeCsv(card.website),
                escapeCsv(card.address),
                escapeCsv(card.category),
                escapeCsv(card.notes),
                escapeCsv(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(card.createdAt)))
            ).joinToString(",")
        }
        return "$header\n$rows"
    }

    private fun escapeCsv(value: String): String {
        var clean = value.replace("\"", "\"\"")
        if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
            clean = "\"$clean\""
        }
        return clean
    }

    /**
     * Shares a vCard or CSV text or file via Android Share Sheet.
     */
    fun shareText(context: Context, text: String, title: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, title)
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    /**
     * Shares business card as an actual .vcf file (best for WhatsApp, Mail, and Phone Contacts).
     */
    fun shareCardAsVCardFile(context: Context, card: BusinessCard) {
        try {
            val vcard = toVCard3String(card)
            val exportDir = File(context.cacheDir, "export_vcards").apply { mkdirs() }
            val safeName = card.fullName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Contact" }
            val vcfFile = File(exportDir, "${safeName}.vcf")

            FileOutputStream(vcfFile).use { out ->
                out.write(vcard.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                vcfFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Business Card - ${card.fullName}")
                putExtra(Intent.EXTRA_TEXT, "Digital Business Card (.vcf): ${card.fullName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share vCard File"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text sharing if file sharing fails
            shareCardAsVCard(context, card)
        }
    }

    /**
     * Shares multiple business cards as a single combined .vcf file.
     */
    fun shareMultiCardsAsVCardFile(context: Context, cards: List<BusinessCard>) {
        try {
            val multiVcard = toMultiVCardString(cards)
            val exportDir = File(context.cacheDir, "export_vcards").apply { mkdirs() }
            val vcfFile = File(exportDir, "CardMate_All_Contacts.vcf")

            FileOutputStream(vcfFile).use { out ->
                out.write(multiVcard.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                vcfFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "CardMate Contacts (${cards.size} cards)")
                putExtra(Intent.EXTRA_TEXT, "Exported ${cards.size} contacts from CardMate AI")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share All vCards"))
        } catch (e: Exception) {
            e.printStackTrace()
            shareText(context, toMultiVCardString(cards), "CardMate Contacts")
        }
    }

    /**
     * Shares business cards as an actual .csv spreadsheet file.
     */
    fun shareCsvFile(context: Context, cards: List<BusinessCard>) {
        try {
            val csvText = toCsvString(cards)
            val exportDir = File(context.cacheDir, "export_csv").apply { mkdirs() }
            val csvFile = File(exportDir, "CardMate_Contacts_Export.csv")

            FileOutputStream(csvFile).use { out ->
                out.write(csvText.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "CardMate Contacts CSV Export (${cards.size} contacts)")
                putExtra(Intent.EXTRA_TEXT, "Exported ${cards.size} contacts from CardMate AI as CSV spreadsheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export All Contacts (CSV)"))
        } catch (e: Exception) {
            e.printStackTrace()
            shareText(context, toCsvString(cards), "CardMate Contacts CSV Export")
        }
    }

    /**
     * Shares business card as vCard text.
     */
    fun shareCardAsVCard(context: Context, card: BusinessCard) {
        val vcard = toVCard3String(card)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/x-vcard"
            putExtra(Intent.EXTRA_TEXT, vcard)
            putExtra(Intent.EXTRA_SUBJECT, "Business Card - ${card.fullName}")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Digital Business Card"))
    }
}
