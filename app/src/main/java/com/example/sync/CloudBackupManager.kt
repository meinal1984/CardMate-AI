package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.BusinessCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CloudSyncState(
    val isAutoBackupEnabled: Boolean = true,
    val lastBackupTimestamp: Long = 0L,
    val totalCloudCards: Int = 0,
    val cloudStorageUsedKb: Long = 0,
    val cloudStorageQuotaKb: Long = 10485760, // 10 GB
    val userEmail: String = "",
    val backupProvider: String = "Firebase Firestore Cloud Vault"
)

object CloudBackupManager {

    private const val PREFS_NAME = "cardmate_cloud_sync_prefs"
    private const val KEY_AUTO_BACKUP = "auto_backup"
    private const val KEY_LAST_BACKUP = "last_backup"
    private const val KEY_CLOUD_COUNT = "cloud_count"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSyncState(context: Context): CloudSyncState {
        val prefs = getPrefs(context)
        val user = AuthManager.currentUser
        val email = user?.email ?: user?.displayName ?: "Offline / Local"
        val cloudCount = prefs.getInt(KEY_CLOUD_COUNT, 0)
        val lastBackup = prefs.getLong(KEY_LAST_BACKUP, 0L)
        val estimatedStorage = (cloudCount * 45L) // avg 45KB per card record

        return CloudSyncState(
            isAutoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP, true),
            lastBackupTimestamp = if (lastBackup > 0) lastBackup else System.currentTimeMillis(),
            totalCloudCards = cloudCount,
            cloudStorageUsedKb = estimatedStorage,
            cloudStorageQuotaKb = 10485760, // 10 GB Firestore quota tier
            userEmail = email,
            backupProvider = if (user != null) "Firebase Firestore (Account: $email)" else "Firebase Firestore (Sign-in Required)"
        )
    }

    fun setAutoBackup(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_BACKUP, true)
    }

    /**
     * Executes cloud backup routine to Firebase Firestore with progress updates.
     */
    suspend fun executeCloudBackup(
        context: Context,
        cards: List<BusinessCard>,
        onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = AuthManager.currentUser
            val userId = user?.uid ?: "local_guest_user"

            onProgress(10, "Connecting to Firebase Firestore Cloud...")

            val result = FirestoreManager.backupAllCards(userId, cards, onProgress)

            if (result.isSuccess) {
                val backedUpCount = result.getOrDefault(cards.size)
                val prefs = getPrefs(context)
                prefs.edit()
                    .putLong(KEY_LAST_BACKUP, System.currentTimeMillis())
                    .putInt(KEY_CLOUD_COUNT, backedUpCount)
                    .apply()
                true
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Unknown Firestore error"
                onProgress(0, "Firestore Backup error: $error")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(0, "Backup error: ${e.localizedMessage}")
            false
        }
    }

    /**
     * Single card backup to Firestore
     */
    suspend fun backupSingleCard(card: BusinessCard): Boolean = withContext(Dispatchers.IO) {
        val user = AuthManager.currentUser ?: return@withContext false
        val result = FirestoreManager.backupCard(user.uid, card)
        result.isSuccess
    }

    /**
     * Restore cards from Firebase Firestore
     */
    suspend fun executeCloudRestore(
        context: Context,
        onProgress: (Int, String) -> Unit
    ): List<BusinessCard> = withContext(Dispatchers.IO) {
        try {
            val user = AuthManager.currentUser
            val userId = user?.uid ?: "local_guest_user"

            onProgress(15, "Connecting to Firebase Firestore...")
            val result = FirestoreManager.fetchCardsFromCloud(userId, onProgress)

            if (result.isSuccess) {
                val cards = result.getOrDefault(emptyList())
                val prefs = getPrefs(context)
                prefs.edit()
                    .putInt(KEY_CLOUD_COUNT, cards.size)
                    .putLong(KEY_LAST_BACKUP, System.currentTimeMillis())
                    .apply()
                cards
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
