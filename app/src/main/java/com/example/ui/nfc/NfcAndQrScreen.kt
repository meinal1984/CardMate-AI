package com.example.ui.nfc

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import com.example.ui.qr.DynamicQrGeneratorContent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessCard
import com.example.data.model.CardTemplate
import com.example.nfc.NfcCardService
import com.example.nfc.NfcOperationMode
import com.example.nfc.NfcWriteResult
import com.example.sync.VCardExporter
import com.example.ui.components.CardShareBottomSheet
import com.example.ui.components.DigitalBusinessCardView
import com.example.ui.components.DigitalCardVisualPreviewDialog
import com.example.ui.components.QrCodeHelper
import com.example.ui.navigation.BackNavigationService
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel

@Composable
fun NfcAndQrScreen(
    viewModel: CardViewModel,
    onCardCreatedFromScan: (BusinessCard) -> Unit
) {
    val context = LocalContext.current
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    var previewCardForDialog by remember { mutableStateOf<BusinessCard?>(null) }
    var shareCardForSheet by remember { mutableStateOf<BusinessCard?>(null) }

    val navService = remember { BackNavigationService.instance }

    // Modal back intercepts
    DisposableEffect(previewCardForDialog) {
        if (previewCardForDialog != null) {
            val unreg = navService.registerModal {
                previewCardForDialog = null
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(shareCardForSheet) {
        if (shareCardForSheet != null) {
            val unreg = navService.registerModal {
                shareCardForSheet = null
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    // If sub-tab is QR Scanner (1) or Digital QR (2), back returns to NFC (0) first
    DisposableEffect(selectedTabIndex) {
        if (selectedTabIndex != 0) {
            val unreg = navService.registerModal {
                selectedTabIndex = 0
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    val tabs = listOf(
        if (isBangla) "এনএফসি" else "NFC",
        if (isBangla) "স্ক্যানার" else "Scanner",
        if (isBangla) "ডিজিটাল কার্ড" else "My Card",
        if (isBangla) "কিউআর স্টুডিও" else "QR Studio"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("nfc_qr_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Custom Tab Bar
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = CardMateTealPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = CardMateTealPrimary,
                    height = 3.dp
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTabIndex) {
            0 -> NfcExchangeSection(
                viewModel = viewModel,
                isBangla = isBangla,
                onCardDetected = onCardCreatedFromScan,
                onPreviewCardLayout = { card -> previewCardForDialog = card }
            )
            1 -> QrScannerSection(
                isBangla = isBangla,
                onCardDetected = onCardCreatedFromScan
            )
            2 -> MyDigitalQrSection(
                viewModel = viewModel,
                isBangla = isBangla,
                onOpenFullPreview = { card -> previewCardForDialog = card },
                onOpenShareSheet = { card -> shareCardForSheet = card },
                onSwitchToNfcTab = { selectedTabIndex = 0 },
                onSwitchToQrStudio = { selectedTabIndex = 3 }
            )
            3 -> DynamicQrGeneratorContent(
                viewModel = viewModel,
                onClose = null
            )
        }
    }

    // Full Visual Preview Mode Dialog
    previewCardForDialog?.let { card ->
        DigitalCardVisualPreviewDialog(
            card = card,
            isBangla = isBangla,
            onDismiss = { previewCardForDialog = null },
            onTemplateChanged = { newTemplateId ->
                // Update profile template if previewing user's own card
                val userProfile = viewModel.userProfile.value
                if (card.fullName == userProfile.fullName) {
                    viewModel.saveUserProfile(userProfile.copy(cardLayoutTemplate = newTemplateId))
                }
            },
            onShareNfc = { updatedCard ->
                viewModel.setNfcMode(NfcOperationMode.WRITE_TAG, updatedCard)
                selectedTabIndex = 0
                Toast.makeText(
                    context,
                    if (isBangla) "এনএফসি রাইট মোড সক্রিয় করা হয়েছে" else "NFC Write Mode Activated",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onShareQr = { updatedCard ->
                selectedTabIndex = 2
            },
            onExportImage = { updatedCard ->
                shareCardForSheet = updatedCard
            }
        )
    }

    // Share Options Bottom Sheet
    shareCardForSheet?.let { card ->
        CardShareBottomSheet(
            card = card,
            isBangla = isBangla,
            onDismiss = { shareCardForSheet = null }
        )
    }
}

@Composable
private fun NfcExchangeSection(
    viewModel: CardViewModel,
    isBangla: Boolean,
    onCardDetected: (BusinessCard) -> Unit,
    onPreviewCardLayout: (BusinessCard) -> Unit
) {
    val context = LocalContext.current
    val nfcState by viewModel.nfcUiState.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isNfcSharingEnabled by viewModel.isNfcSharingEnabled.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "nfcPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var cardPickerExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Privacy Notice if NFC Sharing is disabled in Settings
        if (!isNfcSharingEnabled) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardMateCyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CardMateCyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "এনএফসি শেয়ারিং বন্ধ আছে" else "NFC Sharing is Paused",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) "প্রাইভেসি সেটিংসে এনএফসি শেয়ারিং নিষ্ক্রিয় করা আছে" else "NFC card exchange is turned off in Privacy Settings",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.setNfcSharingEnabled(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isBangla) "সক্রিয় করুন" else "Enable",
                                color = Color(0xFF042F2E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 1. NFC Hardware Status Alert if Disabled
        if (!nfcState.isNfcEnabled) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF7F1D1D).copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "এনএফসি বন্ধ রয়েছে" else "NFC is Disabled",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) "কার্ড রিড বা রাইট করতে সেটিংসে এনএফসি চালু করুন" else "Enable NFC in Settings to tap smart cards",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Button(
                            onClick = { NfcCardService.openNfcSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isBangla) "অন করুন" else "Turn On",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2. NFC Sub-Mode Selector (Read / Write / P2P Beam)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Read Tag Mode
                FilterChip(
                    selected = nfcState.mode == NfcOperationMode.READ_TAG,
                    onClick = { viewModel.setNfcMode(NfcOperationMode.READ_TAG) },
                    label = {
                        Text(
                            text = if (isBangla) "📥 কার্ড রিডার" else "📥 Read Tag",
                            fontSize = 11.sp,
                            fontWeight = if (nfcState.mode == NfcOperationMode.READ_TAG) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardMateTealPrimary,
                        selectedLabelColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Write Tag Mode
                FilterChip(
                    selected = nfcState.mode == NfcOperationMode.WRITE_TAG,
                    onClick = {
                        val card = nfcState.cardToWrite ?: userProfile.toBusinessCard()
                        viewModel.setNfcMode(NfcOperationMode.WRITE_TAG, card)
                    },
                    label = {
                        Text(
                            text = if (isBangla) "📤 ট্যাগ রাইটার" else "📤 Write Tag",
                            fontSize = 11.sp,
                            fontWeight = if (nfcState.mode == NfcOperationMode.WRITE_TAG) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardMateTealPrimary,
                        selectedLabelColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier.weight(1f)
                )

                // P2P Beam / HCE Mode
                FilterChip(
                    selected = nfcState.mode == NfcOperationMode.P2P_BEAM,
                    onClick = { viewModel.setNfcMode(NfcOperationMode.P2P_BEAM) },
                    label = {
                        Text(
                            text = if (isBangla) "📲 ফোন টু ফোন" else "📲 P2P Beam",
                            fontSize = 11.sp,
                            fontWeight = if (nfcState.mode == NfcOperationMode.P2P_BEAM) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardMateTealPrimary,
                        selectedLabelColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Main Interactive Card Based on Selected Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing NFC Antenna Icon
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .scale(if (nfcState.isNfcEnabled) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    when (nfcState.mode) {
                                        NfcOperationMode.WRITE_TAG -> CardMateGoldAccent.copy(alpha = 0.15f)
                                        NfcOperationMode.P2P_BEAM -> CardMateCyanAccent.copy(alpha = 0.15f)
                                        else -> CardMateTealPrimary.copy(alpha = 0.15f)
                                    }
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = when (nfcState.mode) {
                                            NfcOperationMode.WRITE_TAG -> listOf(CardMateGoldAccent, Color(0xFFB45309))
                                            NfcOperationMode.P2P_BEAM -> listOf(CardMateCyanAccent, Color(0xFF0369A1))
                                            else -> listOf(CardMateTealPrimary, Color(0xFF0F766E))
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (nfcState.mode) {
                                    NfcOperationMode.WRITE_TAG -> Icons.Default.Upload
                                    NfcOperationMode.P2P_BEAM -> Icons.Default.Phonelink
                                    else -> Icons.Default.Nfc
                                },
                                contentDescription = "NFC Sensor",
                                tint = Color(0xFF042F2E),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (nfcState.mode) {
                            NfcOperationMode.WRITE_TAG -> if (isBangla) "এনএফসি কার্ডে রাইট করুন" else "Write Card to NFC Smart Tag"
                            NfcOperationMode.P2P_BEAM -> if (isBangla) "পিয়ার-টু-পিয়ার টাচ শেয়ার" else "Peer-to-Peer Contactless Beam"
                            else -> if (isBangla) "এনএফসি কার্ড স্পর্শ করুন" else "Hold NFC Card Near Phone"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (nfcState.mode) {
                            NfcOperationMode.WRITE_TAG -> if (isBangla) "যে কোনো NTAG213/215/216 বা Mifare ট্যাগে vCard 3.0 রাইট করুন" else "Ready to write standard vCard 3.0 & CardMate payload to writable tag"
                            NfcOperationMode.P2P_BEAM -> if (isBangla) "কার্ডমেট HCE সার্ভিস সক্রিয়; অপর ফোনের পেছনে স্পর্শ করালেই কার্ড শেয়ার হবে" else "Active HCE emulation broadcasts your digital card when tapped against another phone"
                            else -> if (isBangla) "NDEF vCard, স্মার্ট ভিজিটিং কার্ড বা অন্য ফোন স্ক্যান করতে প্রস্তুত" else "Ready to read contactless NDEF vCards, smart tags, or digital phones"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // Mode-Specific Controls & Details
                    when (nfcState.mode) {
                        NfcOperationMode.WRITE_TAG -> {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Card Picker for writing
                            val activeCardToWrite = nfcState.cardToWrite ?: userProfile.toBusinessCard()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { cardPickerExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "নির্বাচিত কার্ড:" else "Selected Card to Write:",
                                            fontSize = 10.sp,
                                            color = CardMateTealPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = activeCardToWrite.fullName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${activeCardToWrite.jobTitle} • ${activeCardToWrite.company}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Change Card",
                                        tint = CardMateTealPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = cardPickerExpanded,
                                    onDismissRequest = { cardPickerExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("My Profile Card (${userProfile.fullName})") },
                                        onClick = {
                                            viewModel.setCardToWrite(userProfile.toBusinessCard())
                                            cardPickerExpanded = false
                                        }
                                    )
                                    allCards.forEach { card ->
                                        DropdownMenuItem(
                                            text = { Text("${card.fullName} (${card.company})") },
                                            onClick = {
                                                viewModel.setCardToWrite(card)
                                                cardPickerExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Preview Card Layout button before writing
                            OutlinedButton(
                                onClick = { onPreviewCardLayout(activeCardToWrite) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = CardMateCyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "👁️ কার্ড লেআউট প্রিভিউ দেখুন" else "👁️ Preview Card Layout",
                                    color = CardMateCyanAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // AAR App Launch Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBangla) "অ্যান্ড্রয়েড অ্যাপ অটো-ওপেন (AAR)" else "Include Android App Record (AAR)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isBangla) "অন্য ফোনে ট্যাপ করলে কার্ডমেট অ্যাপ স্বয়ংক্রিয়ভাবে খুলবে" else "Auto-launches CardMate when tapped on Android",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = nfcState.includeAarInWrite,
                                    onCheckedChange = { viewModel.toggleIncludeAar(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CardMateTealPrimary,
                                        checkedTrackColor = CardMateTealPrimary.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Write Action / Simulation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "এনএফসি কার্ড স্পর্শ করুন..." else "Ready! Touch NFC Tag to device...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CardMateGoldAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF042F2E))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBangla) "ট্যাগ স্পর্শ করুন" else "Ready to Write",
                                        color = Color(0xFF042F2E),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.setNfcMode(NfcOperationMode.FORMAT_TAG) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "ট্যাগ মুছুন" else "Format Tag",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        NfcOperationMode.P2P_BEAM -> {
                            val activeBeamCard = userProfile.toBusinessCard()
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CardMateCyanAccent.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardMateCyanAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CardMateCyanAccent, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isBangla) "HCE ব্রডকাস্ট সক্রিয়" else "Host Card Emulation Active",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CardMateCyanAccent
                                        )
                                        Text(
                                            text = "${userProfile.fullName} (${userProfile.company})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { onPreviewCardLayout(activeBeamCard) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = CardMateCyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "👁️ বিম কার্ড লেআউট প্রিভিউ দেখুন" else "👁️ Preview Beam Card Layout",
                                    color = CardMateCyanAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            // Read Tag Mode
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val demoCard = BusinessCard(
                                        fullName = "মাহমুদুর রহমান (Mahmudur Rahman)",
                                        jobTitle = "Managing Partner & Angel Investor",
                                        company = "Bengal Ventures Capital Ltd.",
                                        phone = "+880 1711-002233",
                                        email = "mahmud@bengalventures.vc",
                                        website = "https://bengalventures.vc",
                                        address = "গুলশান এভিনিউ, ঢাকা",
                                        category = "Finance & Banking",
                                        notes = "NFC Tap Contact Card exchanged at FinTech Summit",
                                        cardLayoutTemplate = "executive_gold",
                                        isBackedUpToCloud = true
                                    )
                                    viewModel.setSimulatedReadCard(demoCard)
                                    Toast.makeText(context, "NFC Card Tag Read Successfully!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Nfc, contentDescription = null, tint = Color(0xFF042F2E))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "এনএফসি রিড সিমুলেশন" else "Simulate Instant NFC Read",
                                    color = Color(0xFF042F2E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Status Message Display
                    if (nfcState.statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = nfcState.statusMessage,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 4. Detected NFC Tag Details (if scanned)
        nfcState.lastReadTagInfo?.let { tagInfo ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "সনাক্তকৃত ট্যাগ প্রযুক্তি" else "NFC Hardware Tag Details",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (tagInfo.isWritable) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (tagInfo.isWritable) CardMateTealPrimary else Color(0xFFF87171),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (tagInfo.isWritable) "Writable" else "Read-Only",
                                    color = if (tagInfo.isWritable) CardMateTealPrimary else Color(0xFFF87171),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Tag Type: ${tagInfo.tagType}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "UID: ${tagInfo.tagIdHex}", color = CardMateCyanAccent, fontSize = 11.sp)
                        Text(text = "Memory: ${tagInfo.currentSizeBytes} / ${tagInfo.maxSizeBytes} bytes • Techs: ${tagInfo.technologies.joinToString(", ")}", color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                }
            }
        }

        // 5. Read Business Card Result Card
        nfcState.lastReadCard?.let { card ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardMateTealPrimary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "এনএফসি কার্ড পাওয়া গেছে!" else "NFC Contact Received!",
                                color = CardMateTealPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0F766E)
                            ) {
                                Text(
                                    text = "NDEF vCard 3.0",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = card.fullName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${card.jobTitle} • ${card.company}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        if (card.phone.isNotBlank()) Text(text = card.phone, color = CardMateCyanAccent, fontSize = 12.sp)
                        if (card.email.isNotBlank()) Text(text = card.email, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                        if (card.website.isNotBlank()) Text(text = card.website, color = CardMateGoldAccent, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onCardDetected(card)
                                    viewModel.saveCard(card) {
                                        Toast.makeText(context, "Card Saved to CardMate!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF042F2E))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "সংরক্ষণ করুন" else "Save Card",
                                    color = Color(0xFF042F2E),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetNfcReadResult() },
                                modifier = Modifier.weight(0.6f)
                            ) {
                                Text(text = if (isBangla) "বাতিল" else "Dismiss", color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrScannerSection(
    isBangla: Boolean,
    onCardDetected: (BusinessCard) -> Unit
) {
    val context = LocalContext.current
    var scannedData by remember { mutableStateOf<BusinessCard?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .border(2.dp, CardMateCyanAccent, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Viewfinder",
                            tint = CardMateCyanAccent,
                            modifier = Modifier.size(72.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isBangla) "কিউআর কোড স্ক্যান করুন" else "Align QR Code within frame",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBangla) "vCard, MECARD বা ডিজিটাল বিজনেস কার্ড লিংক" else "Supports vCard 3.0, MECARD & contact URLs",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val card = BusinessCard(
                                fullName = "Zubair Al Mamun",
                                jobTitle = "Senior Full-Stack Engineer",
                                company = "Silicon Dhaka Labs",
                                phone = "+880 1688-990011",
                                email = "zubair@silicondhaka.io",
                                website = "https://silicondhaka.io",
                                address = "Karwan Bazar, Dhaka 1215",
                                category = "Tech & IT",
                                notes = "Scanned from Digital Business QR code.",
                                cardLayoutTemplate = "cyber_neon",
                                isBackedUpToCloud = true
                            )
                            scannedData = card
                            Toast.makeText(context, "QR Code Scanned Successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardMateCyanAccent)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF082F49))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "সিমুলেট কিউআর রিড" else "Simulate QR Scan",
                            color = Color(0xFF082F49),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        scannedData?.let { card ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardMateCyanAccent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (isBangla) "কিউআর কন্টাক্ট পাওয়া গেছে" else "QR Contact Decoded",
                            color = CardMateCyanAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = card.fullName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${card.jobTitle} • ${card.company}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(text = card.phone, color = CardMateTealPrimary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onCardDetected(card)
                                Toast.makeText(context, "Card Saved from QR!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardMateCyanAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBangla) "কার্ডে যুক্ত করুন" else "Add to My Cards",
                                color = Color(0xFF082F49),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyDigitalQrSection(
    viewModel: CardViewModel,
    isBangla: Boolean,
    onOpenFullPreview: (BusinessCard) -> Unit,
    onOpenShareSheet: (BusinessCard) -> Unit,
    onSwitchToNfcTab: () -> Unit,
    onSwitchToQrStudio: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val userProfile by viewModel.userProfile.collectAsState()
    val isQrSharingEnabled by viewModel.isQrSharingEnabled.collectAsState()
    val myCard = userProfile.toBusinessCard()

    var viewMode by remember { mutableStateOf(0) } // 0: 3D Card Layout Preview, 1: QR Code
    var isCardFlipped by remember { mutableStateOf(false) }

    val vcardString = remember(myCard) { VCardExporter.toVCard3String(myCard) }
    val qrBitmap = remember(vcardString) {
        QrCodeHelper.generateQrBitmap(
            content = vcardString,
            sizePx = 512,
            darkColor = android.graphics.Color.BLACK,
            lightColor = android.graphics.Color.WHITE
        )
    }

    val currentTemplate = remember(userProfile.cardLayoutTemplate) {
        CardTemplate.fromId(userProfile.cardLayoutTemplate)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Privacy Notice if QR Sharing is disabled in Settings
        if (!isQrSharingEnabled) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardMateGoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CardMateGoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "QR শেয়ারিং বন্ধ আছে" else "QR Sharing is Paused",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) "প্রাইভেসি সেটিংসে QR শেয়ারিং নিষ্ক্রিয় করা আছে" else "QR card matrix sharing is turned off in Privacy Settings",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.setQrSharingEnabled(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isBangla) "সক্রিয় করুন" else "Enable",
                                color = Color(0xFF042F2E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // View Mode Selector: 3D Layout Preview vs QR Code
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = viewMode == 0,
                    onClick = { viewMode = 0 },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "🎨 কার্ড লেআউট প্রিভিউ" else "🎨 3D Card Preview",
                                fontSize = 11.sp,
                                fontWeight = if (viewMode == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardMateTealPrimary,
                        selectedLabelColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = viewMode == 1,
                    onClick = { viewMode = 1 },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "⬛ কিউআর কোড ভিউ" else "⬛ QR Matrix View",
                                fontSize = 11.sp,
                                fontWeight = if (viewMode == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardMateTealPrimary,
                        selectedLabelColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (viewMode == 0) {
            // 3D Visual Card Layout Preview Mode
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isBangla) "ডিজিটাল কার্ড ভিজ্যুয়াল প্রিভিউ" else "Digital Card Visual Preview",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) "থিম: ${currentTemplate.displayNameBn}" else "Theme: ${currentTemplate.displayNameEn}",
                                    color = Color(currentTemplate.accentColor),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Full preview button
                            IconButton(
                                onClick = { onOpenFullPreview(myCard) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Full Preview",
                                    tint = CardMateCyanAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // The 3D Flip Card View
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DigitalBusinessCardView(
                                card = myCard,
                                isFlipped = isCardFlipped,
                                onFlipClick = { isCardFlipped = !isCardFlipped },
                                showSyncBadges = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Flip Hint & Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clickable { isCardFlipped = !isCardFlipped }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Flip",
                                tint = CardMateCyanAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "ফ্লিপ করতে ট্যাপ করুন (${if (isCardFlipped) "পেছন থেকে সামনে" else "সামনে থেকে পেছনে"})" else "Tap card to flip (${if (isCardFlipped) "Back to Front" else "Front to Back"})",
                                color = CardMateCyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Template Switcher Strip
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (isBangla) "🎨 লেআউট থিম পরিবর্তন করুন:" else "🎨 Quick Layout Switcher:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(CardTemplate.entries.toList()) { template ->
                                    val isSelected = userProfile.cardLayoutTemplate == template.id
                                    val templateName = if (isBangla) template.displayNameBn else template.displayNameEn

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(template.primaryBgColor),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) CardMateTealPrimary else Color.White.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .width(92.dp)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.saveUserProfile(userProfile.copy(cardLayoutTemplate = template.id))
                                                Toast.makeText(
                                                    context,
                                                    if (isBangla) "${templateName} থিম প্রয়োগ করা হয়েছে" else "${templateName} theme applied",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = templateName,
                                                    color = Color(template.textColor),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color(template.accentColor),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Full Preview, NFC Share, Share Options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenFullPreview(myCard) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isBangla) "বড় প্রিভিউ" else "Full Preview", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onSwitchToNfcTab() },
                                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Nfc, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "এনএফসি শেয়ার" else "NFC Share",
                                    color = Color(0xFF042F2E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { onOpenShareSheet(myCard) },
                                colors = ButtonDefaults.buttonColors(containerColor = CardMateCyanAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF082F49), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "শেয়ার" else "Share",
                                    color = Color(0xFF082F49),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Standard QR Matrix Code View
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBangla) "আমার ডিজিটাল কিউআর কোড" else "My Digital Business QR",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "অন্যদের সাথে শেয়ার করতে স্ক্যান করতে বলুন" else "Let others scan to instantly save your contact",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // High-res QR Code Frame
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier
                                .size(210.dp)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "Digital QR Code",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "Digital QR Code",
                                        tint = Color.Black,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = myCard.fullName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = myCard.displaySubtitle,
                            color = CardMateTealPrimary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(vcardString))
                                    Toast.makeText(context, if (isBangla) "vCard ক্লিপবোর্ডে কপি করা হয়েছে!" else "vCard copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isBangla) "কপি" else "Copy", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onSwitchToQrStudio() },
                                colors = ButtonDefaults.buttonColors(containerColor = CardMateCyanAccent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF082F49), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isBangla) "স্টুডিও" else "Studio", color = Color(0xFF082F49), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onOpenShareSheet(myCard) },
                                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isBangla) "শেয়ার" else "Share", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
