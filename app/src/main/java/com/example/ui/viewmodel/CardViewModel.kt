package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CardAiHistoryManager
import com.example.ai.CardScanResult
import com.example.ai.ChatMessage
import com.example.ai.ChatPersona
import com.example.ai.ChatPersonaType
import com.example.ai.ChatRole
import com.example.ai.GeminiCardScannerService
import com.example.ai.GeminiChatService
import com.example.ai.GeminiModels
import com.example.ai.GroundingPlaceSource
import com.example.ai.OcrParserHelper
import com.example.image.CardDetectionState
import com.example.image.CardImageProcessor
import com.example.image.CardQuadCorners
import com.example.image.DetectedCardFrame
import com.example.image.ImageQualityScore
import com.example.image.SmartCardDetectionEngine
import com.example.data.database.AppDatabase
import com.example.data.model.BusinessCard
import com.example.data.model.CardDesignPreset
import com.example.data.model.ProfileFieldItem
import com.example.data.model.UserProfile
import com.example.data.repository.CardRepository
import java.util.UUID
import com.example.nfc.NfcCardService
import com.example.nfc.NfcOperationMode
import com.example.nfc.NfcReadResult
import com.example.nfc.NfcTagInfo
import com.example.nfc.NfcUiState
import com.example.nfc.NfcWriteResult
import com.example.sync.AuthManager
import com.example.sync.BackupRestoreHelper
import com.example.sync.CloudBackupManager
import com.example.sync.CloudSyncState
import com.example.sync.ContactsSyncManager
import com.example.sync.FirestoreManager
import com.example.sync.GoogleContactsApiClient
import com.example.sync.GoogleContactsSyncManager
import com.example.sync.GoogleSyncOutcome
import com.example.sync.GoogleSyncState
import com.example.sync.VCardExporter
import com.example.widget.CardMateWidgetProvider
import android.content.Intent
import android.nfc.Tag
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.example.util.NotificationHelper
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOption {
    NEWEST, OLDEST, NAME_ASC, NAME_DESC, COMPANY
}

enum class NavigationTab {
    DASHBOARD, CARDS, SCANNER, NFC_QR, PROFILE, SETTINGS
}

enum class ScanMode {
    DUAL_SIDE, SINGLE_SIDE
}

enum class ScanStep {
    FRONT, BACK
}

data class ScannerUiState(
    val isScanning: Boolean = false,
    val isProcessingAi: Boolean = false,
    val scanMode: ScanMode = ScanMode.DUAL_SIDE,
    val currentStep: ScanStep = ScanStep.FRONT,
    val detectionState: CardDetectionState = CardDetectionState.SEARCHING,
    val isAutoCaptureEnabled: Boolean = true,
    val isAdaptiveEnhancementEnabled: Boolean = true,
    val lastAppliedAdaptiveEnhancement: String? = null,
    val isAdaptiveAdjustmentActive: Boolean = false,
    val detectedCorners: CardQuadCorners = CardQuadCorners.DEFAULT,
    val detectionConfidence: Float = 0f,
    val isStable: Boolean = false,
    val stableProgress: Float = 0f,
    val hasGlare: Boolean = false,
    val isBlurry: Boolean = false,
    val frontBitmap: Bitmap? = null,
    val backBitmap: Bitmap? = null,
    val frontOriginalUri: String? = null,
    val backOriginalUri: String? = null,
    val frontImageUri: String? = null,
    val backImageUri: String? = null,
    val frontQualityScore: ImageQualityScore? = null,
    val backQualityScore: ImageQualityScore? = null,
    val isPerspectiveCorrected: Boolean = true,
    val hasQualityWarning: Boolean = false,
    val qualityWarningMessage: String? = null,
    val progressMessage: String = "",
    val detectedWidthMm: Float = 88.9f,
    val detectedHeightMm: Float = 50.8f,
    val detectedSizeStandard: String = "Standard US (3.5\" × 2.0\")",
    val aspectRatio: Float = 1.75f,
    val capturedBitmap: Bitmap? = null,
    val scanResult: CardScanResult? = null,
    val errorMessage: String? = null
)

class CardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = CardRepository(database.businessCardDao(), database.cardDesignPresetDao())
    
    // Navigation & UI Preferences
    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isBanglaLanguage = MutableStateFlow(false)
    val isBanglaLanguage: StateFlow<Boolean> = _isBanglaLanguage.asStateFlow()

    // Privacy, Auto-Sync & Sharing Preferences
    private val _isNfcSharingEnabled = MutableStateFlow(true)
    val isNfcSharingEnabled: StateFlow<Boolean> = _isNfcSharingEnabled.asStateFlow()

    private val _isQrSharingEnabled = MutableStateFlow(true)
    val isQrSharingEnabled: StateFlow<Boolean> = _isQrSharingEnabled.asStateFlow()

    private val _isAutoSyncToPhoneContactsEnabled = MutableStateFlow(true)
    val isAutoSyncToPhoneContactsEnabled: StateFlow<Boolean> = _isAutoSyncToPhoneContactsEnabled.asStateFlow()

    // User Profile
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // NFC Tag Operations UI State
    private val _nfcUiState = MutableStateFlow(
        NfcUiState(
            isNfcSupported = NfcCardService.isNfcSupported(application),
            isNfcEnabled = NfcCardService.isNfcEnabled(application)
        )
    )
    val nfcUiState: StateFlow<NfcUiState> = _nfcUiState.asStateFlow()

    // Multi-turn Gemini AI Chat & Google Maps Grounding State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatThinking = MutableStateFlow(false)
    val isChatThinking: StateFlow<Boolean> = _isChatThinking.asStateFlow()

    private val _activeChatPersona = MutableStateFlow(GeminiChatService.ALL_PERSONAS[0])
    val activeChatPersona: StateFlow<ChatPersona> = _activeChatPersona.asStateFlow()

    private val _activeChatModel = MutableStateFlow(GeminiModels.GEMINI_3_5_FLASH)
    val activeChatModel: StateFlow<String> = _activeChatModel.asStateFlow()

    private val _isMapsGroundingEnabled = MutableStateFlow(true)
    val isMapsGroundingEnabled: StateFlow<Boolean> = _isMapsGroundingEnabled.asStateFlow()

    private val _activeChatCardId = MutableStateFlow<Long?>(null)
    val activeChatCardId: StateFlow<Long?> = _activeChatCardId.asStateFlow()

    private val _activeChatCard = MutableStateFlow<BusinessCard?>(null)
    val activeChatCard: StateFlow<BusinessCard?> = _activeChatCard.asStateFlow()

    private val _chatInputDraft = MutableStateFlow("")
    val chatInputDraft: StateFlow<String> = _chatInputDraft.asStateFlow()

    private fun loadPrivacyPreferences() {
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_privacy_prefs", Context.MODE_PRIVATE)
        _isNfcSharingEnabled.value = prefs.getBoolean("nfc_sharing_enabled", true)
        _isQrSharingEnabled.value = prefs.getBoolean("qr_sharing_enabled", true)
        _isAutoSyncToPhoneContactsEnabled.value = prefs.getBoolean("auto_sync_to_contacts", true)
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        CloudBackupManager.setAutoBackup(getApplication(), enabled)
        _cloudSyncState.value = CloudBackupManager.getSyncState(getApplication())
    }

    fun setNfcSharingEnabled(enabled: Boolean) {
        _isNfcSharingEnabled.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_privacy_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("nfc_sharing_enabled", enabled).apply()
    }

    fun setQrSharingEnabled(enabled: Boolean) {
        _isQrSharingEnabled.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_privacy_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("qr_sharing_enabled", enabled).apply()
    }

    fun setAutoSyncToPhoneContacts(enabled: Boolean) {
        _isAutoSyncToPhoneContactsEnabled.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_privacy_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_sync_to_contacts", enabled).apply()
    }

    private fun parseFieldListJson(jsonString: String?): List<ProfileFieldItem> {
        if (jsonString.isNullOrBlank()) return emptyList()
        val list = mutableListOf<ProfileFieldItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProfileFieldItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        label = obj.optString("label", "Mobile"),
                        value = obj.optString("value", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun fieldListToJson(list: List<ProfileFieldItem>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("value", item.value)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun loadUserProfile() {
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_user_profile", Context.MODE_PRIVATE)
        val defaultPhones = listOf(
            ProfileFieldItem(label = "Mobile", value = "+8801719205945"),
            ProfileFieldItem(label = "Work", value = "+880 1912-345678"),
            ProfileFieldItem(label = "WhatsApp", value = "+880 1719-205945")
        )
        val defaultEmails = listOf(
            ProfileFieldItem(label = "Work", value = "mrinal.eee@gmail.com"),
            ProfileFieldItem(label = "Personal", value = "mrinal.ai@innovations.com")
        )
        val defaultWebsites = listOf(
            ProfileFieldItem(label = "Company", value = "https://cardmate.ai"),
            ProfileFieldItem(label = "Portfolio", value = "https://mrinal.dev")
        )
        val defaultSocials = listOf(
            ProfileFieldItem(label = "WhatsApp", value = "+8801719205945"),
            ProfileFieldItem(label = "LinkedIn", value = "linkedin.com/in/mrinal-eee"),
            ProfileFieldItem(label = "GitHub", value = "github.com/mrinal-eee")
        )

        val savedPhones = parseFieldListJson(prefs.getString("phones_json", null))
        val savedEmails = parseFieldListJson(prefs.getString("emails_json", null))
        val savedWebsites = parseFieldListJson(prefs.getString("websites_json", null))
        val savedSocials = parseFieldListJson(prefs.getString("socials_json", null))

        _userProfile.value = UserProfile(
            fullName = prefs.getString("fullName", "Mrinal Kanti Roy") ?: "Mrinal Kanti Roy",
            jobTitle = prefs.getString("jobTitle", "Senior Electrical & AI Engineer") ?: "Senior Electrical & AI Engineer",
            company = prefs.getString("company", "CardMate Innovations Ltd.") ?: "CardMate Innovations Ltd.",
            department = prefs.getString("department", "AI & Research Division") ?: "AI & Research Division",
            phone = prefs.getString("phone", "+8801719205945") ?: "+8801719205945",
            secondaryPhone = prefs.getString("secondaryPhone", "+880 1912-345678") ?: "+880 1912-345678",
            email = prefs.getString("email", "mrinal.eee@gmail.com") ?: "mrinal.eee@gmail.com",
            website = prefs.getString("website", "https://cardmate.ai") ?: "https://cardmate.ai",
            address = prefs.getString("address", "Gulshan-2, Dhaka 1212, Bangladesh") ?: "Gulshan-2, Dhaka 1212, Bangladesh",
            category = prefs.getString("category", "Tech & IT") ?: "Tech & IT",
            bio = prefs.getString("bio", "Passionate about AI architectures, embedded hardware, mobile innovation, and digital networking solutions.") ?: "Passionate about AI architectures, embedded hardware, mobile innovation, and digital networking solutions.",
            socialLinks = prefs.getString("socialLinks", "WhatsApp: +8801719205945 | LinkedIn: linkedin.com/in/mrinal-eee | GitHub: github.com/mrinal") ?: "WhatsApp: +8801719205945 | LinkedIn: linkedin.com/in/mrinal-eee | GitHub: github.com/mrinal",
            photoUri = prefs.getString("photoUri", null),
            companyLogoUri = prefs.getString("companyLogoUri", null),
            cardLayoutTemplate = prefs.getString("cardLayoutTemplate", "modern_slate") ?: "modern_slate",
            phoneList = if (savedPhones.isNotEmpty()) savedPhones else defaultPhones,
            emailList = if (savedEmails.isNotEmpty()) savedEmails else defaultEmails,
            websiteList = if (savedWebsites.isNotEmpty()) savedWebsites else defaultWebsites,
            socialList = if (savedSocials.isNotEmpty()) savedSocials else defaultSocials
        )
    }

    fun saveUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_user_profile", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("fullName", profile.fullName)
            putString("jobTitle", profile.jobTitle)
            putString("company", profile.company)
            putString("department", profile.department)
            putString("phone", profile.getResolvedPhones().firstOrNull()?.value ?: profile.phone)
            putString("secondaryPhone", profile.getResolvedPhones().getOrNull(1)?.value ?: profile.secondaryPhone)
            putString("email", profile.getResolvedEmails().firstOrNull()?.value ?: profile.email)
            putString("website", profile.getResolvedWebsites().firstOrNull()?.value ?: profile.website)
            putString("address", profile.address)
            putString("category", profile.category)
            putString("bio", profile.bio)
            putString("socialLinks", profile.socialLinks)
            putString("photoUri", profile.photoUri)
            putString("companyLogoUri", profile.companyLogoUri)
            putString("cardLayoutTemplate", profile.cardLayoutTemplate)
            putString("phones_json", fieldListToJson(profile.phoneList))
            putString("emails_json", fieldListToJson(profile.emailList))
            putString("websites_json", fieldListToJson(profile.websiteList))
            putString("socials_json", fieldListToJson(profile.socialList))
            apply()
        }
    }

    fun exportUserProfileVCard(context: Context) {
        val card = _userProfile.value.toBusinessCard()
        VCardExporter.shareCardAsVCardFile(context, card)
    }

    // Filtering & Searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Cards list
    val allCards: StateFlow<List<BusinessCard>> = repository.allCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCards: StateFlow<List<BusinessCard>> = combine(
        allCards,
        _searchQuery,
        _selectedCategory,
        _sortOption
    ) { cards, query, category, sort ->
        var list = cards

        // Filter Category
        if (category != "All" && category != "সবগুলো") {
            list = list.filter { 
                it.category.equals(category, ignoreCase = true) ||
                (category == "Favorites" && it.isFavorite) ||
                (category == "পছন্দের" && it.isFavorite)
            }
        }

        // Search Query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.fullName.lowercase().contains(q) ||
                it.company.lowercase().contains(q) ||
                it.jobTitle.lowercase().contains(q) ||
                it.phone.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.address.lowercase().contains(q) ||
                it.notes.lowercase().contains(q)
            }
        }

        // Sorting
        when (sort) {
            SortOption.NEWEST -> list.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> list.sortedBy { it.createdAt }
            SortOption.NAME_ASC -> list.sortedBy { it.fullName.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.fullName.lowercase() }
            SortOption.COMPANY -> list.sortedBy { it.company.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Metrics
    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncedCount: StateFlow<Int> = repository.syncedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val backedUpCount: StateFlow<Int> = repository.backedUpCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favoriteCount: StateFlow<Int> = repository.favoriteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Saved Design Themes / Presets in Room
    val allDesignPresets: StateFlow<List<CardDesignPreset>> = repository.allDesignPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Scanner State
    private val _scannerState = MutableStateFlow(ScannerUiState())
    val scannerState: StateFlow<ScannerUiState> = _scannerState.asStateFlow()

    // Cloud Sync State
    private val _cloudSyncState = MutableStateFlow(CloudBackupManager.getSyncState(application))
    val cloudSyncState: StateFlow<CloudSyncState> = _cloudSyncState.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _backupProgress = MutableStateFlow(0)
    val backupProgress: StateFlow<Int> = _backupProgress.asStateFlow()

    private val _backupStatusText = MutableStateFlow("")
    val backupStatusText: StateFlow<String> = _backupStatusText.asStateFlow()

    // Selected Card for Detail View
    private val _selectedCard = MutableStateFlow<BusinessCard?>(null)
    val selectedCard: StateFlow<BusinessCard?> = _selectedCard.asStateFlow()

    // Requested Card from Widget or Notification
    private val _requestedCardToOpen = MutableStateFlow<BusinessCard?>(null)
    val requestedCardToOpen: StateFlow<BusinessCard?> = _requestedCardToOpen.asStateFlow()

    // Notification Center State
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    val unreadNotificationCount: StateFlow<Int> = _notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Custom Categories State (persisted)
    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    init {
        loadUserProfile()
        loadPrivacyPreferences()
        loadCameraPreferences()
        loadNotifications()
        loadCustomCategories()
        GoogleContactsSyncManager.init(application)
        
        // Listen to Auth state changes and refresh Cloud Firestore state
        viewModelScope.launch {
            AuthManager.currentUserFlow.collect {
                _cloudSyncState.value = CloudBackupManager.getSyncState(getApplication())
            }
        }
    }

    private fun loadCameraPreferences() {
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_camera_prefs", Context.MODE_PRIVATE)
        val isAdaptive = prefs.getBoolean("adaptive_enhancement_enabled", true)
        _scannerState.value = _scannerState.value.copy(isAdaptiveEnhancementEnabled = isAdaptive)
    }

    fun setAdaptiveEnhancementEnabled(enabled: Boolean) {
        _scannerState.value = _scannerState.value.copy(isAdaptiveEnhancementEnabled = enabled)
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_camera_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("adaptive_enhancement_enabled", enabled).apply()
    }

    private fun loadCustomCategories() {
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_categories_prefs", Context.MODE_PRIVATE)
        val defaultCustom = setOf("Government", "Engineering", "Consultancy", "Tech & IT", "Corporate", "Healthcare & Medical", "Legal & Advisory", "Education")
        val saved = prefs.getStringSet("custom_categories_set", defaultCustom) ?: defaultCustom
        _customCategories.value = saved.toList().sorted()
    }

    fun addCustomCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isNotBlank()) {
            val updated = (_customCategories.value + trimmed).distinct().sorted()
            _customCategories.value = updated
            val prefs = getApplication<Application>().getSharedPreferences("cardmate_categories_prefs", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("custom_categories_set", updated.toSet()).apply()
        }
    }

    fun removeCustomCategory(category: String) {
        val updated = _customCategories.value.filter { it != category }
        _customCategories.value = updated
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_categories_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("custom_categories_set", updated.toSet()).apply()
    }

    fun mergeDuplicateCards(cardA: BusinessCard, cardB: BusinessCard, mergedCard: BusinessCard) {
        viewModelScope.launch {
            // Save or update primary card with merged content
            if (mergedCard.id != 0L) {
                repository.updateCard(mergedCard)
                // Delete duplicate secondary card if it was an existing database entity
                if (cardB.id != 0L && cardB.id != mergedCard.id) {
                    repository.deleteCard(cardB)
                } else if (cardA.id != 0L && cardA.id != mergedCard.id) {
                    repository.deleteCard(cardA)
                }
            } else {
                repository.insertCard(mergedCard)
                if (cardA.id != 0L) repository.deleteCard(cardA)
                if (cardB.id != 0L) repository.deleteCard(cardB)
            }

            _selectedCard.value = mergedCard
            CardMateWidgetProvider.notifyDataChanged(getApplication())

            addNotification(
                titleEn = "Contacts Merged Successfully",
                titleBn = "কন্টাক্ট সফলভাবে মার্জ করা হয়েছে",
                messageEn = "${cardA.fullName} and ${cardB.fullName} were unified into a complete profile.",
                messageBn = "${cardA.fullName} এবং ${cardB.fullName}-এর তথ্য একীভূত করা হয়েছে।",
                type = NotificationType.SYSTEM_UPDATE,
                targetTabName = "CARDS"
            )
        }
    }

    private fun loadNotifications() {
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_notifications_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("notifications_json", null)
        if (jsonStr.isNullOrBlank()) {
            val defaults = listOf(
                AppNotification(
                    titleEn = "Welcome to CardMate AI",
                    titleBn = "CardMate AI-তে স্বাগতম",
                    messageEn = "Start organizing business cards by scanning or creating your personal NFC profile.",
                    messageBn = "ভিজিটিং কার্ড স্ক্যান করে বা আপনার ডিজিটাল এনএফসি প্রোফাইল তৈরি করে স্মার্ট যোগাযোগ শুরু করুন।",
                    type = NotificationType.SYSTEM_UPDATE,
                    timestamp = System.currentTimeMillis() - 3600_000 * 2,
                    isRead = false,
                    targetTabName = "SCANNER"
                ),
                AppNotification(
                    titleEn = "Auto Cloud Vault Enabled",
                    titleBn = "ক্লাউড অটো ভল্ট সক্রিয় রয়েছে",
                    messageEn = "All your saved cards are automatically backed up securely.",
                    messageBn = "আপনার সকল ভিজিটিং কার্ড নিরাপদে ক্লাউড ভল্টে ব্যাকআপ রাখা হচ্ছে।",
                    type = NotificationType.BACKUP_COMPLETED,
                    timestamp = System.currentTimeMillis() - 3600_000 * 5,
                    isRead = false,
                    targetTabName = "SETTINGS"
                ),
                AppNotification(
                    titleEn = "NFC & QR Tap-to-Share Ready",
                    titleBn = "NFC ও QR শেয়ারিং প্রস্তুত",
                    messageEn = "Easily tap any NFC card to instantly exchange business contacts.",
                    messageBn = "যেকোনো এনএফসি সাপোর্টেড ফোনে ট্যাপ করে তাৎক্ষণিক কন্টাক্ট শেয়ার করুন।",
                    type = NotificationType.NFC_READY,
                    timestamp = System.currentTimeMillis() - 3600_000 * 24,
                    isRead = true,
                    targetTabName = "NFC_QR"
                )
            )
            _notifications.value = defaults
            saveNotificationsToPrefs(defaults)
        } else {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<AppNotification>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AppNotification(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            titleEn = obj.optString("titleEn", ""),
                            titleBn = obj.optString("titleBn", ""),
                            messageEn = obj.optString("messageEn", ""),
                            messageBn = obj.optString("messageBn", ""),
                            type = runCatching { NotificationType.valueOf(obj.optString("type", "SYSTEM_UPDATE")) }.getOrDefault(NotificationType.SYSTEM_UPDATE),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isRead = obj.optBoolean("isRead", false),
                            targetTabName = obj.optString("targetTabName").takeIf { it.isNotBlank() },
                            targetCardId = if (obj.has("targetCardId") && !obj.isNull("targetCardId")) obj.optLong("targetCardId") else null
                        )
                    )
                }
                _notifications.value = list
            } catch (_: Exception) {
                _notifications.value = emptyList()
            }
        }
    }

    private fun saveNotificationsToPrefs(list: List<AppNotification>) {
        val prefs = getApplication<Application>().getSharedPreferences("cardmate_notifications_prefs", Context.MODE_PRIVATE)
        val array = JSONArray()
        list.take(50).forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("titleEn", item.titleEn)
                put("titleBn", item.titleBn)
                put("messageEn", item.messageEn)
                put("messageBn", item.messageBn)
                put("type", item.type.name)
                put("timestamp", item.timestamp)
                put("isRead", item.isRead)
                put("targetTabName", item.targetTabName ?: "")
                if (item.targetCardId != null) put("targetCardId", item.targetCardId)
            }
            array.put(obj)
        }
        prefs.edit().putString("notifications_json", array.toString()).apply()
    }

    fun addNotification(
        titleEn: String,
        titleBn: String,
        messageEn: String,
        messageBn: String,
        type: NotificationType = NotificationType.SYSTEM_UPDATE,
        targetTabName: String? = null,
        targetCardId: Long? = null,
        showSystemNotification: Boolean = true
    ) {
        val newNotification = AppNotification(
            titleEn = titleEn,
            titleBn = titleBn,
            messageEn = messageEn,
            messageBn = messageBn,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            targetTabName = targetTabName,
            targetCardId = targetCardId
        )
        val updated = listOf(newNotification) + _notifications.value
        _notifications.value = updated
        saveNotificationsToPrefs(updated)

        if (showSystemNotification) {
            val title = if (_isBanglaLanguage.value) titleBn else titleEn
            val message = if (_isBanglaLanguage.value) messageBn else messageEn
            NotificationHelper.showSystemNotification(
                context = getApplication(),
                notificationId = (System.currentTimeMillis() % 10000).toInt(),
                title = title,
                message = message
            )
        }
    }

    fun markAsRead(notificationId: String) {
        val updated = _notifications.value.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        _notifications.value = updated
        saveNotificationsToPrefs(updated)
    }

    fun markAllNotificationsAsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        saveNotificationsToPrefs(updated)
    }

    fun deleteNotification(notificationId: String) {
        val updated = _notifications.value.filter { it.id != notificationId }
        _notifications.value = updated
        saveNotificationsToPrefs(updated)
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
        saveNotificationsToPrefs(emptyList())
    }

    fun triggerSampleNotification() {
        val samples = listOf(
            Triple(
                "Smart OCR Enhanced",
                "স্মার্ট ও সি আর স্ক্যানার সক্রিয়",
                "AI Scanner now recognizes dual-side visiting cards with 99.4% precision." to "এআই স্ক্যানার এখন উভয় পাশের বিজনেস কার্ড ৯৯.৪% নির্ভুলভাবে পড়তে পারে।"
            ),
            Triple(
                "Follow-up Reminder",
                "ফলো-আপ রিমাইন্ডার",
                "Don't forget to connect with your new contacts on WhatsApp or LinkedIn." to "আপনার নতুন সংগৃহীত কন্টাক্টদের সাথে হোয়াটসঅ্যাপ বা লিংকডইনে যুক্ত হতে ভুলবেন না।"
            ),
            Triple(
                "Cloud Sync Status",
                "ক্লাউড সিঙ্ক স্ট্যাটাস",
                "Card vault synchronized with Google Identity safely." to "আপনার কার্ড ভল্ট গুগলের সাথে সুরক্ষিতভাবে সিঙ্ক হয়েছে।"
            )
        )
        val sample = samples.random()
        addNotification(
            titleEn = sample.first,
            titleBn = sample.second,
            messageEn = sample.third.first,
            messageBn = sample.third.second,
            type = NotificationType.REMINDER_FOLLOW_UP,
            targetTabName = "DASHBOARD"
        )
    }

    fun openCardById(cardId: Long) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            _requestedCardToOpen.value = card
        }
    }

    fun clearRequestedCard() {
        _requestedCardToOpen.value = null
    }

    // UI Navigation helper
    fun setTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleLanguage() {
        _isBanglaLanguage.value = !_isBanglaLanguage.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun selectCard(card: BusinessCard?) {
        _selectedCard.value = card
    }

    // CRUD
    fun saveCard(card: BusinessCard, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val appCtx = getApplication<Application>().applicationContext
            val cleanCard = sanitizeBusinessCard(card)
            if (cleanCard.id == 0L) {
                val newId = repository.insertCard(cleanCard)
                val insertedCard = cleanCard.copy(id = newId)

                // 1. Auto-Sync to Google Contacts if enabled
                if (_isAutoSyncToPhoneContactsEnabled.value) {
                    val outcome = withContext(Dispatchers.IO) {
                        GoogleContactsSyncManager.syncCardToGoogleContacts(appCtx, insertedCard)
                    }
                    if (outcome.isSuccess) {
                        repository.setGoogleSyncStatus(newId, true)
                        Toast.makeText(
                            appCtx,
                            if (_isBanglaLanguage.value) "কার্ড সংরক্ষিত ও গুগল কন্টাক্টসে অটো-সিঙ্ক হয়েছে!" else "Card saved & synced to Google Contacts!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            appCtx,
                            if (_isBanglaLanguage.value) "কার্ডটি সফলভাবে সেভ করা হয়েছে" else "Card saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        appCtx,
                        if (_isBanglaLanguage.value) "কার্ডটি সফলভাবে সেভ করা হয়েছে" else "Card saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 2. Cloud Backup to Firebase Firestore if signed in
                if (CloudBackupManager.isAutoBackupEnabled(appCtx) && AuthManager.isUserSignedIn) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val backedUp = CloudBackupManager.backupSingleCard(insertedCard)
                        if (backedUp) {
                            repository.setCloudBackupStatus(newId, true)
                        }
                    }
                }

                CardMateWidgetProvider.notifyDataChanged(getApplication())
                onSaved(newId)
            } else {
                repository.updateCard(cleanCard)
                if (_isAutoSyncToPhoneContactsEnabled.value && !cleanCard.isSyncedWithGoogleContacts) {
                    val outcome = withContext(Dispatchers.IO) {
                        GoogleContactsSyncManager.syncCardToGoogleContacts(appCtx, cleanCard)
                    }
                    if (outcome.isSuccess) {
                        repository.setGoogleSyncStatus(cleanCard.id, true)
                        _selectedCard.value = cleanCard.copy(isSyncedWithGoogleContacts = true)
                    } else {
                        _selectedCard.value = cleanCard
                    }
                } else {
                    _selectedCard.value = cleanCard
                }

                // Cloud Backup to Firebase Firestore on update
                if (CloudBackupManager.isAutoBackupEnabled(appCtx) && AuthManager.isUserSignedIn) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val backedUp = CloudBackupManager.backupSingleCard(cleanCard)
                        if (backedUp) {
                            repository.setCloudBackupStatus(cleanCard.id, true)
                        }
                    }
                }

                CardMateWidgetProvider.notifyDataChanged(getApplication())
                onSaved(cleanCard.id)
            }
        }
    }

    fun deleteCard(card: BusinessCard) {
        viewModelScope.launch {
            repository.deleteCard(card)
            
            // Delete from Firestore Cloud Vault if signed in
            val user = AuthManager.currentUser
            if (user != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    FirestoreManager.deleteCardFromCloud(user.uid, card.id)
                }
            }

            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = null
            }
            CardMateWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    fun toggleFavorite(card: BusinessCard) {
        viewModelScope.launch {
            val newFav = !card.isFavorite
            repository.toggleFavorite(card.id, newFav)
            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = card.copy(isFavorite = newFav)
            }
            CardMateWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    /**
     * Updates card notes and persists directly into local Room database and cloud backup.
     */
    fun updateCardNotes(
        card: BusinessCard,
        notes: String,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val updated = card.copy(notes = notes, updatedAt = System.currentTimeMillis())
            repository.updateCard(updated)
            _selectedCard.value = updated
            CardMateWidgetProvider.notifyDataChanged(getApplication())

            val appCtx = getApplication<Application>()
            if (CloudBackupManager.isAutoBackupEnabled(appCtx) && AuthManager.isUserSignedIn) {
                viewModelScope.launch(Dispatchers.IO) {
                    CloudBackupManager.backupSingleCard(updated)
                }
            }

            Toast.makeText(
                appCtx,
                if (_isBanglaLanguage.value) "নোট সংরক্ষিত হয়েছে" else "Note saved successfully",
                Toast.LENGTH_SHORT
            ).show()
            onSaved()
        }
    }

    /**
     * Updates card design styling (colors, fonts, layout style, pattern, corner radius, borders, badges)
     * and persists directly into local Room SQLite database.
     */
    fun updateCardDesign(
        card: BusinessCard,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateCard(card)
            _selectedCard.value = card
            CardMateWidgetProvider.notifyDataChanged(getApplication())

            val appCtx = getApplication<Application>()
            if (CloudBackupManager.isAutoBackupEnabled(appCtx) && AuthManager.isUserSignedIn) {
                viewModelScope.launch(Dispatchers.IO) {
                    CloudBackupManager.backupSingleCard(card)
                }
            }

            Toast.makeText(
                appCtx,
                if (_isBanglaLanguage.value) "কার্ড ডিজাইন সফলভাবে সংরক্ষিত হয়েছে" else "Card design saved to local database",
                Toast.LENGTH_SHORT
            ).show()

            onSaved()
        }
    }

    /**
     * Saves a reusable custom design preset to Room Database
     */
    fun saveCustomDesignPreset(
        name: String,
        primaryBg: Long,
        secondaryBg: Long,
        accent: Long,
        text: Long,
        fontFamily: String,
        layoutStyle: String,
        bgPattern: String,
        cornerRadiusDp: Int,
        borderStyle: String,
        textAlignment: String,
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val preset = CardDesignPreset(
                name = name,
                primaryBgColor = primaryBg,
                secondaryBgColor = secondaryBg,
                accentColor = accent,
                textColor = text,
                fontFamilyType = fontFamily,
                layoutStyle = layoutStyle,
                bgPattern = bgPattern,
                cornerRadiusDp = cornerRadiusDp,
                borderStyle = borderStyle,
                textAlignment = textAlignment,
                isUserCreated = true
            )
            val newId = repository.saveDesignPreset(preset)
            Toast.makeText(
                getApplication(),
                if (_isBanglaLanguage.value) "কাস্টম থিম প্রিসেট সেভ হয়েছে" else "Custom design theme saved",
                Toast.LENGTH_SHORT
            ).show()
            onSaved(newId)
        }
    }

    /**
     * Deletes a custom design preset from Room Database
     */
    fun deleteCustomDesignPreset(presetId: Long) {
        viewModelScope.launch {
            repository.deleteDesignPreset(presetId)
            Toast.makeText(
                getApplication(),
                if (_isBanglaLanguage.value) "থিম মুছে ফেলা হয়েছে" else "Theme preset deleted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Scanner actions
    fun setScanMode(mode: ScanMode) {
        _scannerState.value = _scannerState.value.copy(
            scanMode = mode,
            currentStep = ScanStep.FRONT,
            detectionState = CardDetectionState.SEARCHING,
            frontBitmap = null,
            backBitmap = null,
            frontOriginalUri = null,
            backOriginalUri = null,
            frontImageUri = null,
            backImageUri = null,
            frontQualityScore = null,
            backQualityScore = null,
            hasQualityWarning = false,
            qualityWarningMessage = null,
            scanResult = null,
            errorMessage = null
        )
    }

    fun setAutoCaptureEnabled(enabled: Boolean) {
        _scannerState.value = _scannerState.value.copy(isAutoCaptureEnabled = enabled)
    }

    fun updateDetectionFrame(frame: DetectedCardFrame) {
        val current = _scannerState.value
        // Don't override state if currently capturing or processing AI
        val newState = if (current.isProcessingAi || current.detectionState == CardDetectionState.CAPTURING || current.detectionState == CardDetectionState.PROCESSING) {
            current.detectionState
        } else {
            frame.detectionState
        }

        val progress = (frame.stableFrameCount.toFloat() / SmartCardDetectionEngine.REQUIRED_STABLE_FRAMES.toFloat()).coerceIn(0f, 1f)

        _scannerState.value = current.copy(
            detectionState = newState,
            detectedCorners = frame.corners,
            detectionConfidence = frame.confidence,
            isStable = frame.isStable,
            stableProgress = progress,
            hasGlare = frame.hasGlare,
            isBlurry = frame.isBlurry,
            aspectRatio = frame.estimatedAspectRatio
        )
    }

    fun setScanStep(step: ScanStep) {
        _scannerState.value = _scannerState.value.copy(
            currentStep = step,
            detectionState = CardDetectionState.SEARCHING,
            stableProgress = 0f,
            isStable = false
        )
    }

    fun dismissQualityWarning() {
        _scannerState.value = _scannerState.value.copy(hasQualityWarning = false, qualityWarningMessage = null)
    }

    fun updateScannerAspectRatio(widthPx: Int, heightPx: Int) {
        val (wMm, hMm, name) = OcrParserHelper.estimateCardDimensions(widthPx, heightPx)
        val ratio = if (heightPx > 0) widthPx.toFloat() / heightPx.toFloat() else 1.75f
        _scannerState.value = _scannerState.value.copy(
            detectedWidthMm = wMm,
            detectedHeightMm = hMm,
            detectedSizeStandard = name,
            aspectRatio = ratio
        )
    }

    fun saveBitmapToStorage(context: Context, bitmap: Bitmap, prefix: String): String {
        return try {
            val dir = java.io.File(context.filesDir, "card_scans")
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun handleCapturedImage(
        context: Context,
        rawBitmap: Bitmap,
        onFrontCapturedForDual: () -> Unit,
        onReadyToReview: () -> Unit
    ) {
        viewModelScope.launch {
            _scannerState.value = _scannerState.value.copy(
                detectionState = CardDetectionState.PROCESSING,
                progressMessage = if (_isBanglaLanguage.value) "কার্ড স্বয়ংক্রিয়ভাবে ক্রপ ও পার্সপেক্টিভ ঠিক করা হচ্ছে..." else "Auto-cropping & applying perspective correction..."
            )

            withContext(Dispatchers.Default) {
                val currentState = _scannerState.value
                val isFront = currentState.currentStep == ScanStep.FRONT
                val isAdaptive = currentState.isAdaptiveEnhancementEnabled

                // 1. Save Original raw capture for non-destructive history & manual adjust crop
                val rawPath = saveBitmapToStorage(context, rawBitmap, if (isFront) "original_front" else "original_back")

                // 2. Auto Detect Card Corners on captured bitmap preserving proper card aspect ratio
                val targetRatio = if (currentState.aspectRatio in 0.6f..2.5f) currentState.aspectRatio else 1.75f
                val detectedCorners = CardImageProcessor.autoDetectCardCorners(rawBitmap, targetRatio)

                // 3. Perspective Correction (Homography) - Preserve true card aspect ratio without squashing or stretching
                val correctedBitmap = CardImageProcessor.applyPerspectiveCorrection(
                    source = rawBitmap,
                    quad = detectedCorners,
                    targetAspectRatio = targetRatio
                )

                // 4. Analyze Image Quality Score
                val qualityScore = CardImageProcessor.analyzeCardQuality(correctedBitmap)

                // 5. Dynamic Pre-OCR Pipeline (Adaptive Brightness, Contrast & Sharpness Adjustment)
                val ocrOptimizedBitmap = if (isAdaptive) {
                    CardImageProcessor.preprocessCardForOcr(correctedBitmap, qualityScore)
                } else {
                    correctedBitmap
                }
                val appliedSummary = if (isAdaptive) qualityScore.ocrEnhanceSummary else "Raw Exposure (Adaptive Enhancements Disabled)"
                val isDynamicAdjustActive = isAdaptive && (qualityScore.isLowLight || qualityScore.isPoorContrast || qualityScore.isOverexposed || qualityScore.sharpnessScore < 65)

                // 6. Save perspective-corrected cropped card
                val processedPath = saveBitmapToStorage(context, correctedBitmap, if (isFront) "card_front" else "card_back")

                val hasWarning = qualityScore.overallScore < 52 || qualityScore.sharpnessScore < 45 || qualityScore.lightingScore < 40
                val warningMsg = if (hasWarning) {
                    if (_isBanglaLanguage.value) "ছবির মান কিছুটা দুর্বল বা ঝাপসা। প্রয়োজনে পুনরায় ছবি তুলুন।" else "Card photo might be blurry or dim. Retake or Adjust Crop if needed."
                } else null

                withContext(Dispatchers.Main) {
                    if (currentState.scanMode == ScanMode.SINGLE_SIDE) {
                        // Single side mode
                        _scannerState.value = _scannerState.value.copy(
                            frontBitmap = correctedBitmap,
                            capturedBitmap = correctedBitmap,
                            frontOriginalUri = rawPath,
                            frontImageUri = processedPath,
                            frontQualityScore = qualityScore,
                            lastAppliedAdaptiveEnhancement = appliedSummary,
                            isAdaptiveAdjustmentActive = isDynamicAdjustActive,
                            hasQualityWarning = hasWarning,
                            qualityWarningMessage = warningMsg,
                            isPerspectiveCorrected = true,
                            detectionState = CardDetectionState.READY_FOR_REVIEW
                        )
                        processSingleImageScan(context, ocrOptimizedBitmap)
                        onReadyToReview()
                    } else {
                        // Dual side mode
                        if (isFront) {
                            _scannerState.value = _scannerState.value.copy(
                                frontBitmap = correctedBitmap,
                                capturedBitmap = correctedBitmap,
                                frontOriginalUri = rawPath,
                                frontImageUri = processedPath,
                                frontQualityScore = qualityScore,
                                lastAppliedAdaptiveEnhancement = appliedSummary,
                                isAdaptiveAdjustmentActive = isDynamicAdjustActive,
                                hasQualityWarning = hasWarning,
                                qualityWarningMessage = warningMsg,
                                isPerspectiveCorrected = true,
                                currentStep = ScanStep.BACK,
                                detectionState = CardDetectionState.SEARCHING
                            )
                            onFrontCapturedForDual()
                        } else {
                            val frontBmp = currentState.frontBitmap ?: correctedBitmap
                            val frontOcrBmp = if (isAdaptive) CardImageProcessor.preprocessCardForOcr(frontBmp, currentState.frontQualityScore) else frontBmp

                            _scannerState.value = _scannerState.value.copy(
                                backBitmap = correctedBitmap,
                                backOriginalUri = rawPath,
                                backImageUri = processedPath,
                                backQualityScore = qualityScore,
                                lastAppliedAdaptiveEnhancement = appliedSummary,
                                isAdaptiveAdjustmentActive = isDynamicAdjustActive,
                                hasQualityWarning = hasWarning,
                                qualityWarningMessage = warningMsg,
                                isPerspectiveCorrected = true,
                                detectionState = CardDetectionState.READY_FOR_REVIEW
                            )
                            processDualImageScan(context, frontOcrBmp, ocrOptimizedBitmap)
                            onReadyToReview()
                        }
                    }
                }
            }
        }
    }

    fun processSingleImageScan(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            _scannerState.value = _scannerState.value.copy(
                isProcessingAi = true,
                capturedBitmap = bitmap,
                progressMessage = if (_isBanglaLanguage.value) "এআই কার্ড বিশ্লেষণ করছে..." else "AI analyzing business card...",
                errorMessage = null
            )

            try {
                val result = GeminiCardScannerService.extractCardFromBitmap(bitmap)
                _scannerState.value = _scannerState.value.copy(
                    isProcessingAi = false,
                    scanResult = result,
                    progressMessage = "",
                    detectionState = CardDetectionState.READY_FOR_REVIEW
                )
            } catch (e: Exception) {
                _scannerState.value = _scannerState.value.copy(
                    isProcessingAi = false,
                    errorMessage = e.localizedMessage ?: "Failed to scan card"
                )
            }
        }
    }

    fun processDualImageScan(context: Context, frontBitmap: Bitmap, backBitmap: Bitmap) {
        viewModelScope.launch {
            _scannerState.value = _scannerState.value.copy(
                isProcessingAi = true,
                progressMessage = if (_isBanglaLanguage.value) "এআই উভয় পাশ (Front + Back) বিশ্লেষণ ও মার্জ করছে..." else "AI fusing Front & Back details with Gemini...",
                errorMessage = null
            )

            try {
                val result = GeminiCardScannerService.extractDualSidedCard(frontBitmap, backBitmap)
                _scannerState.value = _scannerState.value.copy(
                    isProcessingAi = false,
                    scanResult = result,
                    progressMessage = "",
                    detectionState = CardDetectionState.READY_FOR_REVIEW
                )
            } catch (e: Exception) {
                _scannerState.value = _scannerState.value.copy(
                    isProcessingAi = false,
                    errorMessage = e.localizedMessage ?: "Failed to analyze dual-sided card"
                )
            }
        }
    }

    fun skipBackSideAndProcess(context: Context, onReadyToReview: () -> Unit) {
        val frontBmp = _scannerState.value.frontBitmap
        if (frontBmp != null) {
            processSingleImageScan(context, frontBmp)
            onReadyToReview()
        }
    }

    fun retakeFrontSide() {
        _scannerState.value = _scannerState.value.copy(
            currentStep = ScanStep.FRONT,
            frontBitmap = null,
            frontImageUri = null,
            scanResult = null,
            errorMessage = null
        )
    }

    fun retakeBackSide() {
        _scannerState.value = _scannerState.value.copy(
            currentStep = ScanStep.BACK,
            backBitmap = null,
            backImageUri = null,
            scanResult = null,
            errorMessage = null
        )
    }

    fun applyAdjustedCropToScanResult(
        context: Context,
        isFront: Boolean,
        newImagePath: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val newBitmap = withContext(Dispatchers.IO) {
                    CardImageProcessor.loadBitmapSafely(context, newImagePath, maxDimension = 2600)
                } ?: return@launch

                val newQuality = CardImageProcessor.analyzeCardQuality(newBitmap)
                val ocrOptimized = CardImageProcessor.preprocessCardForOcr(newBitmap, newQuality)

                if (isFront) {
                    _scannerState.value = _scannerState.value.copy(
                        frontBitmap = newBitmap,
                        frontImageUri = newImagePath,
                        frontQualityScore = newQuality,
                        hasQualityWarning = false,
                        qualityWarningMessage = null
                    )
                    val backBmp = _scannerState.value.backBitmap
                    if (backBmp != null) {
                        val backOcr = CardImageProcessor.preprocessCardForOcr(backBmp, _scannerState.value.backQualityScore)
                        processDualImageScan(context, ocrOptimized, backOcr)
                    } else {
                        processSingleImageScan(context, ocrOptimized)
                    }
                } else {
                    _scannerState.value = _scannerState.value.copy(
                        backBitmap = newBitmap,
                        backImageUri = newImagePath,
                        backQualityScore = newQuality,
                        hasQualityWarning = false,
                        qualityWarningMessage = null
                    )
                    val frontBmp = _scannerState.value.frontBitmap
                    if (frontBmp != null) {
                        val frontOcr = CardImageProcessor.preprocessCardForOcr(frontBmp, _scannerState.value.frontQualityScore)
                        processDualImageScan(context, frontOcr, ocrOptimized)
                    } else {
                        processSingleImageScan(context, ocrOptimized)
                    }
                }
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun processImageForScan(context: Context, bitmap: Bitmap) {
        handleCapturedImage(
            context = context,
            rawBitmap = bitmap,
            onFrontCapturedForDual = { /* Transitioned to BACK step */ },
            onReadyToReview = { /* Ready */ }
        )
    }

    fun processImageUri(
        context: Context,
        uri: Uri,
        onFrontCapturedForDual: () -> Unit = {},
        onReadyToReview: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                if (bitmap != null) {
                    handleCapturedImage(
                        context = context,
                        rawBitmap = bitmap,
                        onFrontCapturedForDual = onFrontCapturedForDual,
                        onReadyToReview = onReadyToReview
                    )
                }
            } catch (e: Exception) {
                _scannerState.value = _scannerState.value.copy(
                    errorMessage = "Error opening selected image: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearScanner() {
        _scannerState.value = ScannerUiState()
    }

    /**
     * Re-runs Gemini AI OCR & extraction on an edited card image and merges updated information
     * into the existing BusinessCard in Room database.
     */
    fun reRunOcrOnEditedCardImage(
        cardId: Long,
        imagePath: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val bitmap = withContext(Dispatchers.IO) {
                    CardImageProcessor.loadBitmapSafely(context, imagePath, maxDimension = 2600)
                }

                if (bitmap == null) {
                    onComplete(false, if (_isBanglaLanguage.value) "ছবি লোড করা যায়নি" else "Failed to load image")
                    return@launch
                }

                val scanResult = GeminiCardScannerService.extractCardFromBitmap(bitmap)
                val existingCard = repository.getCardById(cardId)

                if (existingCard != null) {
                    val updatedCard = sanitizeBusinessCard(
                        existingCard.copy(
                            fullName = if (scanResult.fullName.isNotBlank()) scanResult.fullName else existingCard.fullName,
                            jobTitle = if (scanResult.jobTitle.isNotBlank()) scanResult.jobTitle else existingCard.jobTitle,
                            company = if (scanResult.company.isNotBlank()) scanResult.company else existingCard.company,
                            phone = if (scanResult.phone.isNotBlank()) scanResult.phone else existingCard.phone,
                            secondaryPhone = if (scanResult.secondaryPhone.isNotBlank()) scanResult.secondaryPhone else existingCard.secondaryPhone,
                            email = if (scanResult.email.isNotBlank()) scanResult.email else existingCard.email,
                            website = if (scanResult.website.isNotBlank()) scanResult.website else existingCard.website,
                            address = if (scanResult.address.isNotBlank()) scanResult.address else existingCard.address,
                            notes = if (scanResult.rawOcrText.isNotBlank()) scanResult.rawOcrText else existingCard.notes,
                            customPrimaryBgColor = scanResult.detectedPrimaryBgColor ?: existingCard.customPrimaryBgColor,
                            customSecondaryBgColor = scanResult.detectedSecondaryBgColor ?: existingCard.customSecondaryBgColor,
                            customAccentColor = scanResult.detectedAccentColor ?: existingCard.customAccentColor,
                            customTextColor = scanResult.detectedTextColor ?: existingCard.customTextColor,
                            cardLayoutTemplate = if (scanResult.detectedTemplate.isNotBlank()) scanResult.detectedTemplate else existingCard.cardLayoutTemplate,
                            layoutStyle = if (scanResult.detectedLayoutStyle.isNotBlank()) scanResult.detectedLayoutStyle else existingCard.layoutStyle,
                            bgPattern = if (scanResult.detectedBgPattern.isNotBlank()) scanResult.detectedBgPattern else existingCard.bgPattern,
                            isBackedUpToCloud = false,
                            updatedAt = System.currentTimeMillis()
                        )
                    )

                    repository.updateCard(updatedCard)
                    _selectedCard.value = updatedCard
                    CardMateWidgetProvider.notifyDataChanged(context)

                    onComplete(
                        true,
                        if (_isBanglaLanguage.value)
                            "এআই দ্বারা কার্ডের তথ্য সফলভাবে পুনরায় স্ক্যান ও আপডেট হয়েছে!"
                        else
                            "Card details successfully re-scanned and updated with Gemini AI!"
                    )
                } else {
                    onComplete(false, "Card not found")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "OCR extraction failed")
            }
        }
    }

    // Google Contacts Synchronization & People API v1 Integration
    val googleContactsSyncState: StateFlow<GoogleSyncState> = GoogleContactsSyncManager.syncState

    fun setGoogleAccountEmail(context: Context, email: String) {
        GoogleContactsSyncManager.setGoogleAccount(context, email)
    }

    fun setGoogleOAuthToken(context: Context, token: String) {
        GoogleContactsSyncManager.setOAuthToken(context, token)
    }

    fun openGoogleContactsWeb(context: Context, resourceName: String? = null) {
        GoogleContactsSyncManager.openGoogleContactsWeb(context, resourceName)
    }

    fun syncCardToGoogleContacts(context: Context, card: BusinessCard, onOutcome: ((GoogleSyncOutcome) -> Unit)? = null) {
        viewModelScope.launch {
            val outcome = GoogleContactsSyncManager.syncCardToGoogleContacts(context, card)
            if (outcome.isSuccess) {
                repository.setGoogleSyncStatus(card.id, true)
                if (_selectedCard.value?.id == card.id) {
                    _selectedCard.value = card.copy(isSyncedWithGoogleContacts = true)
                }
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "গুগল কন্টাক্টসে সফলভাবে সিঙ্ক হয়েছে (${outcome.method})!" else "Synced to Google Contacts (${outcome.method})!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Fallback to native insert intent
                ContactsSyncManager.openAddContactIntent(context, card)
                repository.setGoogleSyncStatus(card.id, true)
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "কন্টাক্ট এডিটরে ওপেন করা হয়েছে" else "Opening phone contact editor...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onOutcome?.invoke(outcome)
        }
    }

    fun syncAllCardsToGoogleContacts(context: Context, onComplete: ((GoogleContactsApiClient.GoogleBatchSyncReport) -> Unit)? = null) {
        viewModelScope.launch {
            val cards = allCards.value
            val targetCards = cards.filter { !it.isSyncedWithGoogleContacts }.ifEmpty { cards }
            val report = GoogleContactsSyncManager.syncMultipleCards(context, targetCards) { _, _ -> }
            
            targetCards.forEach { card ->
                repository.setGoogleSyncStatus(card.id, true)
            }

            Toast.makeText(
                context,
                if (_isBanglaLanguage.value) 
                    "${report.successfulCount}/${report.totalCards} টি কার্ড গুগল কন্টাক্টসে সিঙ্ক হয়েছে!"
                else 
                    "${report.successfulCount}/${report.totalCards} cards synced to Google Contacts!",
                Toast.LENGTH_SHORT
            ).show()
            onComplete?.invoke(report)
        }
    }

    // Cloud Backup & Restore via Firebase Firestore
    fun performCloudBackup(context: Context) {
        if (_isBackingUp.value) return
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupProgress.value = 0
            val cards = allCards.value

            val success = CloudBackupManager.executeCloudBackup(context, cards) { progress, status ->
                _backupProgress.value = progress
                _backupStatusText.value = status
            }

            if (success) {
                for (card in cards) {
                    repository.setCloudBackupStatus(card.id, true)
                }
                _cloudSyncState.value = CloudBackupManager.getSyncState(context)
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "${cards.size} টি কার্ড ফায়ারবেস ফায়ারস্টোর ক্লাউডে ব্যাকআপ হয়েছে!" else "${cards.size} cards backed up to Firebase Firestore Cloud!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "ক্লাউড ব্যাকআপ ব্যর্থ হয়েছে বা সাইন-ইন প্রয়োজন" else "Cloud backup failed. Check connection or sign in.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            _isBackingUp.value = false
        }
    }

    fun restoreFromCloud(context: Context) {
        if (_isBackingUp.value) return
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupProgress.value = 0
            _backupStatusText.value = if (_isBanglaLanguage.value) "ফায়ারবেস ক্লাউড থেকে ডাটা উদ্ধার করা হচ্ছে..." else "Connecting to Firebase Firestore..."

            val cloudCards = CloudBackupManager.executeCloudRestore(context) { progress, status ->
                _backupProgress.value = progress
                _backupStatusText.value = status
            }

            if (cloudCards.isNotEmpty()) {
                val currentCards = allCards.value
                val existingKeys = currentCards.map {
                    "${it.fullName.trim().lowercase()}_${it.phone.trim()}_${it.email.trim().lowercase()}"
                }.toSet()

                val toInsert = cloudCards.filter {
                    val key = "${it.fullName.trim().lowercase()}_${it.phone.trim()}_${it.email.trim().lowercase()}"
                    key !in existingKeys
                }

                if (toInsert.isNotEmpty()) {
                    repository.insertCards(toInsert)
                    CardMateWidgetProvider.notifyDataChanged(getApplication())
                }

                _cloudSyncState.value = CloudBackupManager.getSyncState(context)
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "${cloudCards.size} টি কার্ড ফায়ারস্টোর ক্লাউড থেকে রিস্টোর হয়েছে!" else "Successfully restored ${cloudCards.size} cards from Firestore Cloud!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "ক্লাউডে কোনো সংরক্ষিত কার্ড পাওয়া যায়নি" else "No cards found in cloud or sign in required",
                    Toast.LENGTH_SHORT
                ).show()
            }
            _isBackingUp.value = false
        }
    }

    fun backupSingleCardToFirestore(context: Context, card: BusinessCard) {
        viewModelScope.launch {
            if (!AuthManager.isUserSignedIn) {
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "ক্লাউড ব্যাকআপের জন্য প্রথমে সাইন ইন করুন" else "Please sign in to backup to Firestore Cloud",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val success = CloudBackupManager.backupSingleCard(card)
            if (success) {
                repository.setCloudBackupStatus(card.id, true)
                if (_selectedCard.value?.id == card.id) {
                    _selectedCard.value = card.copy(isBackedUpToCloud = true)
                }
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "কার্ডটি ফায়ারস্টোর ক্লাউডে ব্যাকআপ হয়েছে!" else "Card backed up to Firestore Cloud!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "ক্লাউড ব্যাকআপ ব্যর্থ হয়েছে" else "Failed to backup card to Firestore",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Data Exporting & Backup
    fun exportAllCardsAsVCard(context: Context) {
        val cards = allCards.value
        if (cards.isEmpty()) {
            Toast.makeText(
                context,
                if (_isBanglaLanguage.value) "এক্সপোর্ট করার জন্য কোনো সংরক্ষিত কার্ড নেই" else "No saved contacts to export",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        VCardExporter.shareMultiCardsAsVCardFile(context, cards)
    }

    fun exportAllCardsAsCsv(context: Context) {
        val cards = allCards.value
        if (cards.isEmpty()) {
            Toast.makeText(
                context,
                if (_isBanglaLanguage.value) "এক্সপোর্ট করার জন্য কোনো সংরক্ষিত কার্ড নেই" else "No saved contacts to export",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        VCardExporter.shareCsvFile(context, cards)
    }

    fun exportFullJsonBackup(context: Context) {
        val cards = allCards.value
        val profile = userProfile.value
        BackupRestoreHelper.shareFullJsonBackupFile(context, cards, profile)
    }

    // Data Importing & Restoration
    fun importContactsFromVCardUri(context: Context, uri: Uri, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    BackupRestoreHelper.readTextFromUri(context, uri)
                }
                val cards = BackupRestoreHelper.parseVCardString(text)
                if (cards.isNotEmpty()) {
                    repository.insertCards(cards)
                    CardMateWidgetProvider.notifyDataChanged(getApplication())
                    Toast.makeText(
                        context,
                        if (_isBanglaLanguage.value) "${cards.size} টি কন্টাক্ট vCard থেকে সফলভাবে ইমপোর্ট হয়েছে!" else "Successfully imported ${cards.size} contacts from vCard!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        if (_isBanglaLanguage.value) "ফাইলে কোনো বৈধ vCard কন্টাক্ট পাওয়া যায়নি" else "No valid vCard contacts found in selected file",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                onComplete(cards.size)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "vCard ফাইল পড়তে সমস্যা হয়েছে: ${e.localizedMessage}" else "Error reading vCard file: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                onComplete(0)
            }
        }
    }

    fun importContactsFromVCardText(context: Context, vcardText: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val cards = BackupRestoreHelper.parseVCardString(vcardText)
                if (cards.isNotEmpty()) {
                    repository.insertCards(cards)
                    CardMateWidgetProvider.notifyDataChanged(getApplication())
                    Toast.makeText(
                        context,
                        if (_isBanglaLanguage.value) "${cards.size} টি কন্টাক্ট সফলভাবে ইমপোর্ট হয়েছে!" else "Successfully imported ${cards.size} contacts!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        if (_isBanglaLanguage.value) "কোনো বৈধ vCard ফরম্যাট পাওয়া যায়নি" else "No valid vCard format detected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                onComplete(cards.size)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "Error parsing vCard: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete(0)
            }
        }
    }

    fun restoreFromJsonUri(context: Context, uri: Uri, restoreProfile: Boolean = true, onComplete: (Int, Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    BackupRestoreHelper.readTextFromUri(context, uri)
                }
                val (cards, profile) = BackupRestoreHelper.parseBackupJson(jsonString)
                if (cards.isNotEmpty()) {
                    repository.insertCards(cards)
                    CardMateWidgetProvider.notifyDataChanged(getApplication())
                }
                if (restoreProfile && profile != null) {
                    saveUserProfile(profile)
                }
                val count = cards.size
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "$count টি কার্ড ও ব্যাকআপ সফলভাবে রিস্টোর হয়েছে!" else "Successfully restored $count cards & profile from JSON backup!",
                    Toast.LENGTH_LONG
                ).show()
                onComplete(count, true)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "JSON ব্যাকআপ রিস্টোরে ত্রুটি: ${e.localizedMessage}" else "Error restoring JSON backup: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                onComplete(0, false)
            }
        }
    }

    fun restoreFromJsonText(context: Context, jsonText: String, restoreProfile: Boolean = true, onComplete: (Int, Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val (cards, profile) = BackupRestoreHelper.parseBackupJson(jsonText)
                if (cards.isNotEmpty()) {
                    repository.insertCards(cards)
                    CardMateWidgetProvider.notifyDataChanged(getApplication())
                }
                if (restoreProfile && profile != null) {
                    saveUserProfile(profile)
                }
                val count = cards.size
                Toast.makeText(
                    context,
                    if (_isBanglaLanguage.value) "$count টি কার্ড সফলভাবে রিস্টোর হয়েছে!" else "Successfully restored $count cards!",
                    Toast.LENGTH_LONG
                ).show()
                onComplete(count, true)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "JSON Parse error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete(0, false)
            }
        }
    }

    // ==========================================
    // NFC Tag Reading, Writing & Beam Operations
    // ==========================================

    fun refreshNfcStatus(context: Context) {
        val supported = NfcCardService.isNfcSupported(context)
        val enabled = NfcCardService.isNfcEnabled(context)
        _nfcUiState.value = _nfcUiState.value.copy(
            isNfcSupported = supported,
            isNfcEnabled = enabled
        )
    }

    fun setNfcMode(mode: NfcOperationMode, card: BusinessCard? = null) {
        _nfcUiState.value = _nfcUiState.value.copy(
            mode = mode,
            cardToWrite = card ?: _nfcUiState.value.cardToWrite,
            statusMessage = when (mode) {
                NfcOperationMode.READ_TAG -> if (_isBanglaLanguage.value) "এনএফসি কার্ড বা ফোন কাছাকাছি আনুন..." else "Hold an NFC Tag or Phone near your device..."
                NfcOperationMode.WRITE_TAG -> if (_isBanglaLanguage.value) "রাইট করার জন্য খালি এনএফসি ট্যাগ স্পর্শ করুন..." else "Tap a writable NFC Tag to write contact..."
                NfcOperationMode.FORMAT_TAG -> if (_isBanglaLanguage.value) "ট্যাগ মুছতে এনএফসি ট্যাগ স্পর্শ করুন..." else "Tap an NFC Tag to erase/format..."
                NfcOperationMode.P2P_BEAM -> if (_isBanglaLanguage.value) "ডিভাইস দুটি একসাথে স্পর্শ করান..." else "Tap two phones back-to-back..."
                NfcOperationMode.IDLE -> ""
            },
            isProcessing = false
        )
    }

    fun setCardToWrite(card: BusinessCard) {
        _nfcUiState.value = _nfcUiState.value.copy(
            cardToWrite = card,
            mode = NfcOperationMode.WRITE_TAG,
            statusMessage = if (_isBanglaLanguage.value) "ট্যাগ স্পর্শ করে '${card.fullName}' কার্ডটি রাইট করুন" else "Tap tag to write card for '${card.fullName}'"
        )
    }

    fun toggleIncludeAar(include: Boolean) {
        _nfcUiState.value = _nfcUiState.value.copy(includeAarInWrite = include)
    }

    fun processDiscoveredTag(tag: Tag, packageName: String, onCardRead: ((BusinessCard) -> Unit)? = null) {
        val currentState = _nfcUiState.value

        when (currentState.mode) {
            NfcOperationMode.WRITE_TAG -> {
                val card = currentState.cardToWrite ?: userProfile.value.toBusinessCard()
                _nfcUiState.value = currentState.copy(isProcessing = true, statusMessage = "Writing to NFC Tag...")
                viewModelScope.launch(Dispatchers.IO) {
                    val result = NfcCardService.writeCardToTag(
                        tag = tag,
                        card = card,
                        packageName = packageName,
                        includeAar = currentState.includeAarInWrite
                    )
                    withContext(Dispatchers.Main) {
                        _nfcUiState.value = _nfcUiState.value.copy(
                            isProcessing = false,
                            lastWriteResult = result,
                            statusMessage = when (result) {
                                is NfcWriteResult.Success -> if (_isBanglaLanguage.value) "✅ সফলভাবে ট্যাগে লেখা হয়েছে (${result.bytesWritten} bytes)" else "✅ Successfully written to ${result.tagType} (${result.bytesWritten} bytes)"
                                is NfcWriteResult.InsufficientSpace -> if (_isBanglaLanguage.value) "⚠️ ট্যাগে পর্যাপ্ত জায়গা নেই (${result.availableBytes}B < ${result.requiredBytes}B)" else "⚠️ Tag full: needs ${result.requiredBytes}B, has ${result.availableBytes}B"
                                is NfcWriteResult.TagReadOnly -> if (_isBanglaLanguage.value) "❌ এই ট্যাগটি শুধুমাত্র রিড-অনলি (Read-Only)" else "❌ This NFC tag is locked / Read-Only"
                                is NfcWriteResult.Error -> "❌ ${result.message}"
                            }
                        )
                    }
                }
            }
            NfcOperationMode.FORMAT_TAG -> {
                _nfcUiState.value = currentState.copy(isProcessing = true, statusMessage = "Erasing NFC Tag...")
                viewModelScope.launch(Dispatchers.IO) {
                    val result = NfcCardService.clearNfcTag(tag)
                    withContext(Dispatchers.Main) {
                        _nfcUiState.value = _nfcUiState.value.copy(
                            isProcessing = false,
                            lastWriteResult = result,
                            statusMessage = if (result is NfcWriteResult.Success) {
                                if (_isBanglaLanguage.value) "✅ এনএফসি ট্যাগ সফলভাবে মোছা হয়েছে" else "✅ NFC Tag cleared & reset successfully"
                            } else {
                                "❌ Failed to clear tag"
                            }
                        )
                    }
                }
            }
            else -> {
                // READ_TAG or P2P_BEAM
                _nfcUiState.value = currentState.copy(isProcessing = true, statusMessage = "Reading NFC Tag...")
                viewModelScope.launch(Dispatchers.IO) {
                    val readResult = NfcCardService.readCardFromTag(tag)
                    withContext(Dispatchers.Main) {
                        when (readResult) {
                            is NfcReadResult.Success -> {
                                _nfcUiState.value = _nfcUiState.value.copy(
                                    isProcessing = false,
                                    lastReadCard = readResult.card,
                                    lastReadTagInfo = readResult.tagInfo,
                                    statusMessage = if (_isBanglaLanguage.value) "✅ কার্ড পড়া সফল হয়েছে: ${readResult.card.fullName}" else "✅ Contact Read: ${readResult.card.fullName}"
                                )
                                onCardRead?.invoke(readResult.card)
                            }
                            is NfcReadResult.EmptyTag -> {
                                _nfcUiState.value = _nfcUiState.value.copy(
                                    isProcessing = false,
                                    lastReadCard = null,
                                    statusMessage = if (_isBanglaLanguage.value) "⚠️ ট্যাগটি খালি (Blank Tag)" else "⚠️ Blank NFC Tag (No NDEF records found)"
                                )
                            }
                            is NfcReadResult.Error -> {
                                _nfcUiState.value = _nfcUiState.value.copy(
                                    isProcessing = false,
                                    statusMessage = "❌ ${readResult.message}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun handleNfcIntent(intent: Intent, onCardSaved: (BusinessCard) -> Unit) {
        val card = NfcCardService.readCardFromIntent(intent) ?: return
        _nfcUiState.value = _nfcUiState.value.copy(
            lastReadCard = card,
            statusMessage = if (_isBanglaLanguage.value) "এনএফসি কার্ড পাওয়া গেছে: ${card.fullName}" else "NFC Card Received: ${card.fullName}"
        )
        saveCard(card) {
            onCardSaved(card)
        }
    }

    fun setSimulatedReadCard(card: BusinessCard) {
        _nfcUiState.value = _nfcUiState.value.copy(
            lastReadCard = card,
            lastReadTagInfo = NfcTagInfo(
                tagIdHex = "04:A2:88:1B:3E:70:80",
                technologies = listOf("Ndef", "NfcA", "MifareUltralight"),
                maxSizeBytes = 504,
                currentSizeBytes = 268,
                isWritable = true,
                tagType = "NXP NTAG215 (504B)"
            ),
            statusMessage = if (_isBanglaLanguage.value) "এনএফসি ট্যাগ সিমুলেশন সফল" else "NFC Tag Simulated Read"
        )
    }

    fun resetNfcReadResult() {
        _nfcUiState.value = _nfcUiState.value.copy(
            lastReadCard = null,
            lastReadTagInfo = null,
            lastWriteResult = null,
            statusMessage = ""
        )
    }

    // ==========================================
    // Multi-turn Gemini AI Chat & Google Maps Actions
    // ==========================================

    fun setChatPersona(persona: ChatPersona) {
        _activeChatPersona.value = persona
        _activeChatModel.value = persona.defaultModel
        if (persona.isMapsGroundingDefault) {
            _isMapsGroundingEnabled.value = true
        }
    }

    fun setChatModel(model: String) {
        _activeChatModel.value = model
    }

    fun toggleMapsGrounding(enabled: Boolean) {
        _isMapsGroundingEnabled.value = enabled
    }

    fun setChatInputDraft(draft: String) {
        _chatInputDraft.value = draft
    }

    fun prepareChatForCard(card: BusinessCard, isLocationScout: Boolean = false) {
        val appCtx = getApplication<Application>().applicationContext
        _activeChatCardId.value = card.id
        _activeChatCard.value = card

        // Load card-specific chat history
        val savedHistory = CardAiHistoryManager.getHistoryForCard(appCtx, card.id)
        _chatMessages.value = savedHistory

        if (isLocationScout) {
            val mapsPersona = GeminiChatService.ALL_PERSONAS.find { it.type == ChatPersonaType.MAPS_SCOUT }
                ?: GeminiChatService.ALL_PERSONAS[0]
            setChatPersona(mapsPersona)
            _isMapsGroundingEnabled.value = true
            val initialPrompt = if (_isBanglaLanguage.value) {
                "${card.fullName} (${card.company}) এর ঠিকানা '${card.address.ifBlank { card.company }}' এর কাছাকাছি ব্যবসায়িক মিটিং করার জন্য ভালো ক্যাফে বা শান্ত রেস্তোরাঁ খুঁজে দাও।"
            } else {
                "Find the best quiet business cafes or meeting spots near ${card.fullName}'s company/office at '${card.address.ifBlank { card.company }}' on Google Maps."
            }
            _chatInputDraft.value = initialPrompt
        } else {
            val conciergePersona = GeminiChatService.ALL_PERSONAS[0]
            setChatPersona(conciergePersona)
            val initialPrompt = if (_isBanglaLanguage.value) {
                "${card.fullName} (${card.jobTitle}, ${card.company}) এর সাথে ফলো-আপ করার জন্য একটি সুন্দর বার্তা এবং নেটওয়ার্কিং স্ট্র্যাটেজি দাও।"
            } else {
                "Help me prepare a high-impact follow-up and collaboration plan for ${card.fullName} (${card.jobTitle} at ${card.company})."
            }
            _chatInputDraft.value = initialPrompt
        }
        // Do NOT auto send! Only prepared into draft input field for user review and manual send!
    }

    fun prepareGeneralChat() {
        val appCtx = getApplication<Application>().applicationContext
        _activeChatCardId.value = 0L
        _activeChatCard.value = null
        val savedHistory = CardAiHistoryManager.getHistoryForCard(appCtx, 0L)
        _chatMessages.value = savedHistory
    }

    fun switchChatCard(card: BusinessCard?) {
        if (card != null) {
            prepareChatForCard(card)
        } else {
            prepareGeneralChat()
        }
    }

    fun clearChatHistory() {
        val appCtx = getApplication<Application>().applicationContext
        _chatMessages.value = emptyList()
        val cardId = _activeChatCardId.value ?: 0L
        CardAiHistoryManager.clearHistoryForCard(appCtx, cardId)
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isChatThinking.value) return

        val userMsg = ChatMessage(
            role = ChatRole.USER,
            text = trimmed,
            modelUsed = _activeChatModel.value,
            timestamp = System.currentTimeMillis()
        )

        val updatedHistory = _chatMessages.value + userMsg
        _chatMessages.value = updatedHistory
        _isChatThinking.value = true

        viewModelScope.launch {
            val responseMsg = GeminiChatService.sendChatTurn(
                conversationHistory = updatedHistory.dropLast(1),
                userMessage = trimmed,
                persona = _activeChatPersona.value,
                selectedModel = _activeChatModel.value,
                enableMapsGrounding = _isMapsGroundingEnabled.value,
                cardsContext = allCards.value,
                userProfile = _userProfile.value,
                isBangla = _isBanglaLanguage.value
            )

            val fullHistory = _chatMessages.value + responseMsg
            _chatMessages.value = fullHistory
            _isChatThinking.value = false

            // Persist the conversation history specifically for this card or general assistant
            val appCtx = getApplication<Application>().applicationContext
            val cardId = _activeChatCardId.value ?: 0L
            CardAiHistoryManager.saveHistoryForCard(appCtx, cardId, fullHistory)
        }
    }

    fun startChatForCard(card: BusinessCard, isLocationScout: Boolean = false) {
        prepareChatForCard(card, isLocationScout)
    }

    private fun sanitizeBusinessCard(card: BusinessCard): BusinessCard {
        fun clean(text: String?): String {
            if (text == null) return ""
            val trimmed = text.trim()
            return if (trimmed.equals("null", ignoreCase = true) ||
                trimmed.equals("n/a", ignoreCase = true) ||
                trimmed.equals("none", ignoreCase = true) ||
                trimmed.equals("nil", ignoreCase = true) ||
                trimmed.equals("undefined", ignoreCase = true)
            ) "" else trimmed
        }

        // Ensure multiple phone numbers are split across discrete fields and never saved joined together
        val allRawPhones = buildList {
            addAll(com.example.util.PhoneNumberUtils.splitPhoneNumbers(card.phone))
            addAll(com.example.util.PhoneNumberUtils.splitPhoneNumbers(card.secondaryPhone))
        }
        val distinctPhones = mutableListOf<String>()
        for (p in allRawPhones) {
            val norm = com.example.util.PhoneNumberUtils.normalizeForComparison(p)
            if (norm.isNotBlank() && distinctPhones.none { com.example.util.PhoneNumberUtils.normalizeForComparison(it) == norm }) {
                distinctPhones.add(p)
            }
        }

        val primaryPhone = clean(distinctPhones.firstOrNull() ?: card.phone)
        val secPhone = clean(distinctPhones.getOrNull(1) ?: "")
        val extraPhones = distinctPhones.drop(2)

        var finalSocialLinks = clean(card.socialLinks)
        if (extraPhones.isNotEmpty()) {
            val existingLinks = finalSocialLinks.split("\n", "|").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            extraPhones.forEachIndexed { idx, p ->
                val label = "বিকল্প ফোন ${idx + 3}: $p"
                if (!existingLinks.any { it.contains(p) }) {
                    existingLinks.add(label)
                }
            }
            finalSocialLinks = existingLinks.joinToString("\n")
        }

        return card.copy(
            fullName = clean(card.fullName),
            jobTitle = clean(card.jobTitle),
            company = clean(card.company),
            phone = primaryPhone,
            secondaryPhone = secPhone,
            email = clean(card.email),
            website = clean(card.website),
            address = clean(card.address),
            notes = clean(card.notes),
            socialLinks = finalSocialLinks,
            rawOcrText = clean(card.rawOcrText)
        )
    }
}

