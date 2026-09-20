package com.example.nfc

import com.example.data.model.BusinessCard

/**
 * NFC Operation Modes
 */
enum class NfcOperationMode {
    IDLE,
    READ_TAG,
    WRITE_TAG,
    FORMAT_TAG,
    P2P_BEAM
}

/**
 * Metadata about a scanned NFC Tag
 */
data class NfcTagInfo(
    val tagIdHex: String = "",
    val technologies: List<String> = emptyList(),
    val maxSizeBytes: Int = 0,
    val currentSizeBytes: Int = 0,
    val isWritable: Boolean = true,
    val isNdefSupported: Boolean = true,
    val tagType: String = "NFC Forum Tag"
)

/**
 * Sealed result class for NFC Tag Write operations
 */
sealed class NfcWriteResult {
    data class Success(
        val bytesWritten: Int,
        val tagType: String,
        val cardName: String,
        val isAarIncluded: Boolean
    ) : NfcWriteResult()

    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : NfcWriteResult()

    data class InsufficientSpace(
        val requiredBytes: Int,
        val availableBytes: Int,
        val tagType: String
    ) : NfcWriteResult()

    object TagReadOnly : NfcWriteResult()
}

/**
 * Sealed result class for NFC Tag Read operations
 */
sealed class NfcReadResult {
    data class Success(
        val card: BusinessCard,
        val rawPayload: String,
        val tagInfo: NfcTagInfo
    ) : NfcReadResult()

    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : NfcReadResult()

    object EmptyTag : NfcReadResult()
}

/**
 * Live UI state for NFC Tag interaction
 */
data class NfcUiState(
    val mode: NfcOperationMode = NfcOperationMode.READ_TAG,
    val isNfcSupported: Boolean = true,
    val isNfcEnabled: Boolean = true,
    val isProcessing: Boolean = false,
    val statusMessage: String = "",
    val cardToWrite: BusinessCard? = null,
    val lastReadCard: BusinessCard? = null,
    val lastReadTagInfo: NfcTagInfo? = null,
    val lastWriteResult: NfcWriteResult? = null,
    val includeAarInWrite: Boolean = true,
    val isHceActive: Boolean = true
)
