package com.example.ui.card

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BusinessCard
import com.example.ui.ai.CardMateAiAssistantBottomSheet
import com.example.ui.components.CardShareBottomSheet
import com.example.ui.components.DigitalBusinessCardView
import com.example.ui.components.GoogleContactsSyncDialog
import com.example.ui.components.ZoomableCardImageView
import com.example.ui.navigation.BackNavigationService
import com.example.ui.qr.DynamicQrGeneratorDialog
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.CardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class NumberPickerAction {
    CALL, SMS, WHATSAPP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    viewModel: CardViewModel,
    card: BusinessCard,
    onBack: () -> Unit,
    onEdit: (BusinessCard) -> Unit,
    onCustomizeDesign: (BusinessCard) -> Unit = {},
    onEditImage: (side: String, imageUri: String?) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit = {},
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isBangla by viewModel.isBanglaLanguage.collectAsState()

    var isFlipped by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareBottomSheet by remember { mutableStateOf(false) }
    var showDynamicQrDialog by remember { mutableStateOf(false) }
    var showAiAssistantBottomSheet by remember { mutableStateOf(false) }
    var showScannedPhotosSheet by remember { mutableStateOf(false) }
    var assistantInitialTab by remember { mutableStateOf(0) }
    var isRunningOcrOnCard by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showGoogleContactsDialog by remember { mutableStateOf(false) }

    // Multi-number Selection Dialog & Note Dialog states
    var showNumberPickerDialog by remember { mutableStateOf(false) }
    var numberPickerAction by remember { mutableStateOf(NumberPickerAction.CALL) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var editingNoteText by remember { mutableStateOf("") }

    val navService = remember { BackNavigationService.instance }

    // Intercept modal dismissal on back gesture / back button
    DisposableEffect(showScannedPhotosSheet) {
        if (showScannedPhotosSheet) {
            val unreg = navService.registerModal {
                showScannedPhotosSheet = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(showAiAssistantBottomSheet) {
        if (showAiAssistantBottomSheet) {
            val unreg = navService.registerModal {
                showAiAssistantBottomSheet = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(showGoogleContactsDialog) {
        if (showGoogleContactsDialog) {
            val unreg = navService.registerModal {
                showGoogleContactsDialog = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(showShareBottomSheet) {
        if (showShareBottomSheet) {
            val unreg = navService.registerModal {
                showShareBottomSheet = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(showDeleteDialog) {
        if (showDeleteDialog) {
            val unreg = navService.registerModal {
                showDeleteDialog = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(showNumberPickerDialog) {
        if (showNumberPickerDialog) {
            val unreg = navService.registerModal {
                showNumberPickerDialog = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(showNoteDialog) {
        if (showNoteDialog) {
            val unreg = navService.registerModal {
                showNoteDialog = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    val allPhones = remember(card) { getCardPhoneNumbers(card, isBangla) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .testTag("card_detail_screen")
    ) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleFavorite(card) }) {
                        Icon(
                            imageVector = if (card.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (card.isFavorite) CardMateGoldAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { showDynamicQrDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Dynamic QR Code",
                            tint = CardMateCyanAccent
                        )
                    }

                    IconButton(onClick = { showShareBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Card",
                            tint = CardMateTealPrimary
                        )
                    }

                    // More Options (...) Menu containing Sync Options & All Settings
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.testTag("card_detail_more_options_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options & Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        // 1. Sync this card to Google Contacts
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = if (isBangla) "গুগল কন্টাক্টসে সিঙ্ক করুন" else "Sync to Google Contacts",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = if (isBangla) "Google People API v1 ও ক্লাউড সিঙ্ক" else "Google People API v1 & Cloud Address Book",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                showGoogleContactsDialog = true
                            }
                        )

                        // 2. Sync All Cards to Google Contacts
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = if (isBangla) "সকল কার্ড গুগল কন্টাক্টসে সিঙ্ক" else "Sync All to Google Contacts",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = if (isBangla) "সমস্ত সংরক্ষিত কার্ড সিঙ্ক হবে" else "Synchronize all saved cards to Google",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = CardMateCyanAccent
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                viewModel.syncAllCardsToGoogleContacts(context)
                            }
                        )

                        // 3. Cloud Vault & Firestore Backup
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = if (isBangla) "ক্লাউড ব্যাকআপ নিন" else "Cloud Vault Backup",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = if (isBangla) "ক্লাউডে ডাটা ব্যাকআপ সংরক্ষণ" else "Backup to secure Cloud Vault",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = StatusSuccess
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                viewModel.performCloudBackup(context)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // 4. Customize Design
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isBangla) "কার্ড ডিজাইন স্টুডিও" else "Customize Card Design",
                                    fontSize = 13.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onCustomizeDesign(card)
                            }
                        )

                        // 5. Edit Card
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isBangla) "কার্ডের তথ্য এডিট করুন" else "Edit Card Details",
                                    fontSize = 13.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = CardMateCyanAccent
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onEdit(card)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // 6. All Settings & Account
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = if (isBangla) "সকল সেটিঙ্কস ও ব্যাকআপ" else "All Settings & Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = CardMateTealPrimary
                                    )
                                    Text(
                                        text = if (isBangla) "থিম, ক্লাউড অ্যাকাউন্ট, এনএফসি ও এক্সপোর্ট" else "Theme, Cloud Account, NFC & Export",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onOpenSettings()
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // 7. Delete Card
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isBangla) "কার্ড মুছে ফেলুন" else "Delete Card",
                                    color = StatusError,
                                    fontSize = 13.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = StatusError
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Interactive 3D Card Preview with Slide Down to View Scanned Photos
            item {
                var cardDragOffsetY by remember { mutableStateOf(0f) }
                val hasScannedPhotos = !card.cardFrontImageUri.isNullOrBlank() || !card.cardBackImageUri.isNullOrBlank()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(hasScannedPhotos) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        if (dragAmount.y > 0 || cardDragOffsetY > 0) {
                                            cardDragOffsetY += dragAmount.y
                                            if (cardDragOffsetY > 45f) {
                                                change.consume()
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (cardDragOffsetY > 35f) {
                                            if (hasScannedPhotos) {
                                                showScannedPhotosSheet = true
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    if (isBangla) "কোনো স্ক্যানকৃত ছবি সংযুক্ত নেই" else "No scanned photos available",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        cardDragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        cardDragOffsetY = 0f
                                    }
                                )
                            }
                    ) {
                        DigitalBusinessCardView(
                            card = card,
                            isFlipped = isFlipped,
                            onFlipClick = { isFlipped = !isFlipped }
                        )
                    }

                    // Google Contacts Sync Status Badge
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (card.isSyncedWithGoogleContacts) StatusSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (card.isSyncedWithGoogleContacts) StatusSuccess.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showGoogleContactsDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (card.isSyncedWithGoogleContacts) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = if (card.isSyncedWithGoogleContacts) StatusSuccess else CardMateCyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (card.isSyncedWithGoogleContacts) {
                                    if (isBangla) "গুগল কন্টাক্টসে সিঙ্ক করা হয়েছে (Google People API)" else "Synced to Google Contacts (People API v1)"
                                } else {
                                    if (isBangla) "গুগল কন্টাক্টসে সিঙ্ক করুন" else "Sync to Google Contacts"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (card.isSyncedWithGoogleContacts) StatusSuccess else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (hasScannedPhotos) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showScannedPhotosSheet = true }
                                .testTag("card_detail_swipe_down_photos_hint")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "কার্ড নিচে স্লাইড করে আসল ছবি দেখুন" else "Swipe down to view scanned photos",
                                    fontSize = 11.sp,
                                    color = CardMateTealPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Interactive Contact Actions (Directly below the saved card, without header text)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Call Action
                        item {
                            ActionBigButton(
                                title = if (isBangla) "কল" else "Call",
                                icon = Icons.Default.Call,
                                color = CardMateTealPrimary,
                                onClick = {
                                    if (allPhones.size > 1) {
                                        numberPickerAction = NumberPickerAction.CALL
                                        showNumberPickerDialog = true
                                    } else if (allPhones.size == 1) {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${allPhones.first().second}")))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, allPhones.first().second, Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "কোনো ফোন নম্বর পাওয়া যায়নি" else "No phone number found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }

                        // SMS Action
                        item {
                            ActionBigButton(
                                title = if (isBangla) "এসএমএস" else "SMS",
                                icon = Icons.Default.Message,
                                color = CardMateCyanAccent,
                                onClick = {
                                    if (allPhones.size > 1) {
                                        numberPickerAction = NumberPickerAction.SMS
                                        showNumberPickerDialog = true
                                    } else if (allPhones.size == 1) {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${allPhones.first().second}")))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, allPhones.first().second, Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "কোনো ফোন নম্বর পাওয়া যায়নি" else "No phone number found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }

                        // WhatsApp Action
                        item {
                            ActionBigButton(
                                title = if (isBangla) "হোয়াটসঅ্যাপ" else "WhatsApp",
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                color = Color(0xFF25D366),
                                onClick = {
                                    if (allPhones.size > 1) {
                                        numberPickerAction = NumberPickerAction.WHATSAPP
                                        showNumberPickerDialog = true
                                    } else if (allPhones.size == 1) {
                                        openWhatsApp(
                                            context = context,
                                            rawPhone = allPhones.first().second,
                                            defaultMessage = if (isBangla) "হ্যালো ${card.fullName}," else "Hello ${card.fullName},"
                                        )
                                    } else {
                                        openWhatsApp(
                                            context = context,
                                            rawPhone = "",
                                            defaultMessage = if (isBangla) "হ্যালো ${card.fullName}," else "Hello ${card.fullName},"
                                        )
                                    }
                                }
                            )
                        }

                        // Email Action
                        item {
                            ActionBigButton(
                                title = if (isBangla) "ইমেইল" else "Email",
                                icon = Icons.Default.Email,
                                color = Color(0xFFA78BFA),
                                onClick = {
                                    if (card.email.isNotBlank()) {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${card.email}")))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, card.email, Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "কোনো ইমেইল পাওয়া যায়নি" else "No email found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }

                        // Address Action (Opens Google Maps)
                        item {
                            ActionBigButton(
                                title = if (isBangla) "ঠিকানা" else "Address",
                                icon = Icons.Default.LocationOn,
                                color = CardMateGoldAccent,
                                onClick = {
                                    if (card.address.isNotBlank()) {
                                        try {
                                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(card.address)}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            try {
                                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(card.address)}")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            } catch (e2: Exception) {
                                                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(card.address)}")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "কোনো ঠিকানা যুক্ত নেই" else "No address available",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }

                        // Note Action (Opens Note Viewer)
                        item {
                            ActionBigButton(
                                title = if (isBangla) "নোট" else "Note",
                                icon = Icons.Default.Description,
                                color = Color(0xFFF97316),
                                onClick = {
                                    if (card.notes.isNotBlank() || card.rawOcrText.isNotBlank()) {
                                        showNoteDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "কোনো নোট সংরক্ষিত নেই" else "No notes available",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }

                        // QR Code Action
                        item {
                            ActionBigButton(
                                title = if (isBangla) "কিউআর কোড" else "QR Studio",
                                icon = Icons.Default.QrCode2,
                                color = CardMateCyanAccent,
                                onClick = { showDynamicQrDialog = true }
                            )
                        }

                        // Share Action
                        item {
                            ActionBigButton(
                                title = if (isBangla) "শেয়ার" else "Share",
                                icon = Icons.Default.Share,
                                color = CardMateTealPrimary,
                                onClick = { showShareBottomSheet = true }
                            )
                        }

                        // Google Contacts Action
                        item {
                            ActionBigButton(
                                title = if (isBangla) "গুগল সিঙ্ক" else "Google Contacts",
                                icon = Icons.Default.CloudSync,
                                color = if (card.isSyncedWithGoogleContacts) StatusSuccess else CardMateCyanAccent,
                                onClick = { showGoogleContactsDialog = true }
                            )
                        }
                    }
                }
            }

            // 3. Detailed Interactive Contact Information Card (Header text omitted per user requirement)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Full Name & Identity
                        BasicInfoRow(
                            label = if (isBangla) "পূর্ণ নাম" else "Full Name",
                            value = card.fullName,
                            icon = Icons.Default.Person
                        )

                        if (card.jobTitle.isNotBlank()) {
                            BasicInfoRow(
                                label = if (isBangla) "পদবী" else "Job Title",
                                value = card.jobTitle,
                                icon = Icons.Default.Work
                            )
                        }

                        if (card.company.isNotBlank()) {
                            BasicInfoRow(
                                label = if (isBangla) "প্রতিষ্ঠান" else "Company",
                                value = card.company,
                                icon = Icons.Default.Business
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Interactive Primary Phone Number
                        if (card.phone.isNotBlank()) {
                            PhoneInteractiveRow(
                                label = if (isBangla) "মোবাইল" else "Mobile",
                                number = card.phone,
                                isBangla = isBangla,
                                context = context,
                                clipboardManager = clipboardManager
                            )
                        }

                        // Interactive Secondary Phone Numbers (renders each alternate number in its own row)
                        val secondaryPhones = card.getSecondaryPhoneNumbers()
                        secondaryPhones.forEachIndexed { index, secNum ->
                            val rowLabel = if (secondaryPhones.size == 1) {
                                if (isBangla) "বিকল্প মোবাইল / ফোন" else "Alt Phone"
                            } else {
                                if (isBangla) "বিকল্প ফোন নম্বর ${index + 2}" else "Alt Phone ${index + 2}"
                            }
                            PhoneInteractiveRow(
                                label = rowLabel,
                                number = secNum,
                                isBangla = isBangla,
                                context = context,
                                clipboardManager = clipboardManager
                            )
                        }

                        // Interactive Email
                        if (card.email.isNotBlank()) {
                            EmailInteractiveRow(
                                label = if (isBangla) "ইমেইল" else "Email",
                                email = card.email,
                                isBangla = isBangla,
                                context = context,
                                clipboardManager = clipboardManager
                            )
                        }

                        // Interactive Website
                        if (card.website.isNotBlank()) {
                            WebsiteInteractiveRow(
                                label = if (isBangla) "ওয়েবসাইট" else "Website",
                                url = card.website,
                                isBangla = isBangla,
                                context = context,
                                clipboardManager = clipboardManager
                            )
                        }

                        // Interactive Address (Clicking opens Google Maps)
                        if (card.address.isNotBlank()) {
                            AddressInteractiveRow(
                                label = if (isBangla) "ঠিকানা" else "Address",
                                address = card.address,
                                isBangla = isBangla,
                                context = context,
                                clipboardManager = clipboardManager
                            )
                        }

                        // Category & Dimensions
                        BasicInfoRow(
                            label = if (isBangla) "ক্যাটাগরি" else "Category",
                            value = card.category
                        )

                        BasicInfoRow(
                            label = if (isBangla) "কার্ডের মাপ" else "Card Dimensions",
                            value = "${String.format("%.1f", card.cardWidthMm)} × ${String.format("%.1f", card.cardHeightMm)} mm (${card.cardStandardName})"
                        )

                        // Interactive Social Links & Custom Fields
                        if (card.socialLinks.isNotBlank()) {
                            card.socialLinks.split("\n", "|").forEach { line ->
                                val trimmed = line.trim()
                                if (trimmed.contains(":")) {
                                    val parts = trimmed.split(":", limit = 2)
                                    val lbl = parts[0].trim()
                                    val valStr = parts[1].trim()
                                    if (valStr.isNotBlank()) {
                                        SocialLinkInteractiveRow(
                                            label = lbl,
                                            value = valStr,
                                            isBangla = isBangla,
                                            context = context,
                                            clipboardManager = clipboardManager
                                        )
                                    }
                                } else if (trimmed.isNotBlank()) {
                                    SocialLinkInteractiveRow(
                                        label = if (isBangla) "লিংক / তথ্য" else "Link / Info",
                                        value = trimmed,
                                        isBangla = isBangla,
                                        context = context,
                                        clipboardManager = clipboardManager
                                    )
                                }
                            }
                        }

                        // Interactive Notes
                        if (card.notes.isNotBlank()) {
                            NoteInteractiveRow(
                                label = if (isBangla) "নোট" else "Notes",
                                notes = card.notes,
                                isBangla = isBangla,
                                context = context,
                                clipboardManager = clipboardManager,
                                onClick = {
                                    editingNoteText = card.notes
                                    showNoteDialog = true
                                }
                            )
                        } else {
                            // Option to Add a New Note when empty
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        editingNoteText = ""
                                        showNoteDialog = true
                                    }
                                    .testTag("card_detail_add_note_btn")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF97316).copy(alpha = 0.18f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = Color(0xFFF97316),
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isBangla) "নোট যুক্ত করুন" else "Add Note",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (isBangla) "মিটিং বা প্রাসঙ্গিক তথ্য সংরক্ষণ করুন" else "Add meeting logs or personal notes",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF97316).copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Note",
                                                tint = Color(0xFFF97316),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Unified Floating Action Button: CardMate AI Assistant
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 20.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        ExtendedFloatingActionButton(
            onClick = {
                viewModel.startChatForCard(card, isLocationScout = false)
                assistantInitialTab = 0
                showAiAssistantBottomSheet = true
            },
            modifier = Modifier.testTag("card_detail_unified_ai_fab"),
            containerColor = CardMateTealPrimary,
            contentColor = Color(0xFF042F2E),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            ),
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "CardMate AI Assistant",
                    modifier = Modifier.size(20.dp)
                )
            },
            text = {
                Text(
                    text = if (isBangla) "CardMate AI Assistant" else "CardMate AI Assistant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }
        )
    }

    // Number Selection Dialog (Triggered when multiple numbers exist for Call, SMS, or WhatsApp)
    if (showNumberPickerDialog) {
        val actionTitle = when (numberPickerAction) {
            NumberPickerAction.CALL -> if (isBangla) "ডায়াল করার জন্য নম্বর নির্বাচন করুন" else "Select Number to Call"
            NumberPickerAction.SMS -> if (isBangla) "এসএমএস পাঠানোর জন্য নম্বর নির্বাচন করুন" else "Select Number for SMS"
            NumberPickerAction.WHATSAPP -> if (isBangla) "হোয়াটসঅ্যাপ চ্যাটের জন্য নম্বর নির্বাচন করুন" else "Select Number for WhatsApp"
        }
        val actionColor = when (numberPickerAction) {
            NumberPickerAction.CALL -> CardMateTealPrimary
            NumberPickerAction.SMS -> CardMateCyanAccent
            NumberPickerAction.WHATSAPP -> Color(0xFF25D366)
        }
        val actionIcon = when (numberPickerAction) {
            NumberPickerAction.CALL -> Icons.Default.Call
            NumberPickerAction.SMS -> Icons.Default.Message
            NumberPickerAction.WHATSAPP -> null
        }

        AlertDialog(
            onDismissRequest = { showNumberPickerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (actionIcon != null) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = actionTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    allPhones.forEach { (label, number) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, actionColor.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showNumberPickerDialog = false
                                    when (numberPickerAction) {
                                        NumberPickerAction.CALL -> {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, number, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        NumberPickerAction.SMS -> {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, number, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        NumberPickerAction.WHATSAPP -> {
                                            openWhatsApp(
                                                context = context,
                                                rawPhone = number,
                                                defaultMessage = if (isBangla) "হ্যালো ${card.fullName}," else "Hello ${card.fullName},"
                                            )
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = actionColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = label,
                                            color = actionColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = number,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = actionColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (actionIcon != null) {
                                            Icon(
                                                imageVector = actionIcon,
                                                contentDescription = null,
                                                tint = actionColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                                contentDescription = null,
                                                tint = actionColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNumberPickerDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Card Note Editor & Viewer Dialog
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF97316).copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isBangla) "কার্ড নোট যুক্ত / সম্পাদনা করুন" else "Card Notes & Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Action: Insert Timestamp Header for new note / log
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CardMateTealPrimary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                val prefix = if (editingNoteText.isBlank()) "[$timestamp]: " else "\n\n[$timestamp]: "
                                editingNoteText += prefix
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "+ তারিখ ও সময় সহ নতুন নোট লিখুন" else "+ Insert Date/Time Header",
                                color = CardMateTealPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editingNoteText,
                        onValueChange = { editingNoteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .testTag("card_note_input_field"),
                        placeholder = {
                            Text(
                                text = if (isBangla) "এখানে কার্ডের বিষয়ে নতুন নোট লিখুন বা সম্পাদনা করুন..." else "Write new note or edit notes here...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CardMateTealPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            cursorColor = CardMateTealPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (card.rawOcrText.isNotBlank() && card.rawOcrText != editingNoteText) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val prefix = if (editingNoteText.isBlank()) "" else "\n\n"
                                    editingNoteText += "$prefix${card.rawOcrText}"
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = CardMateCyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "স্ক্যানকৃত আসল টেক্সট নোটে যুক্ত করুন" else "Append scanned OCR text",
                                    color = CardMateCyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCardNotes(card, editingNoteText.trim())
                        showNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("card_note_save_btn")
                ) {
                    Text(
                        text = if (isBangla) "সংরক্ষণ করুন" else "Save Note",
                        color = Color(0xFF042F2E),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text(
                        text = if (isBangla) "বাতিল" else "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(if (isBangla) "কার্ড মুছে ফেলতে চান?" else "Delete Business Card?") },
            text = { Text(if (isBangla) "এই কার্ডটি স্থায়ীভাবে মুছে যাবে।" else "Are you sure you want to delete ${card.fullName}'s business card?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCard(card)
                        showDeleteDialog = false
                        onDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(if (isBangla) "মুছে ফেলুন" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Share Bottom Sheet (vCard & Image Export)
    if (showShareBottomSheet) {
        CardShareBottomSheet(
            card = card,
            isBangla = isBangla,
            onDismiss = { showShareBottomSheet = false },
            onOpenQrStudio = { showDynamicQrDialog = true }
        )
    }

    // Dynamic QR Generator Dialog
    if (showDynamicQrDialog) {
        DynamicQrGeneratorDialog(
            viewModel = viewModel,
            initialCard = card,
            onDismiss = { showDynamicQrDialog = false }
        )
    }

    // Google Contacts Synchronization Dialog (Google People API v1)
    if (showGoogleContactsDialog) {
        GoogleContactsSyncDialog(
            card = card,
            viewModel = viewModel,
            onDismiss = { showGoogleContactsDialog = false }
        )
    }

    // CardMate AI Assistant Bottom Sheet
    if (showAiAssistantBottomSheet) {
        CardMateAiAssistantBottomSheet(
            viewModel = viewModel,
            targetCard = card,
            initialTab = assistantInitialTab,
            onDismiss = { showAiAssistantBottomSheet = false }
        )
    }

    // Scanned Card Photos Bottom Sheet (Slide down from Card)
    if (showScannedPhotosSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScannedPhotosSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            var selectedPhotoTab by remember { mutableStateOf(0) }
            val targetUri = if (selectedPhotoTab == 0) {
                card.cardFrontImageUri ?: card.cardBackImageUri
            } else {
                card.cardBackImageUri ?: card.cardFrontImageUri
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBangla) "স্ক্যানকৃত আসল কার্ডের ছবি" else "Scanned Card Photos",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "Pinch / ডাবল ট্যাপ করে জুম ও ফুলস্ক্রিন দেখুন" else "Pinch to zoom, double-tap & fullscreen",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }

                    IconButton(
                        onClick = { showScannedPhotosSheet = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Front / Back Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!card.cardFrontImageUri.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedPhotoTab == 0) CardMateTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedPhotoTab = 0 }
                        ) {
                            Text(
                                text = if (isBangla) "📷 সামনের পাশ (Front)" else "📷 Front Photo",
                                color = if (selectedPhotoTab == 0) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (!card.cardBackImageUri.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedPhotoTab == 1) CardMateGoldAccent else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedPhotoTab = 1 }
                        ) {
                            Text(
                                text = if (isBangla) "📷 পেছনের পাশ (Back)" else "📷 Back Photo",
                                color = if (selectedPhotoTab == 1) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Zoomable Card Image View
                ZoomableCardImageView(
                    imagePath = targetUri,
                    title = "${card.fullName} - ${if (selectedPhotoTab == 0) "Front Side" else "Back Side"}",
                    ocrRawText = card.notes,
                    isBangla = isBangla,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )

                // Image Action Buttons: Edit Image & Re-run OCR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Image Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardMateTealPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val currentSide = if (selectedPhotoTab == 0) "front" else "back"
                                showScannedPhotosSheet = false
                                onEditImage(currentSide, targetUri)
                            }
                            .testTag("edit_card_image_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Edit Image",
                                tint = Color(0xFF042F2E),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "ছবি এডিট ও ক্রপ" else "Edit & Crop Photo",
                                color = Color(0xFF042F2E),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Re-run OCR Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRunningOcrOnCard) Color(0xFF1E293B) else CardMateGoldAccent.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardMateGoldAccent),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isRunningOcrOnCard && !targetUri.isNullOrBlank()) {
                                if (!targetUri.isNullOrBlank()) {
                                    isRunningOcrOnCard = true
                                    viewModel.reRunOcrOnEditedCardImage(card.id, targetUri) { _, msg ->
                                        isRunningOcrOnCard = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .testTag("rerun_ocr_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRunningOcrOnCard) {
                                CircularProgressIndicator(
                                    color = CardMateGoldAccent,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Re-run OCR",
                                    tint = CardMateGoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunningOcrOnCard) {
                                    if (isBangla) "স্ক্যান হচ্ছে..." else "Scanning..."
                                } else {
                                    if (isBangla) "পুনরায় AI OCR" else "Re-run AI OCR"
                                },
                                color = CardMateGoldAccent,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns all phone numbers available on a BusinessCard (Primary, Secondary, Custom fields)
 */
private fun getCardPhoneNumbers(card: BusinessCard, isBangla: Boolean): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    val primaryPhones = com.example.util.PhoneNumberUtils.splitPhoneNumbers(card.phone)
    if (primaryPhones.isNotEmpty()) {
        list.add(Pair(if (isBangla) "প্রধান ফোন" else "Primary Phone", primaryPhones[0]))
        primaryPhones.drop(1).forEachIndexed { idx, p ->
            list.add(Pair(if (isBangla) "বিকল্প ফোন ${idx + 2}" else "Alt Phone ${idx + 2}", p))
        }
    }
    val secPhones = card.getSecondaryPhoneNumbers()
    secPhones.forEachIndexed { idx, p ->
        if (list.none { com.example.util.PhoneNumberUtils.normalizeForComparison(it.second) == com.example.util.PhoneNumberUtils.normalizeForComparison(p) }) {
            list.add(Pair(if (isBangla) "বিকল্প ফোন ${list.size + 1}" else "Alt Phone ${list.size + 1}", p))
        }
    }
    return list
}

/**
 * Resolves and launches external web URLs or specific social apps
 */
private fun openExternalLinkOrApp(context: Context, rawLink: String) {
    var link = rawLink.trim()
    if (link.isBlank()) return

    var platform = ""
    if (link.contains(":")) {
        val parts = link.split(":", limit = 2)
        platform = parts[0].trim().lowercase()
        link = parts[1].trim()
    }

    val formattedUrl = when {
        link.startsWith("http://", ignoreCase = true) || link.startsWith("https://", ignoreCase = true) -> link
        platform == "linkedin" -> {
            if (link.contains("linkedin.com")) "https://$link" else "https://www.linkedin.com/in/${link.removePrefix("@")}"
        }
        platform == "twitter" || platform == "x" -> {
            if (link.contains("twitter.com") || link.contains("x.com")) "https://$link" else "https://x.com/${link.removePrefix("@")}"
        }
        platform == "facebook" || platform == "fb" -> {
            if (link.contains("facebook.com")) "https://$link" else "https://www.facebook.com/${link.removePrefix("@")}"
        }
        platform == "instagram" || platform == "insta" -> {
            if (link.contains("instagram.com")) "https://$link" else "https://www.instagram.com/${link.removePrefix("@")}"
        }
        platform == "github" -> {
            if (link.contains("github.com")) "https://$link" else "https://github.com/${link.removePrefix("@")}"
        }
        platform == "youtube" || platform == "yt" -> {
            if (link.contains("youtube.com") || link.contains("youtu.be")) "https://$link" else "https://www.youtube.com/@${link.removePrefix("@")}"
        }
        platform == "telegram" || platform == "tg" -> {
            if (link.contains("t.me")) "https://$link" else "https://t.me/${link.removePrefix("@")}"
        }
        platform == "whatsapp" -> {
            val cleanPhone = link.replace(Regex("[^0-9+]"), "").let { if (it.startsWith("+")) it.substring(1) else it }
            "https://wa.me/$cleanPhone"
        }
        else -> "https://$link"
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, link, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Quick Action Button under Card
 */
@Composable
private fun ActionBigButton(
    title: String,
    icon: ImageVector? = null,
    painter: Painter? = null,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
            .widthIn(min = 68.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            } else if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/**
 * Interactive Phone Number Row in View Mode:
 * Tapping number directly dials, with Call, SMS, WhatsApp, and Copy icons on right.
 */
@Composable
private fun PhoneInteractiveRow(
    label: String,
    number: String,
    isBangla: Boolean,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                } catch (e: Exception) {
                    Toast.makeText(context, number, Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CardMateTealPrimary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = label,
                        color = CardMateTealPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = number,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Interactive Icons: Call, SMS, WhatsApp, Copy
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Call
                IconButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                        } catch (e: Exception) {
                            Toast.makeText(context, number, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // SMS
                IconButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")))
                        } catch (e: Exception) {
                            Toast.makeText(context, number, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "SMS",
                        tint = CardMateCyanAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // WhatsApp
                IconButton(
                    onClick = {
                        openWhatsApp(context, number)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Copy
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(number))
                        Toast.makeText(
                            context,
                            if (isBangla) "$number কপি হয়েছে" else "Phone number copied",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive Email Row
 */
@Composable
private fun EmailInteractiveRow(
    label: String,
    email: String,
    isBangla: Boolean,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                } catch (e: Exception) {
                    Toast.makeText(context, email, Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFA78BFA).copy(alpha = 0.18f)
                ) {
                    Text(
                        text = label,
                        color = Color(0xFFA78BFA),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = email,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                        } catch (e: Exception) {
                            Toast.makeText(context, email, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Send Email",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(email))
                        Toast.makeText(context, if (isBangla) "ইমেইল কপি হয়েছে" else "Email copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive Website Row
 */
@Composable
private fun WebsiteInteractiveRow(
    label: String,
    url: String,
    isBangla: Boolean,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { openExternalLinkOrApp(context, url) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CardMateCyanAccent.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = label,
                        color = CardMateCyanAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = url,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(
                    onClick = { openExternalLinkOrApp(context, url) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Open Browser",
                        tint = CardMateCyanAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(url))
                        Toast.makeText(context, if (isBangla) "লিংক কপি হয়েছে" else "Link copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive Address Row (Clicking opens Google Maps)
 */
@Composable
private fun AddressInteractiveRow(
    label: String,
    address: String,
    isBangla: Boolean,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val openMaps = {
        try {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            mapIntent.setPackage("com.google.android.apps.maps")
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            try {
                val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e2: Exception) {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { openMaps() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CardMateGoldAccent.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = label,
                        color = CardMateGoldAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = address,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(
                    onClick = openMaps,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Open Maps",
                        tint = CardMateGoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(address))
                        Toast.makeText(context, if (isBangla) "ঠিকানা কপি হয়েছে" else "Address copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive Social Link & Custom Field Row (Opens external browser or relevant app)
 */
@Composable
private fun SocialLinkInteractiveRow(
    label: String,
    value: String,
    isBangla: Boolean,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val openAction = {
        openExternalLinkOrApp(context, "$label: $value")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { openAction() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (label.lowercase()) {
                        "whatsapp" -> Color(0xFF25D366).copy(alpha = 0.18f)
                        "linkedin" -> Color(0xFF0077B5).copy(alpha = 0.18f)
                        "github" -> Color(0xFF94A3B8).copy(alpha = 0.18f)
                        "twitter", "x" -> Color(0xFF38BDF8).copy(alpha = 0.18f)
                        "facebook", "fb" -> Color(0xFF1877F2).copy(alpha = 0.18f)
                        "instagram", "insta" -> Color(0xFFE4405F).copy(alpha = 0.18f)
                        else -> CardMateTealPrimary.copy(alpha = 0.18f)
                    }
                ) {
                    Text(
                        text = label,
                        color = when (label.lowercase()) {
                            "whatsapp" -> Color(0xFF16A34A)
                            "linkedin" -> Color(0xFF0284C7)
                            "github" -> Color(0xFF94A3B8)
                            "twitter", "x" -> Color(0xFF38BDF8)
                            "facebook", "fb" -> Color(0xFF3B82F6)
                            "instagram", "insta" -> Color(0xFFEC4899)
                            else -> CardMateTealPrimary
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(
                    onClick = openAction,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open Link",
                        tint = CardMateCyanAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(value))
                        Toast.makeText(context, if (isBangla) "কপি হয়েছে" else "Copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive Notes Row
 */
@Composable
private fun NoteInteractiveRow(
    label: String,
    notes: String,
    isBangla: Boolean,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF97316).copy(alpha = 0.18f)
                ) {
                    Text(
                        text = label,
                        color = Color(0xFFF97316),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notes,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(notes))
                    Toast.makeText(context, if (isBangla) "নোট কপি হয়েছে" else "Note copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Note",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Clean Basic Info Row for Non-Interactive Identity Properties
 */
@Composable
private fun BasicInfoRow(
    label: String,
    value: String,
    icon: ImageVector? = null
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CardMateTealPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun openWhatsApp(
    context: Context,
    rawPhone: String,
    defaultMessage: String = ""
) {
    val cleanPhone = rawPhone.replace(Regex("[^0-9+]"), "").let {
        if (it.startsWith("+")) it.substring(1) else it
    }
    try {
        val uri = if (cleanPhone.isNotBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone" + if (defaultMessage.isNotBlank()) "&text=${Uri.encode(defaultMessage)}" else "")
        } else {
            Uri.parse("https://api.whatsapp.com/send" + if (defaultMessage.isNotBlank()) "?text=${Uri.encode(defaultMessage)}" else "")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "হোয়াটসঅ্যাপ চালু করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
    }
}
