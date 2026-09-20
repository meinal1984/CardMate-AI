package com.example.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.example.ai.OcrParserHelper
import com.example.data.model.BusinessCard
import com.example.sync.VCardExporter
import java.io.IOException
import java.nio.charset.Charset

object NfcCardService {

    private const val TAG = "NfcCardService"

    const val MIME_TYPE_VCARD = "text/vcard"
    const val MIME_TYPE_X_VCARD = "text/x-vcard"
    const val MIME_TYPE_CARDMATE_JSON = "application/vnd.com.example.cardmate.card"

    /**
     * Check if device has NFC hardware
     */
    fun isNfcSupported(context: Context): Boolean {
        return try {
            val adapter = NfcAdapter.getDefaultAdapter(context)
            adapter != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking NFC adapter support", e)
            false
        }
    }

    /**
     * Check if NFC is enabled in system settings
     */
    fun isNfcEnabled(context: Context): Boolean {
        return try {
            val adapter = NfcAdapter.getDefaultAdapter(context)
            adapter != null && adapter.isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking NFC enabled state", e)
            false
        }
    }

    /**
     * Intent to open Android NFC System Settings
     */
    fun openNfcSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.ACTION_NFC_SETTINGS)
            } else {
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to launch settings", ex)
            }
        }
    }

    /**
     * Creates an NDEF Message for the BusinessCard.
     * Contains:
     * 1. Standard vCard 3.0 MIME Record (for all NFC readers & Android/iOS contacts)
     * 2. Web URL Record (if card has website, enables one-tap browser opening on standard readers)
     * 3. Optional Android Application Record (AAR) to launch CardMate on scan
     */
    fun createCardNdefMessage(
        card: BusinessCard,
        packageName: String = "com.example",
        includeAar: Boolean = true
    ): NdefMessage {
        val records = mutableListOf<NdefRecord>()

        // 1. Primary vCard Record
        val vCardString = VCardExporter.toVCard3String(card)
        val vCardBytes = vCardString.toByteArray(Charset.forName("UTF-8"))
        val vCardRecord = NdefRecord.createMime(MIME_TYPE_VCARD, vCardBytes)
        records.add(vCardRecord)

        // 2. Custom JSON Payload Record for lossless CardMate attributes (template, detected dimensions, category)
        val jsonPayload = buildCardJson(card)
        val jsonRecord = NdefRecord.createMime(
            MIME_TYPE_CARDMATE_JSON,
            jsonPayload.toByteArray(Charset.forName("UTF-8"))
        )
        records.add(jsonRecord)

        // 3. Optional Web URI Record if card has website
        if (card.website.isNotBlank()) {
            try {
                val cleanUrl = if (!card.website.startsWith("http://") && !card.website.startsWith("https://")) {
                    "https://${card.website}"
                } else {
                    card.website
                }
                val uriRecord = NdefRecord.createUri(cleanUrl)
                records.add(uriRecord)
            } catch (e: Exception) {
                Log.w(TAG, "Could not create URI NDEF record", e)
            }
        }

        // 4. Android Application Record (AAR)
        if (includeAar && packageName.isNotBlank()) {
            records.add(NdefRecord.createApplicationRecord(packageName))
        }

        return NdefMessage(records.toTypedArray())
    }

    /**
     * Writes a BusinessCard to an NFC Tag.
     */
    fun writeCardToTag(
        tag: Tag,
        card: BusinessCard,
        packageName: String = "com.example",
        includeAar: Boolean = true
    ): NfcWriteResult {
        val message = createCardNdefMessage(card, packageName, includeAar)
        val messageBytes = message.toByteArray()
        val messageSize = messageBytes.size
        val tagType = detectTagType(tag)

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return NfcWriteResult.TagReadOnly
                }
                val capacity = ndef.maxSize
                if (capacity < messageSize) {
                    ndef.close()
                    return NfcWriteResult.InsufficientSpace(
                        requiredBytes = messageSize,
                        availableBytes = capacity,
                        tagType = tagType
                    )
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                return NfcWriteResult.Success(
                    bytesWritten = messageSize,
                    tagType = tagType,
                    cardName = card.fullName,
                    isAarIncluded = includeAar
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error writing NDEF to tag", e)
                try { ndef.close() } catch (_: Exception) {}
                return NfcWriteResult.Error("Failed to write to NFC tag: ${e.localizedMessage ?: e.javaClass.simpleName}", e)
            }
        }

        // Check if unformatted tag can be formatted
        val ndefFormatable = NdefFormatable.get(tag)
        if (ndefFormatable != null) {
            try {
                ndefFormatable.connect()
                ndefFormatable.format(message)
                ndefFormatable.close()
                return NfcWriteResult.Success(
                    bytesWritten = messageSize,
                    tagType = "$tagType (Formatted)",
                    cardName = card.fullName,
                    isAarIncluded = includeAar
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting and writing NDEF to tag", e)
                try { ndefFormatable.close() } catch (_: Exception) {}
                return NfcWriteResult.Error("Failed to format NFC tag: ${e.localizedMessage ?: e.javaClass.simpleName}", e)
            }
        }

        return NfcWriteResult.Error("This tag does not support NDEF format ($tagType).")
    }

    /**
     * Erases / Formats an NFC Tag with an empty NDEF message.
     */
    fun clearNfcTag(tag: Tag): NfcWriteResult {
        val emptyMessage = NdefMessage(arrayOf(NdefRecord.createMime("text/plain", ByteArray(0))))
        val tagType = detectTagType(tag)
        val ndef = Ndef.get(tag)

        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return NfcWriteResult.TagReadOnly
                }
                ndef.writeNdefMessage(emptyMessage)
                ndef.close()
                return NfcWriteResult.Success(
                    bytesWritten = 0,
                    tagType = tagType,
                    cardName = "Tag Cleared",
                    isAarIncluded = false
                )
            } catch (e: Exception) {
                try { ndef.close() } catch (_: Exception) {}
                return NfcWriteResult.Error("Failed to clear tag: ${e.message}", e)
            }
        }
        return NfcWriteResult.Error("Cannot format or wipe this tag type.")
    }

    /**
     * Reads a BusinessCard and Tag info from an active NFC Tag.
     */
    fun readCardFromTag(tag: Tag): NfcReadResult {
        val tagInfo = extractTagInfo(tag)
        val ndef = Ndef.get(tag)

        if (ndef != null) {
            try {
                ndef.connect()
                val ndefMessage = ndef.ndefMessage ?: ndef.cachedNdefMessage
                ndef.close()

                if (ndefMessage == null || ndefMessage.records.isEmpty()) {
                    return NfcReadResult.EmptyTag
                }

                val card = parseNdefMessageToCard(ndefMessage)
                val rawPayload = extractRawPayload(ndefMessage)

                return if (card != null) {
                    NfcReadResult.Success(card, rawPayload, tagInfo)
                } else {
                    NfcReadResult.Error("Could not extract contact card from NDEF records.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading NDEF message from tag", e)
                try { ndef.close() } catch (_: Exception) {}
                return NfcReadResult.Error("Failed to read NFC tag: ${e.localizedMessage ?: e.javaClass.simpleName}", e)
            }
        }

        return NfcReadResult.Error("Tag is not NDEF compliant ($tagInfo.tagType).")
    }

    /**
     * Reads a BusinessCard from an Android Intent (NDEF_DISCOVERED, TECH_DISCOVERED, TAG_DISCOVERED)
     */
    fun readCardFromIntent(intent: Intent): BusinessCard? {
        val action = intent.action ?: return null
        if (NfcAdapter.ACTION_NDEF_DISCOVERED != action &&
            NfcAdapter.ACTION_TECH_DISCOVERED != action &&
            NfcAdapter.ACTION_TAG_DISCOVERED != action
        ) {
            return null
        }

        // Check NDEF Messages in extra
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null && rawMessages.isNotEmpty()) {
            val message = rawMessages[0] as? NdefMessage
            if (message != null) {
                return parseNdefMessageToCard(message)
            }
        }

        // Fallback: Read from Tag object in extra
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null) {
            val result = readCardFromTag(tag)
            if (result is NfcReadResult.Success) {
                return result.card
            }
        }

        return null
    }

    /**
     * Parses an NDEF Message into a BusinessCard
     */
    fun parseNdefMessageToCard(message: NdefMessage): BusinessCard? {
        val records = message.records
        if (records.isEmpty()) return null

        // 1. Check for custom CardMate JSON record first (preserves full metadata)
        for (record in records) {
            val mimeType = record.toMimeType()
            if (mimeType == MIME_TYPE_CARDMATE_JSON) {
                try {
                    val json = String(record.payload, Charset.forName("UTF-8"))
                    val card = parseCardFromJson(json)
                    if (card != null) return card
                } catch (e: Exception) {
                    Log.w(TAG, "Failed parsing JSON record", e)
                }
            }
        }

        // 2. Check for standard vCard MIME records
        for (record in records) {
            val mimeType = record.toMimeType()
            if (mimeType == MIME_TYPE_VCARD || mimeType == MIME_TYPE_X_VCARD) {
                val payload = String(record.payload, Charset.forName("UTF-8"))
                return OcrParserHelper.parseVCardOrText(payload)
            }
        }

        // 3. Fallback: Check text or well-known URI records
        for (record in records) {
            try {
                val payload = String(record.payload, Charset.forName("UTF-8"))
                if (payload.contains("BEGIN:VCARD") || payload.contains("FN:") || payload.contains("TEL:")) {
                    return OcrParserHelper.parseVCardOrText(payload)
                }
                if (payload.length > 5 && !payload.startsWith("android.com:pkg")) {
                    return OcrParserHelper.parseVCardOrText(payload)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed extracting text record", e)
            }
        }

        return null
    }

    /**
     * Extracts full tag metadata (ID, technologies, memory, lock state)
     */
    fun extractTagInfo(tag: Tag): NfcTagInfo {
        val idBytes = tag.id
        val tagIdHex = idBytes.joinToString(":") { "%02X".format(it) }
        val techs = tag.techList.map { it.substringAfterLast(".") }
        val tagType = detectTagType(tag)

        var maxSize = 0
        var currentSize = 0
        var isWritable = true
        var isNdefSupported = false

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            isNdefSupported = true
            try {
                maxSize = ndef.maxSize
                isWritable = ndef.isWritable
                val msg = ndef.cachedNdefMessage ?: ndef.ndefMessage
                currentSize = msg?.byteArrayLength ?: 0
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch detailed NDEF tag info", e)
            }
        }

        return NfcTagInfo(
            tagIdHex = tagIdHex,
            technologies = techs,
            maxSizeBytes = maxSize,
            currentSizeBytes = currentSize,
            isWritable = isWritable,
            isNdefSupported = isNdefSupported,
            tagType = tagType
        )
    }

    /**
     * Detects human-readable tag type (e.g., NTAG213, NTAG215, NTAG216, Mifare Classic, Ultralight)
     */
    fun detectTagType(tag: Tag): String {
        val techs = tag.techList.map { it.substringAfterLast(".") }

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            val type = ndef.type
            val maxSize = try { ndef.maxSize } catch (_: Exception) { 0 }
            return when {
                maxSize in 130..160 -> "NXP NTAG213 (144B)"
                maxSize in 480..520 -> "NXP NTAG215 (504B)"
                maxSize in 870..920 -> "NXP NTAG216 (888B)"
                type == Ndef.NFC_FORUM_TYPE_1 -> "NFC Forum Type 1 (Topaz)"
                type == Ndef.NFC_FORUM_TYPE_2 -> "NFC Forum Type 2 Tag"
                type == Ndef.NFC_FORUM_TYPE_3 -> "NFC Forum Type 3 (FeliCa)"
                type == Ndef.NFC_FORUM_TYPE_4 -> "NFC Forum Type 4 (Smart Card)"
                type == Ndef.MIFARE_CLASSIC -> "Mifare Classic"
                else -> "NDEF Tag ($type)"
            }
        }

        val mifareClassic = MifareClassic.get(tag)
        if (mifareClassic != null) {
            return when (mifareClassic.type) {
                MifareClassic.TYPE_CLASSIC -> "Mifare Classic 1K/4K"
                MifareClassic.TYPE_PLUS -> "Mifare Plus"
                MifareClassic.TYPE_PRO -> "Mifare Pro"
                else -> "Mifare Classic"
            }
        }

        val mifareUltralight = MifareUltralight.get(tag)
        if (mifareUltralight != null) {
            return when (mifareUltralight.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "Mifare Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "Mifare Ultralight C"
                else -> "Mifare Ultralight"
            }
        }

        if (techs.contains("IsoDep")) return "ISO 14443-4 Smart Card"
        if (techs.contains("NfcA")) return "ISO 14443-3A (NFC-A)"
        if (techs.contains("NfcB")) return "ISO 14443-3B (NFC-B)"
        if (techs.contains("NfcF")) return "JIS 6319-4 (FeliCa)"
        if (techs.contains("NfcV")) return "ISO 15693 (NFC-V)"

        return "Generic NFC Tag"
    }

    /**
     * Enables modern Reader Mode for real-time tag interception (Android 4.4+)
     */
    fun enableReaderMode(
        activity: Activity,
        flags: Int = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
        onTagDiscovered: (Tag) -> Unit
    ) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }
        try {
            adapter.enableReaderMode(activity, { tag ->
                activity.runOnUiThread {
                    onTagDiscovered(tag)
                }
            }, flags, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed enabling NFC reader mode", e)
        }
    }

    /**
     * Disables Reader Mode
     */
    fun disableReaderMode(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableReaderMode(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed disabling NFC reader mode", e)
        }
    }

    /**
     * Fallback: Enables Legacy Foreground Dispatch
     */
    fun enableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)

        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                addDataType("*/*")
            },
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )

        val techList = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name),
            arrayOf(NfcA::class.java.name),
            arrayOf(IsoDep::class.java.name)
        )

        try {
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed enabling foreground dispatch", e)
        }
    }

    /**
     * Disables Legacy Foreground Dispatch
     */
    fun disableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableForegroundDispatch(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed disabling foreground dispatch", e)
        }
    }

    // Helper: extracts raw payload string from NDEF message
    private fun extractRawPayload(message: NdefMessage): String {
        return buildString {
            for ((index, record) in message.records.withIndex()) {
                appendLine("[Record $index] MIME: ${record.toMimeType() ?: "Well-Known"}")
                try {
                    appendLine(String(record.payload, Charset.forName("UTF-8")))
                } catch (_: Exception) {
                    appendLine("(Binary ${record.payload.size} bytes)")
                }
            }
        }
    }

    // Helper: Serializes BusinessCard to compact JSON
    private fun buildCardJson(card: BusinessCard): String {
        return buildString {
            append("{")
            append("\"fn\":\"${escapeJson(card.fullName)}\",")
            append("\"title\":\"${escapeJson(card.jobTitle)}\",")
            append("\"org\":\"${escapeJson(card.company)}\",")
            append("\"phone\":\"${escapeJson(card.phone)}\",")
            append("\"phone2\":\"${escapeJson(card.secondaryPhone)}\",")
            append("\"email\":\"${escapeJson(card.email)}\",")
            append("\"web\":\"${escapeJson(card.website)}\",")
            append("\"addr\":\"${escapeJson(card.address)}\",")
            append("\"cat\":\"${escapeJson(card.category)}\",")
            append("\"notes\":\"${escapeJson(card.notes)}\",")
            append("\"tmpl\":\"${escapeJson(card.cardLayoutTemplate)}\",")
            append("\"w\":${card.cardWidthMm},")
            append("\"h\":${card.cardHeightMm},")
            append("\"std\":\"${escapeJson(card.cardStandardName)}\"")
            append("}")
        }
    }

    // Helper: Deserializes compact JSON to BusinessCard
    private fun parseCardFromJson(json: String): BusinessCard? {
        return try {
            fun getVal(key: String): String {
                val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
                return pattern.find(json)?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\n", "\n") ?: ""
            }
            fun getFloat(key: String, default: Float): Float {
                val pattern = Regex("\"$key\"\\s*:\\s*([0-9.]+)")
                return pattern.find(json)?.groupValues?.get(1)?.toFloatOrNull() ?: default
            }

            val fn = getVal("fn")
            if (fn.isBlank()) return null

            BusinessCard(
                fullName = fn,
                jobTitle = getVal("title"),
                company = getVal("org"),
                phone = getVal("phone"),
                secondaryPhone = getVal("phone2"),
                email = getVal("email"),
                website = getVal("web"),
                address = getVal("addr"),
                category = getVal("cat").ifBlank { "Corporate" },
                notes = getVal("notes").ifBlank { "Imported via CardMate NFC Beam" },
                cardLayoutTemplate = getVal("tmpl").ifBlank { "modern_slate" },
                cardWidthMm = getFloat("w", 88.9f),
                cardHeightMm = getFloat("h", 50.8f),
                cardStandardName = getVal("std").ifBlank { "Standard US (3.5\" × 2.0\")" },
                isBackedUpToCloud = true,
                rawOcrText = json
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing JSON card", e)
            null
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("\t", "\\t")
    }
}
