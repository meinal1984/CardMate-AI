package com.example.sync

import android.util.Log
import com.example.data.model.BusinessCard
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Robust Firebase Firestore Manager for cloud backup and multi-device synchronization
 * of digital business cards.
 */
object FirestoreManager {
    private const val TAG = "CardMateFirestore"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_CARDS = "cards"
    private const val COLLECTION_PROFILE = "profile"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore initialization fallback: ${e.message}")
            null
        }
    }

    val isAvailable: Boolean
        get() = firestore != null

    /**
     * Backup or update a single card in Firestore under users/{userId}/cards/{cardId}
     */
    suspend fun backupCard(userId: String, card: BusinessCard): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized"))
        try {
            val docId = if (card.id > 0) card.id.toString() else "card_${card.createdAt}"
            val data = cardToMap(card)
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CARDS)
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up card to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Batch / sequence backup of multiple cards to Firestore with real progress callback
     */
    suspend fun backupAllCards(
        userId: String,
        cards: List<BusinessCard>,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized"))
        try {
            if (cards.isEmpty()) {
                onProgress(100, "No cards to backup")
                return@withContext Result.success(0)
            }

            onProgress(10, "Connecting to Firebase Firestore Cloud Vault...")
            val userCardsRef = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CARDS)

            var completedCount = 0
            val total = cards.size

            // Process in batches or sequential writes
            for ((index, card) in cards.withIndex()) {
                val docId = if (card.id > 0) card.id.toString() else "card_${card.createdAt}_$index"
                val data = cardToMap(card)
                userCardsRef.document(docId).set(data, SetOptions.merge()).await()
                completedCount++
                val progressPercent = 10 + ((completedCount.toFloat() / total) * 80).toInt()
                onProgress(progressPercent, "Backing up card $completedCount of $total (${card.fullName})...")
            }

            onProgress(100, "All $completedCount cards backed up to Firestore successfully!")
            Result.success(completedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup all cards to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch all cards from Firestore for the given user to restore or sync across devices
     */
    suspend fun fetchCardsFromCloud(
        userId: String,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<List<BusinessCard>> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized"))
        try {
            onProgress(20, "Querying Firebase Firestore Cloud Vault...")
            val snapshot = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CARDS)
                .get()
                .await()

            onProgress(60, "Parsing ${snapshot.size()} card documents from Cloud...")
            val cardsList = mutableListOf<BusinessCard>()

            for (doc in snapshot.documents) {
                val data = doc.data
                if (data != null) {
                    val card = mapToCard(doc.id, data)
                    cardsList.add(card)
                }
            }

            onProgress(100, "Successfully retrieved ${cardsList.size} cards from Firestore!")
            Result.success(cardsList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch cards from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a card from Firestore when removed locally
     */
    suspend fun deleteCardFromCloud(userId: String, cardId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized"))
        try {
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CARDS)
                .document(cardId.toString())
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting card $cardId from Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get total cloud card count
     */
    suspend fun getCloudCardCount(userId: String): Int = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext 0
        try {
            val snapshot = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CARDS)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }

    private fun cardToMap(card: BusinessCard): Map<String, Any?> {
        return mapOf(
            "id" to card.id,
            "fullName" to card.fullName,
            "jobTitle" to card.jobTitle,
            "company" to card.company,
            "phone" to card.phone,
            "secondaryPhone" to card.secondaryPhone,
            "email" to card.email,
            "website" to card.website,
            "address" to card.address,
            "category" to card.category,
            "notes" to card.notes,
            "socialLinks" to card.socialLinks,
            "cardFrontImageUri" to (card.cardFrontImageUri ?: ""),
            "cardBackImageUri" to (card.cardBackImageUri ?: ""),
            "cardLayoutTemplate" to card.cardLayoutTemplate,
            "cardWidthMm" to card.cardWidthMm.toDouble(),
            "cardHeightMm" to card.cardHeightMm.toDouble(),
            "cardStandardName" to card.cardStandardName,
            "isFavorite" to card.isFavorite,
            "isSyncedWithGoogleContacts" to card.isSyncedWithGoogleContacts,
            "isBackedUpToCloud" to true,
            "createdAt" to card.createdAt,
            "updatedAt" to card.updatedAt,
            "rawOcrText" to card.rawOcrText,
            "language" to card.language,
            "customPrimaryBgColor" to card.customPrimaryBgColor,
            "customSecondaryBgColor" to card.customSecondaryBgColor,
            "customAccentColor" to card.customAccentColor,
            "customTextColor" to card.customTextColor,
            "fontFamilyType" to card.fontFamilyType,
            "layoutStyle" to card.layoutStyle,
            "bgPattern" to card.bgPattern,
            "cornerRadiusDp" to card.cornerRadiusDp,
            "borderStyle" to card.borderStyle,
            "textAlignment" to card.textAlignment,
            "showQrBadge" to card.showQrBadge,
            "showNfcBadge" to card.showNfcBadge,
            "showAvatar" to card.showAvatar,
            "showCategoryBadge" to card.showCategoryBadge,
            "lastSyncedTimestamp" to System.currentTimeMillis()
        )
    }

    private fun mapToCard(docId: String, data: Map<String, Any?>): BusinessCard {
        val parsedId = (data["id"] as? Number)?.toLong()
            ?: docId.toLongOrNull()
            ?: 0L

        return BusinessCard(
            id = 0L, // Reset to 0L so Room can auto-generate or merge safely on new device
            fullName = data["fullName"] as? String ?: "",
            jobTitle = data["jobTitle"] as? String ?: "",
            company = data["company"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            secondaryPhone = data["secondaryPhone"] as? String ?: "",
            email = data["email"] as? String ?: "",
            website = data["website"] as? String ?: "",
            address = data["address"] as? String ?: "",
            category = data["category"] as? String ?: "Corporate",
            notes = data["notes"] as? String ?: "",
            socialLinks = data["socialLinks"] as? String ?: "",
            cardFrontImageUri = (data["cardFrontImageUri"] as? String)?.takeIf { it.isNotBlank() },
            cardBackImageUri = (data["cardBackImageUri"] as? String)?.takeIf { it.isNotBlank() },
            cardLayoutTemplate = data["cardLayoutTemplate"] as? String ?: "modern_slate",
            cardWidthMm = (data["cardWidthMm"] as? Number)?.toFloat() ?: 88.9f,
            cardHeightMm = (data["cardHeightMm"] as? Number)?.toFloat() ?: 50.8f,
            cardStandardName = data["cardStandardName"] as? String ?: "Standard US (3.5\" × 2.0\")",
            isFavorite = data["isFavorite"] as? Boolean ?: false,
            isSyncedWithGoogleContacts = data["isSyncedWithGoogleContacts"] as? Boolean ?: false,
            isBackedUpToCloud = true,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            rawOcrText = data["rawOcrText"] as? String ?: "",
            language = data["language"] as? String ?: "auto",
            customPrimaryBgColor = (data["customPrimaryBgColor"] as? Number)?.toLong(),
            customSecondaryBgColor = (data["customSecondaryBgColor"] as? Number)?.toLong(),
            customAccentColor = (data["customAccentColor"] as? Number)?.toLong(),
            customTextColor = (data["customTextColor"] as? Number)?.toLong(),
            fontFamilyType = data["fontFamilyType"] as? String ?: "sans_serif",
            layoutStyle = data["layoutStyle"] as? String ?: "modern_floating",
            bgPattern = data["bgPattern"] as? String ?: "gradient",
            cornerRadiusDp = (data["cornerRadiusDp"] as? Number)?.toInt() ?: 18,
            borderStyle = data["borderStyle"] as? String ?: "subtle",
            textAlignment = data["textAlignment"] as? String ?: "left",
            showQrBadge = data["showQrBadge"] as? Boolean ?: true,
            showNfcBadge = data["showNfcBadge"] as? Boolean ?: true,
            showAvatar = data["showAvatar"] as? Boolean ?: true,
            showCategoryBadge = data["showCategoryBadge"] as? Boolean ?: true
        )
    }
}
