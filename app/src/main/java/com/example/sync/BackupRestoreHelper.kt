package com.example.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BusinessCard
import com.example.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupRestoreHelper {

    /**
     * Creates a full JSON backup string including all business cards and user profile.
     */
    fun createFullBackupJson(cards: List<BusinessCard>, profile: UserProfile): String {
        val root = JSONObject()
        root.put("app", "CardMate AI")
        root.put("version", "1.2.0")
        root.put("schemaVersion", 2)
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("totalCards", cards.size)

        // User Profile JSON
        val profileJson = JSONObject().apply {
            put("fullName", profile.fullName)
            put("jobTitle", profile.jobTitle)
            put("company", profile.company)
            put("phone", profile.phone)
            put("secondaryPhone", profile.secondaryPhone)
            put("email", profile.email)
            put("website", profile.website)
            put("address", profile.address)
            put("category", profile.category)
            put("bio", profile.bio)
            put("socialLinks", profile.socialLinks)
            put("cardLayoutTemplate", profile.cardLayoutTemplate)
        }
        root.put("userProfile", profileJson)

        // Business Cards Array
        val cardsArray = JSONArray()
        for (card in cards) {
            val cardObj = JSONObject().apply {
                put("id", card.id)
                put("fullName", card.fullName)
                put("jobTitle", card.jobTitle)
                put("company", card.company)
                put("phone", card.phone)
                put("secondaryPhone", card.secondaryPhone)
                put("email", card.email)
                put("website", card.website)
                put("address", card.address)
                put("category", card.category)
                put("notes", card.notes)
                put("socialLinks", card.socialLinks)
                put("cardLayoutTemplate", card.cardLayoutTemplate)
                put("cardFrontImageUri", card.cardFrontImageUri ?: "")
                put("cardBackImageUri", card.cardBackImageUri ?: "")
                put("cardWidthMm", card.cardWidthMm.toDouble())
                put("cardHeightMm", card.cardHeightMm.toDouble())
                put("cardStandardName", card.cardStandardName)
                put("isFavorite", card.isFavorite)
                put("isSyncedWithGoogleContacts", card.isSyncedWithGoogleContacts)
                put("isBackedUpToCloud", card.isBackedUpToCloud)
                put("createdAt", card.createdAt)
                put("updatedAt", card.updatedAt)
                put("rawOcrText", card.rawOcrText)
                put("language", card.language)
            }
            cardsArray.put(cardObj)
        }
        root.put("cards", cardsArray)

        return root.toString(2)
    }

    /**
     * Shares or saves the Full JSON Backup as a .json file.
     */
    fun shareFullJsonBackupFile(context: Context, cards: List<BusinessCard>, profile: UserProfile) {
        try {
            val jsonString = createFullBackupJson(cards, profile)
            val exportDir = File(context.cacheDir, "export_backups").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(exportDir, "CardMate_Full_Backup_$timestamp.json")

            FileOutputStream(backupFile).use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "CardMate Full Backup ($timestamp)")
                putExtra(Intent.EXTRA_TEXT, "CardMate AI Full Backup (${cards.size} cards + User Profile)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share / Save JSON Backup"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text sharing
            VCardExporter.shareText(
                context,
                createFullBackupJson(cards, profile),
                "CardMate JSON Backup"
            )
        }
    }

    /**
     * Parses a JSON string (either full backup object or raw cards array) into BusinessCards and optional UserProfile.
     */
    fun parseBackupJson(jsonString: String): Pair<List<BusinessCard>, UserProfile?> {
        val cardsList = mutableListOf<BusinessCard>()
        var profile: UserProfile? = null

        val trimmed = jsonString.trim()
        if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)

            // Parse User Profile if present
            if (root.has("userProfile")) {
                val pObj = root.getJSONObject("userProfile")
                profile = UserProfile(
                    fullName = pObj.optString("fullName", ""),
                    jobTitle = pObj.optString("jobTitle", ""),
                    company = pObj.optString("company", ""),
                    phone = pObj.optString("phone", ""),
                    secondaryPhone = pObj.optString("secondaryPhone", ""),
                    email = pObj.optString("email", ""),
                    website = pObj.optString("website", ""),
                    address = pObj.optString("address", ""),
                    category = pObj.optString("category", "Tech & IT"),
                    bio = pObj.optString("bio", ""),
                    socialLinks = pObj.optString("socialLinks", ""),
                    cardLayoutTemplate = pObj.optString("cardLayoutTemplate", "modern_slate")
                )
            }

            // Parse Cards Array
            val cardsArray = when {
                root.has("cards") -> root.getJSONArray("cards")
                root.has("businessCards") -> root.getJSONArray("businessCards")
                root.has("contacts") -> root.getJSONArray("contacts")
                else -> JSONArray()
            }

            for (i in 0 until cardsArray.length()) {
                val cardObj = cardsArray.getJSONObject(i)
                cardsList.add(parseCardFromJsonObject(cardObj))
            }
        } else if (trimmed.startsWith("[")) {
            val array = JSONArray(trimmed)
            for (i in 0 until array.length()) {
                val cardObj = array.getJSONObject(i)
                cardsList.add(parseCardFromJsonObject(cardObj))
            }
        }

        return Pair(cardsList, profile)
    }

    private fun parseCardFromJsonObject(obj: JSONObject): BusinessCard {
        return BusinessCard(
            id = 0L, // Always assign 0 so Room autogenerates unique new ID on import
            fullName = obj.optString("fullName", obj.optString("name", "Imported Contact")),
            jobTitle = obj.optString("jobTitle", obj.optString("title", "")),
            company = obj.optString("company", obj.optString("org", "")),
            phone = obj.optString("phone", obj.optString("tel", "")),
            secondaryPhone = obj.optString("secondaryPhone", ""),
            email = obj.optString("email", ""),
            website = obj.optString("website", obj.optString("url", "")),
            address = obj.optString("address", obj.optString("adr", "")),
            category = obj.optString("category", "Corporate"),
            notes = obj.optString("notes", obj.optString("note", "")),
            socialLinks = obj.optString("socialLinks", ""),
            cardLayoutTemplate = obj.optString("cardLayoutTemplate", "modern_slate"),
            cardWidthMm = obj.optDouble("cardWidthMm", 88.9).toFloat(),
            cardHeightMm = obj.optDouble("cardHeightMm", 50.8).toFloat(),
            cardStandardName = obj.optString("cardStandardName", "Standard US (3.5\" × 2.0\")"),
            isFavorite = obj.optBoolean("isFavorite", false),
            isSyncedWithGoogleContacts = false,
            isBackedUpToCloud = false,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = System.currentTimeMillis(),
            rawOcrText = obj.optString("rawOcrText", ""),
            language = obj.optString("language", "auto")
        )
    }

    /**
     * Parses a vCard string (single or multi-contact) into a list of BusinessCard objects.
     */
    fun parseVCardString(vcardText: String): List<BusinessCard> {
        val result = mutableListOf<BusinessCard>()
        val lines = vcardText.lines()
        var insideVCard = false

        var fn = ""
        var title = ""
        var org = ""
        var phone = ""
        var secondaryPhone = ""
        var email = ""
        var url = ""
        var adr = ""
        var note = ""
        var category = "Corporate"

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.equals("BEGIN:VCARD", ignoreCase = true)) {
                insideVCard = true
                fn = ""; title = ""; org = ""; phone = ""; secondaryPhone = ""
                email = ""; url = ""; adr = ""; note = ""; category = "Corporate"
                continue
            }

            if (line.equals("END:VCARD", ignoreCase = true)) {
                if (fn.isNotBlank() || org.isNotBlank() || phone.isNotBlank() || email.isNotBlank()) {
                    result.add(
                        BusinessCard(
                            id = 0L,
                            fullName = fn.ifBlank { if (org.isNotBlank()) org else "Contact ${result.size + 1}" },
                            jobTitle = title,
                            company = org,
                            phone = phone,
                            secondaryPhone = secondaryPhone,
                            email = email,
                            website = url,
                            address = adr,
                            notes = note,
                            category = category.ifBlank { "Corporate" },
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                insideVCard = false
                continue
            }

            if (!insideVCard) continue

            val colonIndex = line.indexOf(':')
            if (colonIndex <= 0) continue

            val keyPart = line.substring(0, colonIndex).uppercase()
            val value = line.substring(colonIndex + 1).trim()

            when {
                keyPart == "FN" -> fn = value
                keyPart.startsWith("FN;") -> fn = value
                keyPart == "N" && fn.isBlank() -> {
                    val parts = value.split(";")
                    val lastName = parts.getOrNull(0) ?: ""
                    val firstName = parts.getOrNull(1) ?: ""
                    fn = "$firstName $lastName".trim()
                }
                keyPart == "TITLE" || keyPart.startsWith("TITLE;") -> title = value
                keyPart == "ORG" || keyPart.startsWith("ORG;") -> org = value.replace(";", " - ").trim()
                keyPart.startsWith("TEL") -> {
                    if (phone.isBlank()) {
                        phone = value
                    } else if (secondaryPhone.isBlank() && value != phone) {
                        secondaryPhone = value
                    }
                }
                keyPart.startsWith("EMAIL") -> {
                    if (email.isBlank()) email = value
                }
                keyPart.startsWith("URL") -> {
                    if (url.isBlank()) url = value
                }
                keyPart.startsWith("ADR") -> {
                    val cleanAdr = value.replace(";", " ").replace(Regex("\\s+"), " ").trim()
                    if (adr.isBlank()) adr = cleanAdr
                }
                keyPart.startsWith("NOTE") -> {
                    note = value
                }
                keyPart.startsWith("CATEGORIES") -> {
                    category = value.split(",").firstOrNull()?.trim() ?: "Corporate"
                }
            }
        }

        return result
    }

    /**
     * Reads text content from a content Uri (e.g. from File Picker).
     */
    fun readTextFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } ?: ""
    }
}
