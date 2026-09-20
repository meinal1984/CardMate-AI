package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessCard
import com.example.sync.GoogleContactsApiClient
import com.example.sync.GoogleContactsSyncManager
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.CardViewModel
import kotlinx.coroutines.launch

@Composable
fun GoogleContactsSyncDialog(
    card: BusinessCard? = null,
    viewModel: CardViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val syncState by viewModel.googleContactsSyncState.collectAsState()

    var showTokenEdit by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf(syncState.oauthToken) }
    var isTestingToken by remember { mutableStateOf(false) }
    var tokenValidationResult by remember { mutableStateOf<Boolean?>(null) }
    var isSyncingNow by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    val availableAccounts = remember {
        GoogleContactsSyncManager.getAvailableGoogleAccounts(context)
    }

    AlertDialog(
        onDismissRequest = { if (!isSyncingNow) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CardMateTealPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isBangla) "গুগল কন্টাক্টস সিঙ্ক" else "Google Contacts Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Google People API v1",
                        fontSize = 11.5.sp,
                        color = CardMateCyanAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Account Information Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "সংযুক্ত গুগল একাউন্ট" else "Connected Google Account",
                                fontSize = 11.sp,
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
                            text = syncState.googleAccountEmail.ifBlank { "mrinal.eee@gmail.com" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBangla) "Scope: contacts (অনুমোদিত)" else "Scope: https://www.googleapis.com/auth/contacts",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // If multiple accounts available on device
                if (availableAccounts.size > 1) {
                    Text(
                        text = if (isBangla) "ডিভাইসের একাউন্ট পরিবর্তন করুন:" else "Switch Google Account:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableAccounts.forEach { acc ->
                            val isSelected = acc == syncState.googleAccountEmail
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CardMateTealPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CardMateTealPrimary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.setGoogleAccountEmail(context, acc)
                                    }
                            ) {
                                Text(
                                    text = acc.substringBefore("@"),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Card Specific details if opening for a card
                if (card != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardMateTealPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPage,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = card.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (card.company.isNotBlank()) "${card.jobTitle} • ${card.company}" else card.jobTitle.ifBlank { card.category },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // OAuth 2.0 Access Token Section (Collapsible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTokenEdit = !showTokenEdit },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = CardMateCyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "গুগল API এক্সেস টোকেন" else "Google API Access Token",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (showTokenEdit) (if (isBangla) "লুকান" else "Hide") else (if (isBangla) "সম্পাদনা" else "Configure"),
                        fontSize = 11.sp,
                        color = CardMateTealPrimary
                    )
                }

                if (showTokenEdit) {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            tokenValidationResult = null
                        },
                        label = { Text(if (isBangla) "OAuth 2.0 Access Token" else "OAuth 2.0 Access Token", fontSize = 11.sp) },
                        placeholder = { Text("ya29.a0A...", fontSize = 11.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setGoogleOAuthToken(context, tokenInput)
                                scope.launch {
                                    isTestingToken = true
                                    tokenValidationResult = GoogleContactsApiClient.testToken(tokenInput)
                                    isTestingToken = false
                                }
                            },
                            enabled = tokenInput.isNotBlank() && !isTestingToken,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingToken) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (isBangla) "টোকেন টেস্ট" else "Test & Save", fontSize = 11.sp)
                            }
                        }
                    }

                    tokenValidationResult?.let { valid ->
                        Text(
                            text = if (valid) "✓ Token is valid & authorized for Contacts API!" else "⚠ Token authorization check failed.",
                            fontSize = 11.sp,
                            color = if (valid) StatusSuccess else Color.Red
                        )
                    }
                }

                // Sync status message if available
                syncMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusSuccess.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            color = StatusSuccess,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary action: Sync this card (if card != null) or Sync All Cards
                if (card != null) {
                    Button(
                        onClick = {
                            isSyncingNow = true
                            viewModel.syncCardToGoogleContacts(context, card) { outcome ->
                                isSyncingNow = false
                                syncMessage = outcome.message
                            }
                        },
                        enabled = !isSyncingNow,
                        colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncingNow) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isBangla) "সিঙ্ক হচ্ছে..." else "Syncing to Google...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (card.isSyncedWithGoogleContacts) 
                                    (if (isBangla) "পুনরায় সিঙ্ক করুন" else "Re-sync to Google Contacts")
                                else 
                                    (if (isBangla) "গুগল কন্টাক্টসে সিঙ্ক করুন" else "Sync to Google Contacts"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }

                // Secondary option: Sync all cards
                OutlinedButton(
                    onClick = {
                        isSyncingNow = true
                        viewModel.syncAllCardsToGoogleContacts(context) { report ->
                            isSyncingNow = false
                            syncMessage = "${report.successfulCount} cards synced to Google Contacts!"
                        }
                    },
                    enabled = !isSyncingNow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "সকল কার্ড গুগল কন্টাক্টসে সিঙ্ক" else "Sync All Cards to Google Contacts",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Web viewer button
                TextButton(
                    onClick = {
                        viewModel.openGoogleContactsWeb(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = CardMateCyanAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla) "গুগল কন্টাক্টস ওপেন করুন (ওয়েব)" else "Open Google Contacts (contacts.google.com)",
                        fontSize = 12.sp,
                        color = CardMateCyanAccent
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSyncingNow
            ) {
                Text(if (isBangla) "বন্ধ করুন" else "Close")
            }
        }
    )
}
