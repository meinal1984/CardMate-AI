package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sync.AuthManager
import com.example.ui.components.GoogleContactsSyncDialog
import com.example.ui.navigation.BackNavigationService
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.CardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: CardViewModel,
    onNavigateToSignIn: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val cloudState by viewModel.cloudSyncState.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val backupStatusText by viewModel.backupStatusText.collectAsState()
    val currentUser by AuthManager.currentUserFlow.collectAsState()

    // Privacy & Sharing Toggles
    val isNfcSharingEnabled by viewModel.isNfcSharingEnabled.collectAsState()
    val isQrSharingEnabled by viewModel.isQrSharingEnabled.collectAsState()
    val isAutoSyncToPhoneContactsEnabled by viewModel.isAutoSyncToPhoneContactsEnabled.collectAsState()
    val googleContactsSyncState by viewModel.googleContactsSyncState.collectAsState()

    // Dialog States
    var showAboutDialog by remember { mutableStateOf(false) }
    var showVCardImportDialog by remember { mutableStateOf(false) }
    var showJsonRestoreDialog by remember { mutableStateOf(false) }
    var showGoogleContactsDialog by remember { mutableStateOf(false) }

    var vcardRawInput by remember { mutableStateOf("") }
    var jsonRawInput by remember { mutableStateOf("") }
    var restoreProfileWithJson by remember { mutableStateOf(true) }

    // File pickers for vCard and JSON files
    val vCardFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importContactsFromVCardUri(context, uri) { count ->
                if (count > 0) {
                    showVCardImportDialog = false
                    vcardRawInput = ""
                }
            }
        }
    }

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreFromJsonUri(context, uri, restoreProfileWithJson) { count, success ->
                if (success) {
                    showJsonRestoreDialog = false
                    jsonRawInput = ""
                }
            }
        }
    }

    val formattedLastBackup = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        .format(Date(cloudState.lastBackupTimestamp))

    val navService = remember { BackNavigationService.instance }

    // Intercept modal dismissals on back
    DisposableEffect(showVCardImportDialog) {
        if (showVCardImportDialog) {
            val unreg = navService.registerModal {
                showVCardImportDialog = false
                vcardRawInput = ""
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(showJsonRestoreDialog) {
        if (showJsonRestoreDialog) {
            val unreg = navService.registerModal {
                showJsonRestoreDialog = false
                jsonRawInput = ""
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(showAboutDialog) {
        if (showAboutDialog) {
            val unreg = navService.registerModal {
                showAboutDialog = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    // State for collapsible/expandable category menus
    var expandedSections by remember { mutableStateOf(setOf<String>()) }

    fun toggleSection(sectionKey: String) {
        expandedSections = if (expandedSections.contains(sectionKey)) {
            expandedSections - sectionKey
        } else {
            expandedSections + sectionKey
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(if (onBack != null) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Optional Back Bar when opened from top header
        if (onBack != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "সেটিংস ও ক্লাউড সিঙ্ক" else "Settings & Cloud Sync",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 1. App Theme & Appearance Section
        item {
            SettingsCategoryCard(
                title = if (isBangla) "অ্যাপের থিম ও ভাষা" else "Appearance & Language",
                subtitle = "${if (isDarkTheme) (if (isBangla) "গাঢ় মোড" else "Dark Theme") else (if (isBangla) "লাইট মোড" else "Light Theme")} • ${if (isBangla) "বাংলা ভাষা" else "English"}",
                icon = Icons.Default.Palette,
                iconColor = CardMateTealPrimary,
                isExpanded = expandedSections.contains("appearance"),
                onToggle = { toggleSection("appearance") },
                badgeText = if (isBangla) "বাংলা" else "EN",
                badgeColor = CardMateTealPrimary,
                modifier = Modifier.testTag("settings_cat_appearance")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CardMateTealPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "গাঢ় মোড (Dark Theme)" else "Dark Theme Mode",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isBangla) "রাতে স্বাচ্ছন্দ্যে ব্যবহারের জন্য অপ্টিমাইজড" else "Easy on the eyes for night usage",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF042F2E),
                                checkedTrackColor = CardMateTealPrimary
                            ),
                            modifier = Modifier.testTag("theme_toggle_switch")
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )

                    // Language Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CardMateCyanAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = CardMateCyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "অ্যাপের ভাষা (Language)" else "App Language",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isBangla) "বাংলা ও ইংরেজি উভয়েই সক্রিয়" else "Currently: English / বাংলা",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.toggleLanguage() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBangla) CardMateTealPrimary else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("language_toggle_btn")
                        ) {
                            Text(
                                text = if (isBangla) "বাংলা" else "English",
                                color = if (isBangla) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. User Account & Firebase Firestore Cloud Sync Section
        item {
            SettingsCategoryCard(
                title = if (isBangla) "ফায়ারবেস ক্লাউড ও অ্যাকাউন্ট" else "Firebase Firestore & Account",
                subtitle = if (currentUser != null)
                    (currentUser?.displayName ?: currentUser?.email ?: "Active")
                else
                    (if (isBangla) "গুগল সাইন-ইন করে ক্লাউড সিঙ্ক চালু করুন" else "Sign in to backup & sync across devices"),
                icon = Icons.Default.Cloud,
                iconColor = CardMateTealPrimary,
                isExpanded = expandedSections.contains("cloud_account"),
                onToggle = { toggleSection("cloud_account") },
                badgeText = if (currentUser != null) "LOGGED IN" else null,
                badgeColor = StatusSuccess,
                modifier = Modifier.testTag("settings_cat_cloud")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Account Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CardMateTealPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentUser != null) Icons.Default.VerifiedUser else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (currentUser != null)
                                        (currentUser?.displayName ?: currentUser?.email ?: "CardMate Cloud User")
                                    else
                                        (if (isBangla) "ফায়ারবেস ক্লাউড অ্যাকাউন্ট" else "Firebase Cloud Vault"),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (currentUser != null)
                                        (currentUser?.email ?: (if (isBangla) "ফায়ারস্টোর সিঙ্ক সক্রিয়" else "Firestore Sync Active"))
                                    else
                                        (if (isBangla) "গুগল সাইন-ইন করে সব ডিভাইসে কার্ড সিঙ্ক রাখুন" else "Sign in to backup & sync across devices"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToSignIn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentUser != null) MaterialTheme.colorScheme.surface
                                else CardMateTealPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = if (currentUser != null) androidx.compose.foundation.BorderStroke(1.dp, CardMateTealPrimary) else null
                        ) {
                            Text(
                                text = if (currentUser != null)
                                    (if (isBangla) "ম্যানেজ" else "Manage")
                                else
                                    (if (isBangla) "সাইন ইন" else "Sign In"),
                                color = if (currentUser != null) CardMateTealPrimary else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Auto-Backup to Firestore Cloud Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (cloudState.isAutoBackupEnabled) CardMateTealPrimary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (cloudState.isAutoBackupEnabled) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isBangla) "ক্লাউড অটো-ব্যাকআপ (Firestore)" else "Auto Cloud Backup",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (cloudState.isAutoBackupEnabled) StatusSuccess.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (cloudState.isAutoBackupEnabled) "ON" else "OFF",
                                            color = if (cloudState.isAutoBackupEnabled) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isBangla)
                                        "প্রতিটি নতুন বা এডিটেড কার্ড ফায়ারবেস ক্লাউডে স্বয়ংক্রিয় সেভ হবে"
                                    else
                                        "Real-time auto-backup to Firebase Firestore on new scan or edit",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = cloudState.isAutoBackupEnabled,
                            onCheckedChange = { viewModel.setAutoBackupEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF042F2E),
                                checkedTrackColor = CardMateTealPrimary
                            ),
                            modifier = Modifier.testTag("auto_cloud_backup_toggle")
                        )
                    }

                    // Progress Bar during backup/restore
                    if (isBackingUp) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { backupProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CardMateTealPrimary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = backupStatusText,
                                    color = CardMateTealPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$backupProgress%",
                                    color = CardMateTealPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Backup & Restore Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.performCloudBackup(context) },
                            enabled = !isBackingUp,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_to_firestore_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "ক্লাউডে ব্যাকআপ" else "Backup Now",
                                color = Color(0xFF042F2E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.restoreFromCloud(context) },
                            enabled = !isBackingUp,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_from_firestore_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = CardMateCyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "ক্লাউড থেকে রিস্টোর" else "Restore Cloud",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Cloud Vault Info & Sync Status
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "ক্লাউডে কার্ড: ${cloudState.totalCloudCards} টি" else "Cloud Vault: ${cloudState.totalCloudCards} cards",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isBangla) "লাস্ট ব্যাকআপ: $formattedLastBackup" else "Last sync: $formattedLastBackup",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Privacy and Sharing Section (NFC & QR Toggles)
        item {
            SettingsCategoryCard(
                title = if (isBangla) "প্রাইভেসি ও শেয়ারিং কন্ট্রোল" else "Privacy & Sharing",
                subtitle = if (isBangla)
                    "NFC: ${if (isNfcSharingEnabled) "অন" else "অফ"} • QR: ${if (isQrSharingEnabled) "অন" else "অফ"}"
                else
                    "NFC: ${if (isNfcSharingEnabled) "ON" else "OFF"} • QR: ${if (isQrSharingEnabled) "ON" else "OFF"}",
                icon = Icons.Default.Security,
                iconColor = CardMateTealPrimary,
                isExpanded = expandedSections.contains("privacy_sharing"),
                onToggle = { toggleSection("privacy_sharing") },
                badgeText = if (isNfcSharingEnabled && isQrSharingEnabled) "ACTIVE" else null,
                badgeColor = StatusSuccess,
                modifier = Modifier.testTag("settings_cat_privacy")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // NFC Sharing On/Off Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isNfcSharingEnabled) CardMateTealPrimary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = if (isNfcSharingEnabled) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isBangla) "NFC শেয়ারিং (NFC Sharing)" else "NFC Sharing",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isNfcSharingEnabled) StatusSuccess.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isNfcSharingEnabled) "ON" else "OFF",
                                            color = if (isNfcSharingEnabled) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isBangla)
                                        "এনএফসি ট্যাগে কার্ড রাইট এবং ডিভাইস বিমিং অনুমোদন করে"
                                    else
                                        "Allow contactless card beaming & writing to physical NFC tags",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Switch(
                            checked = isNfcSharingEnabled,
                            onCheckedChange = { viewModel.setNfcSharingEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF042F2E),
                                checkedTrackColor = CardMateTealPrimary
                            ),
                            modifier = Modifier.testTag("nfc_sharing_toggle")
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )

                    // QR Sharing On/Off Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isQrSharingEnabled) CardMateCyanAccent.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = if (isQrSharingEnabled) CardMateCyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isBangla) "QR শেয়ারিং (QR Sharing)" else "QR Sharing",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isQrSharingEnabled) StatusSuccess.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isQrSharingEnabled) "ON" else "OFF",
                                            color = if (isQrSharingEnabled) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isBangla)
                                        "অন্যদের সাথে শেয়ারের জন্য ডায়নামিক কিউআর কোড জেনারেশন সক্রিয় রাখে"
                                    else
                                        "Allow dynamic QR matrix generation & sharing for visiting cards",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Switch(
                            checked = isQrSharingEnabled,
                            onCheckedChange = { viewModel.setQrSharingEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF042F2E),
                                checkedTrackColor = CardMateCyanAccent
                            ),
                            modifier = Modifier.testTag("qr_sharing_toggle")
                        )
                    }
                }
            }
        }

        // 4. Google Contacts Synchronization & Auto-Sync
        item {
            SettingsCategoryCard(
                title = if (isBangla) "গুগল কন্টাক্টস API ও অটো-সিঙ্ক" else "Google Contacts API & Sync",
                subtitle = if (isAutoSyncToPhoneContactsEnabled)
                    (if (isBangla) "Google People API v1 • অটো-সিঙ্ক চালু" else "Google People API v1 • Auto-Sync Active")
                else
                    (if (isBangla) "Google People API v1 • ১-ট্যাপ সিঙ্ক" else "Google People API v1 • 1-Tap Sync"),
                icon = Icons.Default.CloudSync,
                iconColor = CardMateCyanAccent,
                isExpanded = expandedSections.contains("contacts_sync"),
                onToggle = { toggleSection("contacts_sync") },
                badgeText = if (isAutoSyncToPhoneContactsEnabled) "AUTO-SYNC" else "PEOPLE API",
                badgeColor = StatusSuccess,
                modifier = Modifier.testTag("settings_cat_phone_sync")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Connected Google Account Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBangla) "সংযুক্ত গুগল একাউন্ট (Google People API)" else "Connected Google Account",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StatusSuccess.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "OAuth Active",
                                        fontSize = 10.sp,
                                        color = StatusSuccess,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = googleContactsSyncState.googleAccountEmail.ifBlank { "mrinal.eee@gmail.com" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Scope: https://www.googleapis.com/auth/contacts",
                                fontSize = 10.sp,
                                color = CardMateCyanAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { showGoogleContactsDialog = true },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Text(if (isBangla) "কনফিগার / পরিবর্তন" else "Configure & Token", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // 1. Auto-Sync to Google Contacts Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isAutoSyncToPhoneContactsEnabled) CardMateTealPrimary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = if (isAutoSyncToPhoneContactsEnabled) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isBangla) "অটো গুগল সিঙ্ক (Auto-Sync)" else "Auto-Sync Scanned Cards",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isAutoSyncToPhoneContactsEnabled) StatusSuccess.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isAutoSyncToPhoneContactsEnabled) "ON" else "OFF",
                                            color = if (isAutoSyncToPhoneContactsEnabled) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isBangla)
                                        "নতুন স্ক্যান করা কার্ড সরাসরি গুগল কন্টাক্টসে (People API v1) স্বয়ংক্রিয়ভাবে সিঙ্ক হবে"
                                    else
                                        "Automatically push newly scanned business cards directly into Google Contacts",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Switch(
                            checked = isAutoSyncToPhoneContactsEnabled,
                            onCheckedChange = { viewModel.setAutoSyncToPhoneContacts(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF042F2E),
                                checkedTrackColor = CardMateTealPrimary
                            ),
                            modifier = Modifier.testTag("auto_sync_contacts_toggle")
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )

                    // 2. Manual 1-Tap Sync All to Google Contacts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "সকল কার্ড গুগল কন্টাক্টসে সিঙ্ক" else "1-Tap Sync All to Google",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isBangla) "সংরক্ষিত সমস্ত ভিজিটিং কার্ড Google People API দিয়ে সিঙ্ক হবে" else "Synchronize your entire digital card database directly into Google Contacts",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.syncAllCardsToGoogleContacts(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("sync_all_contacts_button")
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "সিঙ্ক করুন" else "Sync All", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // 3. Open Google Contacts Web Button
                    OutlinedButton(
                        onClick = { viewModel.openGoogleContactsWeb(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = CardMateCyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "গুগল কন্টাক্টস ওয়েব ওপেন করুন (contacts.google.com)" else "Open Google Contacts (contacts.google.com)",
                            color = CardMateCyanAccent,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 5. Export All Contacts & Full JSON Backup Section
        item {
            SettingsCategoryCard(
                title = if (isBangla) "ডাটা ব্যাকআপ ও এক্সপোর্ট" else "Data Backup & Export",
                subtitle = "vCard (.vcf) • CSV • JSON Backup",
                icon = Icons.Default.FileDownload,
                iconColor = CardMateGoldAccent,
                isExpanded = expandedSections.contains("data_export"),
                onToggle = { toggleSection("data_export") },
                modifier = Modifier.testTag("settings_cat_export")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla)
                            "আপনার সংরক্ষিত সমস্ত কার্ড আন্তর্জাতিক ফরম্যাটে এক্সপোর্ট বা ফুল ব্যাকআপ ফাইল তৈরি করুন:"
                        else
                            "Export your digital contact library in industry-standard formats or generate a full CardMate JSON backup:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // 1. Export All Contacts (vCard)
                    OutlinedButton(
                        onClick = { viewModel.exportAllCardsAsVCard(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_vcard_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = CardMateTealPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "Export All Contacts (vCard .vcf)" else "Export All Contacts (vCard)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Universal contact file for Google, Apple & Outlook",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. Export All Contacts (CSV)
                    OutlinedButton(
                        onClick = { viewModel.exportAllCardsAsCsv(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = CardMateGoldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "Export All Contacts (CSV / Excel)" else "Export All Contacts (CSV)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Spreadsheet table format for Excel, Google Sheets & CRMs",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 3. Full CardMate JSON Backup
                    Button(
                        onClick = { viewModel.exportFullJsonBackup(context) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_full_json_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "Full CardMate JSON Backup" else "Full CardMate JSON Backup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Complete archive with all card fields, notes & user profile",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 6. Import Contacts & Restore JSON Section
        item {
            SettingsCategoryCard(
                title = if (isBangla) "ডাটা ইমপোর্ট ও রিস্টোর" else "Import & Restore Contacts",
                subtitle = "vCard (.vcf) • JSON Database",
                icon = Icons.Default.FileUpload,
                iconColor = CardMateCyanAccent,
                isExpanded = expandedSections.contains("data_import"),
                onToggle = { toggleSection("data_import") },
                modifier = Modifier.testTag("settings_cat_import")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla)
                            "পূর্বে সেভ করা vCard বা CardMate JSON ব্যাকআপ থেকে কন্টাক্টগুলো অ্যাপে ফিরিয়ে আনুন:"
                        else
                            "Import contacts from external .vcf files or restore a complete CardMate JSON database backup:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // 1. Import Contacts (vCard)
                    OutlinedButton(
                        onClick = { showVCardImportDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_vcard_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = CardMateCyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "Import Contacts (vCard)" else "Import Contacts (vCard)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Load from .vcf file or paste standard vCard text",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. Restore JSON Backup
                    Button(
                        onClick = { showJsonRestoreDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_json_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "Restore JSON Backup" else "Restore JSON",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF042F2E)
                            )
                            Text(
                                text = "Restore cards and profile from CardMate .json backup file",
                                fontSize = 10.sp,
                                color = Color(0xFF042F2E).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 7. Home Screen Widget Info & Sync
        item {
            SettingsCategoryCard(
                title = if (isBangla) "হোম স্ক্রিন উইজেট" else "Home Screen Widget",
                subtitle = if (isBangla) "হোম স্ক্রিন থেকে সরাসরি দ্রুত স্ক্যান" else "Quick Scan & Widgets from Launcher",
                icon = Icons.Default.Widgets,
                iconColor = CardMateTealPrimary,
                isExpanded = expandedSections.contains("widget"),
                onToggle = { toggleSection("widget") },
                modifier = Modifier.testTag("settings_cat_widget")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla)
                            "• অ্যান্ড্রয়েড হোম স্ক্রিনে চেপে ধরে 'Widgets' থেকে 'CardMate AI' যোগ করুন।\n• এক ট্যাপে ক্যামেরা স্ক্যান চালু করুন অথবা ঘন ঘন ব্যবহৃত কার্ডগুলো স্ক্রিন থেকেই দেখুন।"
                        else
                            "• Long-press on your Android home screen and select Widgets → 'CardMate AI'.\n• Access instant 1-tap AI camera scanning and browse your most frequent business cards right on your launcher.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedButton(
                        onClick = {
                            com.example.widget.CardMateWidgetProvider.notifyDataChanged(context)
                            Toast.makeText(
                                context,
                                if (isBangla) "হোম স্ক্রিন উইজেটের ডাটা সিঙ্ক করা হয়েছে" else "Home Screen Widget data synchronized!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "উইজেট ডাটা রিফ্রেশ করুন" else "Force Refresh Widget Data",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 8. About CardMate AI Section
        item {
            SettingsCategoryCard(
                title = if (isBangla) "কার্ডমেট এআই পরিচিতি ও সাপোর্ট" else "About CardMate AI & Support",
                subtitle = "CardMate AI Pro • Version 1.2.0",
                icon = Icons.Default.Info,
                iconColor = CardMateTealPrimary,
                isExpanded = expandedSections.contains("about"),
                onToggle = { toggleSection("about") },
                badgeText = "v1.2.0",
                badgeColor = CardMateCyanAccent,
                modifier = Modifier.testTag("settings_cat_about")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isBangla)
                            "কার্ডমেট এআই হলো একটি আধুনিক ও শক্তিশালী অন-ডিভাইস বিজনেস কার্ড স্ক্যানার ও অর্গানাইজার। গুগল জেমিনাই এআই ভিশন এবং এনএফসি/কিউআর প্রযুক্তির সমন্বয়ে এটি দ্রুত ও নির্ভুলভাবে কার্ড সংরক্ষণ করে।"
                        else
                            "CardMate AI is a smart on-device business card scanner and digital networking hub powered by Google Gemini AI, offline Room database, NFC sharing, and instant QR generation.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showAboutDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF042F2E))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "অ্যাপ বিবরণ ও ক্রেডিট" else "View Full Details",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF042F2E)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:mrinal.eee@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "CardMate AI Feedback & Support")
                                    putExtra(Intent.EXTRA_TEXT, "Hello CardMate Team,\n\nHere is my feedback/inquiry:")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Email: mrinal.eee@gmail.com", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isBangla) "ফিডব্যাক পাঠান" else "Send Feedback",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 9. Developer Console & Navigation Debug Section
        item {
            SettingsCategoryCard(
                title = if (isBangla) "ডেভেলপার কনসোল ও ডিবাগ" else "Developer Console & Debug",
                subtitle = "Inspect Live Stack, Back Handler & Event Telemetry",
                icon = Icons.Default.BugReport,
                iconColor = CardMateGoldAccent,
                isExpanded = expandedSections.contains("developer"),
                onToggle = { toggleSection("developer") },
                modifier = Modifier.testTag("settings_cat_debug")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla)
                            "সেন্ট্রালাইজড ব্যাক হ্যান্ডলার, ন্যাভিগেশন স্ট্যাক ডেপথ এবং এজ-সোয়াইপ ইন্টারসেপশন স্টেট লাইভ পরীক্ষা করুন।"
                        else
                            "Test and inspect the centralized back navigation stack, active screen hierarchy, canGoBack state, unsaved changes guards, and back event history logs in real-time.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = {
                            navService.isDeveloperDebugOverlayVisible = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("open_nav_debug_btn")
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "নেভিগেশন ডিবাগ কনসোল খুলুন" else "Open Navigation Debug Console",
                            color = Color(0xFF042F2E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog: Import Contacts (vCard)
    if (showVCardImportDialog) {
        AlertDialog(
            onDismissRequest = { showVCardImportDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CardMateCyanAccent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = CardMateCyanAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isBangla) "vCard কন্টাক্ট ইমপোর্ট করুন" else "Import Contacts (vCard)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isBangla)
                            "আপনার ফোনের মেমোরি থেকে .vcf ফাইল নির্বাচন করুন অথবা নিচে সরাসরি vCard টেক্সট পেস্ট করুন:"
                        else
                            "Choose a .vcf contact file from your device storage or paste raw vCard text below:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            try {
                                vCardFilePickerLauncher.launch("*/*")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open file chooser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "ফাইল নির্বাচন করুন (.vcf)" else "Select .vcf File from Storage",
                            color = Color(0xFF042F2E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Text(
                            text = if (isBangla) " অথবা পেস্ট করুন " else " OR PASTE TEXT ",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    OutlinedTextField(
                        value = vcardRawInput,
                        onValueChange = { vcardRawInput = it },
                        placeholder = { Text("BEGIN:VCARD\nVERSION:3.0\nFN:John Doe\nTEL:+1234567890\nEND:VCARD", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 6,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vcardRawInput.isNotBlank()) {
                            viewModel.importContactsFromVCardText(context, vcardRawInput) { count ->
                                if (count > 0) {
                                    showVCardImportDialog = false
                                    vcardRawInput = ""
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please paste vCard text or select a file", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                    enabled = vcardRawInput.isNotBlank()
                ) {
                    Text(
                        text = if (isBangla) "ইমপোর্ট করুন" else "Import Pasted Text",
                        color = Color(0xFF042F2E),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showVCardImportDialog = false }) {
                    Text(text = if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Modal Dialog: Restore JSON Backup
    if (showJsonRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showJsonRestoreDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CardMateGoldAccent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = CardMateGoldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isBangla) "CardMate JSON ব্যাকআপ রিস্টোর" else "Restore JSON Backup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isBangla)
                            "পূর্বে সেভ করা CardMate JSON ব্যাকআপ ফাইল নির্বাচন করুন অথবা সরাসরি JSON ডাটা পেস্ট করুন:"
                        else
                            "Select your previously exported CardMate .json backup file or paste the JSON text below:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            try {
                                jsonFilePickerLauncher.launch("*/*")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open file chooser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "JSON ফাইল নির্বাচন করুন" else "Select .json File from Storage",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = restoreProfileWithJson,
                            onCheckedChange = { restoreProfileWithJson = it },
                            colors = CheckboxDefaults.colors(checkedColor = CardMateTealPrimary)
                        )
                        Text(
                            text = if (isBangla) "ইউজার প্রোফাইলও রিস্টোর করুন" else "Restore user profile if found in backup",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Text(
                            text = if (isBangla) " অথবা পেস্ট করুন " else " OR PASTE JSON ",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    OutlinedTextField(
                        value = jsonRawInput,
                        onValueChange = { jsonRawInput = it },
                        placeholder = { Text("{\n  \"app\": \"CardMate AI\",\n  \"cards\": [...]\n}", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 6,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonRawInput.isNotBlank()) {
                            viewModel.restoreFromJsonText(context, jsonRawInput, restoreProfileWithJson) { count, success ->
                                if (success) {
                                    showJsonRestoreDialog = false
                                    jsonRawInput = ""
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please paste JSON text or select a file", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                    enabled = jsonRawInput.isNotBlank()
                ) {
                    Text(
                        text = if (isBangla) "রিস্টোর করুন" else "Restore Pasted JSON",
                        color = Color(0xFF042F2E),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showJsonRestoreDialog = false }) {
                    Text(text = if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Google Contacts Synchronization Dialog
    if (showGoogleContactsDialog) {
        GoogleContactsSyncDialog(
            viewModel = viewModel,
            onDismiss = { showGoogleContactsDialog = false }
        )
    }

    // Comprehensive About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CardMateTealPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CardMate AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Version 1.2.0 • Build 2026.08",
                        fontSize = 11.sp,
                        color = CardMateCyanAccent
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isBangla) "প্রধান বৈশিষ্ট্য ও প্রযুক্তি:" else "Key Features & Architecture:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (isBangla)
                            "• AI OCR: গুগল জেমিনাই ৩.৫ ফ্ল্যাশ ভিশন মডেলের মাধ্যমে বাংলা ও ইংরেজি উভয় ভাষার ভিজিটিং কার্ডের নির্ভুল ফিল্ড ডিটেকশন।\n• 3D ডিজিটাল কার্ড: টাচ-ফ্লিপ সহ আকর্ষণীয় ডিজিটাল কার্ড ভিউ।\n• QR ও NFC: যেকোনো কার্ডের জন্য ডায়নামিক vCard 3.0 QR কোড ও NFC বিমিং।\n• সম্পূর্ণ অফলাইন প্রাইভেসি: সমস্ত কার্ড স্থানীয় Room SQLite ডাটাবেসে সুরক্ষিত।\n• সিঙ্ক ও ব্যাকআপ: গুগল কন্টাক্টস সিঙ্ক ও ক্লাউড এনক্রিপ্টেড ব্যাকআপ সুবিধা।\n• হোম উইজেট: হোম স্ক্রিন থেকে ১-ট্যাপ স্ক্যান ও ফেভারিট কার্ড অ্যাক্সেস。"
                        else
                            "• AI OCR: Multimodal vision OCR with Google Gemini 3.5 Flash for high-accuracy Bengali & English business card parsing.\n• 3D Digital Card: Realistic flippable interactive digital visiting cards.\n• QR & NFC Hub: Built-in vCard 3.0 dynamic QR code generation & NFC data beam.\n• On-Device Privacy: 100% local persistence with Android Room SQLite DB.\n• Google Contacts & Cloud: Bi-directional contact synchronization and exports.\n• Launcher Widget: Instant 1-tap quick scan and frequent cards right on home screen.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isBangla) "ডেভেলপার ও যোগাযোগ:" else "Developer & Contact:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBangla)
                            "Lead Developer: Mrinal Kanti Roy\nইমেইল: mrinal.eee@gmail.com\nমোবাইল: +8801719205945 | WhatsApp: @mrinal.eee\nBuilt with Google AI Studio & Jetpack Compose"
                        else
                            "Lead Developer: Mrinal Kanti Roy\nEmail: mrinal.eee@gmail.com\nMobile: +8801719205945 | WhatsApp: @mrinal.eee\nBuilt with Google AI Studio & Jetpack Compose",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isBangla) "লাইসেন্স ও ওপেন সোর্স লাইব্রেরি:" else "Open Source Licenses:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• Google ZXing (Apache 2.0 License)\n• AndroidX Jetpack & Room (Apache 2.0)\n• Coil Image Loader (Apache 2.0)\n• Material 3 Components (Apache 2.0)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary)
                ) {
                    Text(
                        text = if (isBangla) "ঠিক আছে" else "Close",
                        color = Color(0xFF042F2E),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

/**
 * Reusable expandable Settings Menu Card.
 * Submenus remain hidden by default and expand on click with animated visibility.
 */
@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = CardMateTealPrimary,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    badgeText: String? = null,
    badgeColor: Color = StatusSuccess,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Clickable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (badgeText != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = badgeColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = subtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = if (isExpanded) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expanded Submenu Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }
            }
        }
    }
}
