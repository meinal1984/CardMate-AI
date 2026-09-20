package com.example.nfc

import android.content.Context
import android.nfc.NdefMessage
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.example.data.model.BusinessCard
import java.util.Arrays

/**
 * Host-based Card Emulation (HCE) Service.
 * Allows this Android device to act as an NFC Type 4 Tag, broadcasting the user's
 * digital business card when tapped against another phone or NFC reader.
 */
class CardMateHceService : HostApduService() {

    companion object {
        private const val TAG = "CardMateHceService"

        // ISO-DEP APDU Command Constants
        private val APDU_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val APDU_UNKNOWN_CMD = byteArrayOf(0x6D.toByte(), 0x00.toByte())
        private val APDU_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        // Standard NDEF Application AID (D2760000850101)
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        // CardMate Custom AID (F0010203040506)
        private val CARDMATE_AID = byteArrayOf(
            0xF0.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(),
            0x04.toByte(), 0x05.toByte(), 0x06.toByte()
        )

        // Capability Container (CC) File ID: E1 03
        private val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03.toByte())

        // NDEF Data File ID: E1 04
        private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())

        // Capability Container File Content (15 bytes)
        // CCLEN (00 0F), Mapping Version 2.0 (20), MLe (00 7F), MLc (00 7F),
        // NDEF File Control TLV (04 06 E1 04 08 00 00 00)
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F, // CCLEN
            0x20,       // Mapping Version 2.0
            0x00, 0x7F, // MLe (Max R-APDU size: 127 bytes)
            0x00, 0x7F, // MLc (Max C-APDU size: 127 bytes)
            0x04, 0x06, // NDEF File Control TLV
            0xE1.toByte(), 0x04.toByte(), // File ID E1 04
            0x08, 0x00, // Max NDEF Size (2048 bytes)
            0x00,       // Read Access: Always allowed
            0xFF.toByte() // Write Access: Read-only in emulation
        )
    }

    private var selectedFile: ByteArray? = null
    private var cachedNdefPayload: ByteArray? = null

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.size < 4) {
            return APDU_UNKNOWN_CMD
        }

        val cla = commandApdu[0]
        val ins = commandApdu[1]
        val p1 = commandApdu[2]
        val p2 = commandApdu[3]

        // 1. SELECT Command (INS = A4)
        if (cla == 0x00.toByte() && ins == 0xA4.toByte()) {
            // SELECT BY NAME (AID) - P1 = 04
            if (p1 == 0x04.toByte() && p2 == 0x00.toByte()) {
                val lc = commandApdu[4].toInt() and 0xFF
                if (commandApdu.size >= 5 + lc) {
                    val aid = commandApdu.copyOfRange(5, 5 + lc)
                    if (Arrays.equals(aid, NDEF_AID) || Arrays.equals(aid, CARDMATE_AID)) {
                        Log.d(TAG, "Selected NDEF / CardMate Application AID")
                        prepareNdefPayload()
                        selectedFile = null
                        return APDU_SUCCESS
                    }
                }
            }

            // SELECT BY FILE ID - P1 = 00
            if (p1 == 0x00.toByte() && p2 == 0x0C.toByte()) {
                val lc = commandApdu[4].toInt() and 0xFF
                if (lc == 2 && commandApdu.size >= 7) {
                    val fileId = commandApdu.copyOfRange(5, 7)
                    if (Arrays.equals(fileId, CC_FILE_ID)) {
                        selectedFile = CC_FILE_ID
                        Log.d(TAG, "Selected CC File")
                        return APDU_SUCCESS
                    } else if (Arrays.equals(fileId, NDEF_FILE_ID)) {
                        selectedFile = NDEF_FILE_ID
                        Log.d(TAG, "Selected NDEF Data File")
                        return APDU_SUCCESS
                    }
                }
            }
            return APDU_FILE_NOT_FOUND
        }

        // 2. READ BINARY Command (INS = B0)
        if (cla == 0x00.toByte() && ins == 0xB0.toByte()) {
            val offset = ((p1.toInt() and 0xFF) shl 8) or (p2.toInt() and 0xFF)
            val le = if (commandApdu.size >= 5) commandApdu[4].toInt() and 0xFF else 0

            if (Arrays.equals(selectedFile, CC_FILE_ID)) {
                if (offset >= CC_FILE.size) return APDU_SUCCESS
                val length = minOf(if (le == 0) CC_FILE.size - offset else le, CC_FILE.size - offset)
                val response = ByteArray(length + 2)
                System.arraycopy(CC_FILE, offset, response, 0, length)
                response[length] = 0x90.toByte()
                response[length + 1] = 0x00.toByte()
                return response
            }

            if (Arrays.equals(selectedFile, NDEF_FILE_ID)) {
                val payload = cachedNdefPayload ?: prepareNdefPayload()
                if (offset >= payload.size) return APDU_SUCCESS
                val length = minOf(if (le == 0) payload.size - offset else le, payload.size - offset)
                val response = ByteArray(length + 2)
                System.arraycopy(payload, offset, response, 0, length)
                response[length] = 0x90.toByte()
                response[length + 1] = 0x00.toByte()
                return response
            }

            return APDU_FILE_NOT_FOUND
        }

        return APDU_UNKNOWN_CMD
    }

    override fun onDeactivated(reason: Int) {
        selectedFile = null
        Log.d(TAG, "NFC HCE Deactivated, reason: $reason")
    }

    /**
     * Builds the NDEF File payload: 2 bytes length (NLEN) + NDEF Message bytes
     */
    private fun prepareNdefPayload(): ByteArray {
        val prefs = getSharedPreferences("cardmate_user_profile", Context.MODE_PRIVATE)
        val name = prefs.getString("full_name", "") ?: ""
        val title = prefs.getString("job_title", "") ?: ""
        val company = prefs.getString("company", "") ?: ""
        val phone = prefs.getString("phone", "") ?: ""
        val email = prefs.getString("email", "") ?: ""
        val website = prefs.getString("website", "") ?: ""
        val address = prefs.getString("address", "") ?: ""

        val card = if (name.isNotBlank()) {
            BusinessCard(
                fullName = name,
                jobTitle = title,
                company = company,
                phone = phone,
                email = email,
                website = website,
                address = address,
                category = "Corporate",
                notes = "Exchanged via CardMate Contactless NFC Beam",
                cardLayoutTemplate = "modern_slate"
            )
        } else {
            BusinessCard(
                fullName = "CardMate Contact Card",
                jobTitle = "Contactless Card Exchange",
                company = "CardMate AI",
                phone = "+880 1700-000000",
                email = "info@cardmate.ai",
                website = "https://cardmate.ai",
                address = "Dhaka, Bangladesh",
                category = "Tech & IT",
                notes = "Contactless Digital Business Card"
            )
        }

        val ndefMessage = NfcCardService.createCardNdefMessage(card, packageName = packageName, includeAar = true)
        val msgBytes = ndefMessage.toByteArray()
        val nlen = msgBytes.size

        val fullPayload = ByteArray(nlen + 2)
        fullPayload[0] = ((nlen shr 8) and 0xFF).toByte()
        fullPayload[1] = (nlen and 0xFF).toByte()
        System.arraycopy(msgBytes, 0, fullPayload, 2, nlen)

        cachedNdefPayload = fullPayload
        return fullPayload
    }
}
