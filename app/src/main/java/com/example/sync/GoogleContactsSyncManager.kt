package com.example.sync

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.data.model.BusinessCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class GoogleSyncOutcome(
    val isSuccess: Boolean,
    val method: String, // "Google People API v1 (Cloud)" or "Android Google Account Sync"
    val resourceName: String? = null,
    val webUrl: String? = null,
    val message: String
)

data class GoogleSyncState(
    val isConnected: Boolean = false,
    val googleAccountEmail: String = "",
    val oauthToken: String = "",
    val totalSyncedToGoogle: Int = 0,
    val lastSyncTime: Long = 0L,
    val lastStatusMessage: String = ""
)

object GoogleContactsSyncManager {

    private const val TAG = "GoogleContactsSync"
    private const val PREFS_NAME = "google_contacts_sync_prefs"
    private const val KEY_OAUTH_TOKEN = "google_oauth_token"
    private const val KEY_GOOGLE_ACCOUNT = "google_account_email"
    private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    private const val KEY_TOTAL_SYNCED = "total_synced"

    private val _syncState = MutableStateFlow(GoogleSyncState())
    val syncState: StateFlow<GoogleSyncState> = _syncState.asStateFlow()

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val token = prefs.getString(KEY_OAUTH_TOKEN, "") ?: ""
        var account = prefs.getString(KEY_GOOGLE_ACCOUNT, "") ?: ""
        val lastSync = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        val totalSynced = prefs.getInt(KEY_TOTAL_SYNCED, 0)

        // If no account stored, detect from system accounts or default
        if (account.isBlank()) {
            val detected = getAvailableGoogleAccounts(context)
            account = if (detected.isNotEmpty()) detected.first() else "mrinal.eee@gmail.com"
        }

        _syncState.value = GoogleSyncState(
            isConnected = token.isNotBlank() || account.isNotBlank(),
            googleAccountEmail = account,
            oauthToken = token,
            totalSyncedToGoogle = totalSynced,
            lastSyncTime = lastSync,
            lastStatusMessage = if (token.isNotBlank()) "Google People API Connected" else "Google Account Sync Ready ($account)"
        )
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setGoogleAccount(context: Context, email: String) {
        val clean = email.trim()
        getPrefs(context).edit().putString(KEY_GOOGLE_ACCOUNT, clean).apply()
        _syncState.value = _syncState.value.copy(
            googleAccountEmail = clean,
            isConnected = clean.isNotBlank() || _syncState.value.oauthToken.isNotBlank(),
            lastStatusMessage = "Account updated: $clean"
        )
    }

    fun setOAuthToken(context: Context, token: String) {
        val clean = token.trim()
        getPrefs(context).edit().putString(KEY_OAUTH_TOKEN, clean).apply()
        _syncState.value = _syncState.value.copy(
            oauthToken = clean,
            isConnected = clean.isNotBlank() || _syncState.value.googleAccountEmail.isNotBlank(),
            lastStatusMessage = if (clean.isNotBlank()) "OAuth 2.0 Token saved (People API)" else "Local Google Sync Active"
        )
    }

    /**
     * Finds Google accounts registered on this Android device.
     */
    fun getAvailableGoogleAccounts(context: Context): List<String> {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            accounts.map { it.name }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read device accounts: ${e.message}")
            emptyList()
        }
    }

    /**
     * Primary synchronization method for a single business card.
     * Uses Google People API v1 if token is present, and always ensures local
     * Android ContactsProvider has the contact marked under the user's Google Account.
     */
    suspend fun syncCardToGoogleContacts(context: Context, card: BusinessCard): GoogleSyncOutcome = withContext(Dispatchers.IO) {
        init(context)
        val token = _syncState.value.oauthToken
        val account = _syncState.value.googleAccountEmail.ifBlank { "mrinal.eee@gmail.com" }

        var apiSuccess = false
        var resourceName: String? = null
        var webUrl: String? = null
        var errorMessage: String? = null

        // 1. If Google OAuth token is configured, call Google People API v1
        if (token.isNotBlank()) {
            val apiResult = GoogleContactsApiClient.createContact(card, token)
            if (apiResult.isSuccess) {
                apiSuccess = true
                resourceName = apiResult.resourceName
                webUrl = apiResult.webUrl
            } else {
                errorMessage = apiResult.errorMessage
                Log.w(TAG, "People API sync returned error: $errorMessage, falling back to Google Account Provider")
            }
        }

        // 2. Sync to Android Google Contacts Provider (accountType = com.google)
        val providerSuccess = ContactsSyncManager.syncDirectlyToGoogleContacts(
            context = context,
            card = card,
            accountType = "com.google",
            accountName = account
        )

        val isSuccess = apiSuccess || providerSuccess
        val method = when {
            apiSuccess -> "Google People API v1 (Cloud)"
            providerSuccess -> "Google Account Sync ($account)"
            else -> "Failed to sync"
        }

        if (isSuccess) {
            val newTotal = _syncState.value.totalSyncedToGoogle + 1
            val now = System.currentTimeMillis()
            getPrefs(context).edit()
                .putInt(KEY_TOTAL_SYNCED, newTotal)
                .putLong(KEY_LAST_SYNC_TIME, now)
                .apply()

            _syncState.value = _syncState.value.copy(
                totalSyncedToGoogle = newTotal,
                lastSyncTime = now,
                lastStatusMessage = "Synced ${card.fullName} via $method"
            )

            GoogleSyncOutcome(
                isSuccess = true,
                method = method,
                resourceName = resourceName,
                webUrl = webUrl ?: "https://contacts.google.com",
                message = "Contact '${card.fullName}' successfully synced to Google Contacts!"
            )
        } else {
            GoogleSyncOutcome(
                isSuccess = false,
                method = method,
                message = errorMessage ?: "Could not write contact to Google Contacts provider."
            )
        }
    }

    /**
     * Batch synchronizes multiple business cards to Google Contacts.
     */
    suspend fun syncMultipleCards(
        context: Context,
        cards: List<BusinessCard>,
        onProgress: (current: Int, total: Int) -> Unit
    ): GoogleContactsApiClient.GoogleBatchSyncReport = withContext(Dispatchers.IO) {
        init(context)
        var successCount = 0
        var failCount = 0
        val resources = mutableListOf<String>()

        cards.forEachIndexed { index, card ->
            val result = syncCardToGoogleContacts(context, card)
            if (result.isSuccess) {
                successCount++
                result.resourceName?.let { resources.add(it) }
            } else {
                failCount++
            }
            onProgress(index + 1, cards.size)
        }

        GoogleContactsApiClient.GoogleBatchSyncReport(
            totalCards = cards.size,
            successfulCount = successCount,
            failedCount = failCount,
            syncedResourceNames = resources
        )
    }

    /**
     * Opens the web Google Contacts interface or a specific contact.
     */
    fun openGoogleContactsWeb(context: Context, resourceName: String? = null) {
        try {
            val contactId = resourceName?.substringAfter("people/")
            val url = if (!contactId.isNullOrBlank()) {
                "https://contacts.google.com/person/$contactId"
            } else {
                "https://contacts.google.com"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot launch browser for Google Contacts", e)
        }
    }
}
