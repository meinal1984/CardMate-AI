package com.example.ui.qr

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BusinessCard
import com.example.sync.VCardExporter
import com.example.ui.components.ContactQrData
import com.example.ui.components.QrCenterBadge
import com.example.ui.components.QrCodeHelper
import com.example.ui.components.QrContactFormat
import com.example.ui.components.QrThemeColor
import com.example.ui.components.toContactQrData
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interactive Dynamic QR Code Generator Dialog & Studio.
 * Allows live input of contact information, real-time ZXing matrix generation,
 * customizable color themes, center badge overlays, and instant sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicQrGeneratorDialog(
    viewModel: CardViewModel,
    initialCard: BusinessCard? = null,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DynamicQrGeneratorContent(
                viewModel = viewModel,
                initialCard = initialCard,
                onClose = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicQrGeneratorContent(
    viewModel: CardViewModel,
    initialCard: BusinessCard? = null,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val allCards by viewModel.allCards.collectAsState()

    // Active Contact Data Form state
    var fullName by remember { mutableStateOf(initialCard?.fullName ?: userProfile.fullName) }
    var jobTitle by remember { mutableStateOf(initialCard?.jobTitle ?: userProfile.jobTitle) }
    var company by remember { mutableStateOf(initialCard?.company ?: userProfile.company) }
    var phone by remember { mutableStateOf(initialCard?.phone ?: userProfile.phone) }
    var secondaryPhone by remember { mutableStateOf(initialCard?.secondaryPhone ?: userProfile.secondaryPhone) }
    var email by remember { mutableStateOf(initialCard?.email ?: userProfile.email) }
    var website by remember { mutableStateOf(initialCard?.website ?: userProfile.website) }
    var address by remember { mutableStateOf(initialCard?.address ?: userProfile.address) }
    var notes by remember { mutableStateOf(initialCard?.notes ?: userProfile.bio) }

    // Customization state
    var selectedFormat by remember { mutableStateOf(QrContactFormat.VCARD_3_0) }
    var selectedThemeColor by remember { mutableStateOf(QrThemeColor.CARDMATE_TEAL) }
    var selectedBadge by remember { mutableStateOf(QrCenterBadge.CARDMATE_LOGO) }
    var showSavedCardsPicker by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    // Live Dynamic QR code bitmap
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingQr by remember { mutableStateOf(false) }
    var isSharingImage by remember { mutableStateOf(false) }

    val currentContactData = remember(fullName, jobTitle, company, phone, secondaryPhone, email, website, address, notes) {
        ContactQrData(
            fullName = fullName,
            jobTitle = jobTitle,
            company = company,
            phone = phone,
            secondaryPhone = secondaryPhone,
            email = email,
            website = website,
            address = address,
            notes = notes
        )
    }

    val currentPayload = remember(currentContactData, selectedFormat) {
        QrCodeHelper.buildContactPayload(currentContactData, selectedFormat)
    }

    // Dynamic QR generation triggered asynchronously on every state change
    LaunchedEffect(currentContactData, selectedFormat, selectedThemeColor, selectedBadge) {
        isGeneratingQr = true
        withContext(Dispatchers.Default) {
            val bitmap = QrCodeHelper.generateDynamicContactQr(
                data = currentContactData,
                format = selectedFormat,
                sizePx = 700,
                themeColor = selectedThemeColor,
                centerBadge = selectedBadge
            )
            withContext(Dispatchers.Main) {
                qrBitmap = bitmap
                isGeneratingQr = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dynamic_qr_generator_screen")
    ) {
        // Studio Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(CardMateTealPrimary.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = CardMateTealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBangla) "ডায়নামিক কিউআর জেনারেটর" else "Dynamic QR Generator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBangla) "লাইভ ZXing ইঞ্জিন • তাৎক্ষণিক শেয়ার" else "Live ZXing Engine • Instant Scannable Card",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_qr_generator_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dynamic QR Code Live Preview Display
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_preview_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, Color(selectedThemeColor.hexLong).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // QR Matrix Frame
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(240.dp)
                                .border(2.dp, Color(selectedThemeColor.hexLong), RoundedCornerShape(16.dp))
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap!!.asImageBitmap(),
                                        contentDescription = "Dynamic Contact QR Code",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                            .testTag("rendered_qr_image")
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = CardMateTealPrimary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isBangla) "কিউআর তৈরি হচ্ছে..." else "Generating QR...",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Contact Name & Subtitle
                        Text(
                            text = fullName.ifBlank { if (isBangla) "নাম উল্লেখ নেই" else "Contact Name" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        if (currentContactData.displaySubtitle.isNotBlank()) {
                            Text(
                                text = currentContactData.displaySubtitle,
                                fontSize = 13.sp,
                                color = Color(selectedThemeColor.hexLong),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Payload Status and Scan Info Chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (currentPayload.length < 500) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentPayload.toByteArray().size} bytes • ${selectedFormat.label} • ${if (currentPayload.length < 500) "Optimal Scan Density" else "Dense Matrix"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Share & Copy Icon Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentPayload))
                                    Toast.makeText(
                                        context,
                                        if (isBangla) "কিউআর ডাটা ক্লিপবোর্ডে কপি করা হয়েছে" else "QR contact payload copied to clipboard",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_payload_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "কপি ডাটা" else "Copy Text",
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    val bitmapToShare = qrBitmap
                                    if (bitmapToShare != null) {
                                        isSharingImage = true
                                        val sharableCard = QrCodeHelper.renderSharableQrCard(
                                            data = currentContactData,
                                            qrBitmap = bitmapToShare,
                                            themeColor = selectedThemeColor
                                        )
                                        QrCodeHelper.shareQrCodeImage(
                                            context = context,
                                            qrCardBitmap = sharableCard,
                                            title = "Business QR - $fullName"
                                        )
                                        isSharingImage = false
                                    } else {
                                        Toast.makeText(context, "QR code not ready", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_qr_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color(0xFF042F2E),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "শেয়ার QR" else "Share QR",
                                    color = Color(0xFF042F2E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    val bitmapToSave = qrBitmap
                                    if (bitmapToSave != null) {
                                        val sharableCard = QrCodeHelper.renderSharableQrCard(
                                            data = currentContactData,
                                            qrBitmap = bitmapToSave,
                                            themeColor = selectedThemeColor
                                        )
                                        val success = QrCodeHelper.saveQrCodeToGallery(
                                            context = context,
                                            qrBitmap = sharableCard,
                                            contactName = fullName
                                        )
                                        Toast.makeText(
                                            context,
                                            if (success) {
                                                if (isBangla) "গ্যালারিতে QR ইমেজ সংরক্ষিত হয়েছে!" else "QR code saved to Gallery!"
                                            } else {
                                                if (isBangla) "সংরক্ষণ ব্যর্থ হয়েছে" else "Failed to save image"
                                            },
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CardMateCyanAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_gallery_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF082F49),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "গ্যালারি" else "Save HD",
                                    color = Color(0xFF082F49),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Source Autofill Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Autofill My Profile
                    OutlinedButton(
                        onClick = {
                            fullName = userProfile.fullName
                            jobTitle = userProfile.jobTitle
                            company = userProfile.company
                            phone = userProfile.phone
                            secondaryPhone = userProfile.secondaryPhone
                            email = userProfile.email
                            website = userProfile.website
                            address = userProfile.address
                            notes = userProfile.bio
                            Toast.makeText(context, if (isBangla) "প্রোফাইল তথ্য লোড করা হয়েছে" else "Loaded from Profile", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isBangla) "আমার প্রোফাইল" else "My Profile", fontSize = 11.sp)
                    }

                    // Pick from saved Business Cards
                    OutlinedButton(
                        onClick = { showSavedCardsPicker = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "সংরক্ষিত কার্ড (${allCards.size})" else "From Cards (${allCards.size})",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Reset / Clear
                    IconButton(
                        onClick = {
                            fullName = ""
                            jobTitle = ""
                            company = ""
                            phone = ""
                            secondaryPhone = ""
                            email = ""
                            website = ""
                            address = ""
                            notes = ""
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear Form",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Dynamic Styling & Palette Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "🎨 কিউআর কালার থিম" else "🎨 QR Matrix Color Theme",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = selectedThemeColor.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(selectedThemeColor.hexLong)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(QrThemeColor.entries.toList()) { theme ->
                                val isSelected = selectedThemeColor == theme
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(theme.hexLong),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedThemeColor = theme }
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Center Badge Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBangla) "সেন্টার লোগো / প্রতীক:" else "Center Emblem / Icon:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(QrCenterBadge.entries.toList()) { badge ->
                                val isSelected = selectedBadge == badge
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedBadge = badge },
                                    label = {
                                        Text(
                                            text = badge.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CardMateTealPrimary,
                                        selectedLabelColor = Color(0xFF042F2E)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. Contact Information Live Fields
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBangla) "📇 কন্টাক্ট ইনফরমেশন (লাইভ এডিট)" else "📇 Contact Information (Live)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Format selector chip
                            FilterChip(
                                selected = selectedFormat == QrContactFormat.MECARD,
                                onClick = {
                                    selectedFormat = if (selectedFormat == QrContactFormat.VCARD_3_0) {
                                        QrContactFormat.MECARD
                                    } else {
                                        QrContactFormat.VCARD_3_0
                                    }
                                },
                                label = {
                                    Text(
                                        text = selectedFormat.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CardMateCyanAccent,
                                    selectedLabelColor = Color(0xFF082F49)
                                )
                            )
                        }

                        // Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text(if (isBangla) "পুরো নাম *" else "Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CardMateTealPrimary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_input_name"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        // Job Title & Company
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = jobTitle,
                                onValueChange = { jobTitle = it },
                                label = { Text(if (isBangla) "পদবী" else "Job Title") },
                                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = CardMateTealPrimary) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qr_input_job"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = company,
                                onValueChange = { company = it },
                                label = { Text(if (isBangla) "প্রতিষ্ঠান" else "Company") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = CardMateTealPrimary) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qr_input_company"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }

                        // Phone & Secondary Phone
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text(if (isBangla) "ফোন নম্বর *" else "Phone *") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = CardMateTealPrimary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qr_input_phone"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = secondaryPhone,
                                onValueChange = { secondaryPhone = it },
                                label = { Text(if (isBangla) "বিকল্প ফোন" else "Secondary") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qr_input_secondary_phone"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(if (isBangla) "ইমেইল অ্যাড্রেস" else "Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CardMateTealPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_input_email"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        // Website
                        OutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = { Text(if (isBangla) "ওয়েবসাইট" else "Website URL") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = CardMateTealPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_input_website"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        // Toggle for Address & Notes
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedSettings = !showAdvancedSettings }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = if (showAdvancedSettings) {
                                    if (isBangla) "▼ অতিরিক্ত তথ্য লুকান" else "▼ Hide Address & Bio"
                                } else {
                                    if (isBangla) "▶ ঠিকানা ও বায়ো যুক্ত করুন" else "▶ Add Address & Bio"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CardMateCyanAccent
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedSettings) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    label = { Text(if (isBangla) "ঠিকানা" else "Office / Business Address") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = CardMateTealPrimary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    maxLines = 2
                                )

                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text(if (isBangla) "নোট বা বায়ো" else "Notes / Bio") },
                                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = CardMateTealPrimary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // 5. Direct Export Options (vCard .vcf file)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (isBangla) "📤 সরাসরি কন্টাক্ট শেয়ার অপশন" else "📤 Additional Contact Sharing Options",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val dummyCard = BusinessCard(
                                        fullName = fullName,
                                        jobTitle = jobTitle,
                                        company = company,
                                        phone = phone,
                                        secondaryPhone = secondaryPhone,
                                        email = email,
                                        website = website,
                                        address = address,
                                        notes = notes
                                    )
                                    VCardExporter.shareCardAsVCardFile(context, dummyCard)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContactPage, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "vCard (.vcf) ফাইল" else "Share .vcf File",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val shareText = buildString {
                                        appendLine("📇 $fullName")
                                        if (jobTitle.isNotBlank()) appendLine("💼 $jobTitle")
                                        if (company.isNotBlank()) appendLine("🏢 $company")
                                        if (phone.isNotBlank()) appendLine("📞 $phone")
                                        if (email.isNotBlank()) appendLine("✉️ $email")
                                        if (website.isNotBlank()) appendLine("🌐 $website")
                                    }
                                    VCardExporter.shareText(context, shareText, "Contact - $fullName")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "টেক্সট হিসেবে শেয়ার" else "Share as Text",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet to Pick From Saved Business Cards
    if (showSavedCardsPicker) {
        ModalBottomSheet(
            onDismissRequest = { showSavedCardsPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (isBangla) "কার্ড নির্বাচন করুন" else "Select Business Card for QR",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (allCards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "কোন সংরক্ষিত কার্ড নেই" else "No saved cards found in database",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allCards) { card ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        fullName = card.fullName
                                        jobTitle = card.jobTitle
                                        company = card.company
                                        phone = card.phone
                                        secondaryPhone = card.secondaryPhone
                                        email = card.email
                                        website = card.website
                                        address = card.address
                                        notes = card.notes
                                        showSavedCardsPicker = false
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "${card.fullName}-এর তথ্য লোড করা হয়েছে" else "Loaded ${card.fullName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(CardMateTealPrimary.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = card.fullName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = CardMateTealPrimary,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = card.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = card.displaySubtitle.ifBlank { card.phone },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Select",
                                        tint = CardMateCyanAccent,
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
