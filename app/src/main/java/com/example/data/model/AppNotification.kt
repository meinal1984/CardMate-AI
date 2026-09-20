package com.example.data.model

import java.util.UUID

enum class NotificationType {
    CARD_SCANNED,
    BACKUP_COMPLETED,
    CONTACT_SYNCED,
    NFC_READY,
    REMINDER_FOLLOW_UP,
    TIPS_TRICKS,
    SYSTEM_UPDATE
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val titleEn: String,
    val titleBn: String,
    val messageEn: String,
    val messageBn: String,
    val type: NotificationType = NotificationType.SYSTEM_UPDATE,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetTabName: String? = null,
    val targetCardId: Long? = null
)
