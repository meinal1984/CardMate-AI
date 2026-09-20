package com.example.ui.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessCard
import com.example.data.model.CardCategory
import com.example.ui.ai.CardMateAiAssistantBottomSheet
import com.example.ui.components.DashboardMetricGrid
import com.example.ui.components.DigitalBusinessCardView
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealDark
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel
import com.example.ui.viewmodel.NavigationTab

@Composable
fun DashboardScreen(
    viewModel: CardViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToNfc: () -> Unit,
    onNavigateToQr: () -> Unit,
    onNavigateToNewCard: () -> Unit,
    onNavigateToCards: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onSelectCard: (BusinessCard) -> Unit
) {
    val context = LocalContext.current
    val totalCount by viewModel.totalCount.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()
    val backedUpCount by viewModel.backedUpCount.collectAsState()
    val favoriteCount by viewModel.favoriteCount.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val isBangla by viewModel.isBanglaLanguage.collectAsState()

    var showAiAssistantBottomSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Scan Banner
            item {
                HeroScanBanner(
                    isBangla = isBangla,
                    onScanClick = onNavigateToScanner
                )
            }

            // Key Performance Metrics Grid
            item {
                Text(
                    text = if (isBangla) "সারসংক্ষেপ ও সিঙ্ক্রোনাইজেশন" else "Overview & Sync Status",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                DashboardMetricGrid(
                    totalCards = totalCount,
                    googleSynced = syncedCount,
                    cloudBackedUp = backedUpCount,
                    favorites = favoriteCount,
                    isBangla = isBangla,
                    onMetricClick = { category ->
                        viewModel.setSelectedCategory(category)
                        onNavigateToCards()
                    }
                )
            }

            // Quick Actions Row / Grid
            item {
                Text(
                    text = if (isBangla) "দ্রুত অ্যাকশন" else "Quick Actions",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                QuickActionGrid(
                    isBangla = isBangla,
                    onScanClick = onNavigateToScanner,
                    onQrClick = onNavigateToQr,
                    onNfcClick = onNavigateToNfc,
                    onNewCardClick = onNavigateToNewCard,
                    onProfileClick = onNavigateToProfile,
                    onAiAssistantClick = { showAiAssistantBottomSheet = true },
                    onGoogleSyncClick = { viewModel.syncAllCardsToGoogleContacts(context) },
                    onCloudBackupClick = { viewModel.performCloudBackup(context) },
                    onExportClick = { viewModel.exportAllCardsAsVCard(context) }
                )
            }

        // Categories Overview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "কার্ডের ক্যাটাগরি" else "Card Categories",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBangla) "সব দেখুন" else "View All",
                    color = CardMateTealPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToCards() }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            CategoryChipsRow(
                cards = allCards,
                isBangla = isBangla,
                onCategoryClick = { categoryName ->
                    viewModel.setSelectedCategory(categoryName)
                    onNavigateToCards()
                }
            )
        }

        // Recent Scans Carousel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "সাম্প্রতিক কার্ডসমূহ" else "Recent Business Cards",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${allCards.size} ${if (isBangla) "টি কার্ড" else "Cards"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        if (allCards.isEmpty()) {
            item {
                EmptyStateCard(isBangla = isBangla, onScanClick = onNavigateToScanner)
            }
        } else {
            items(allCards.take(5)) { card ->
                DigitalBusinessCardView(
                    card = card,
                    modifier = Modifier.padding(vertical = 4.dp),
                    onFlipClick = { onSelectCard(card) }
                )
            }
        }
    }

    // CardMate AI Assistant Bottom Sheet
    if (showAiAssistantBottomSheet) {
        CardMateAiAssistantBottomSheet(
            viewModel = viewModel,
            onDismiss = { showAiAssistantBottomSheet = false },
            onOpenCardDetail = onSelectCard
        )
    }
}
}

@Composable
private fun HeroScanBanner(
    isBangla: Boolean,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onScanClick() }
            .testTag("hero_scan_banner"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F766E),
                            Color(0xFF0E4A56),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x332DD4BF)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Powered",
                                tint = CardMateGoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "বাংলা ও ইংরেজি এআই ওসিআর" else "Gemini 3.5 Bilingual OCR",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isBangla) "ভিজিটিং কার্ড স্ক্যান করুন" else "Scan Business Card",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBangla) "স্বয়ংক্রিয় সাইজ পরিমাপ ও তাৎক্ষণিক গুগল কন্টাক্ট সিঙ্ক" else "Real-time dimension detection & 1-tap Google Sync",
                        color = Color(0xFFCCFBF1),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onScanClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardMateTealPrimary,
                            contentColor = Color(0xFF042F2E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scan",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "স্ক্যান শুরু করুন" else "Start Camera Scan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Visual scanner badge icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Scanner",
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionGrid(
    isBangla: Boolean,
    onScanClick: () -> Unit,
    onQrClick: () -> Unit,
    onNfcClick: () -> Unit,
    onNewCardClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onGoogleSyncClick: () -> Unit,
    onCloudBackupClick: () -> Unit,
    onExportClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickActionButton(
                title = if (isBangla) "AI অ্যাসিস্ট্যান্ট" else "AI Assistant",
                icon = Icons.Default.AutoAwesome,
                color = CardMateGoldAccent,
                onClick = onAiAssistantClick
            )
        }
        item {
            QuickActionButton(
                title = if (isBangla) "আমার প্রোফাইল" else "My Profile",
                icon = Icons.Default.Person,
                color = CardMateTealPrimary,
                onClick = onProfileClick
            )
        }
        item {
            QuickActionButton(
                title = if (isBangla) "ক্যামেরা স্ক্যান" else "Scan Card",
                icon = Icons.Default.DocumentScanner,
                color = CardMateTealPrimary,
                onClick = onScanClick
            )
        }
        item {
            QuickActionButton(
                title = if (isBangla) "এনএফসি রিড" else "NFC Read",
                icon = Icons.Default.Nfc,
                color = CardMateCyanAccent,
                onClick = onNfcClick
            )
        }
        item {
            QuickActionButton(
                title = if (isBangla) "কিউআর স্ক্যান" else "QR Scanner",
                icon = Icons.Default.QrCodeScanner,
                color = CardMateGoldAccent,
                onClick = onQrClick
            )
        }
        item {
            QuickActionButton(
                title = if (isBangla) "নতুন কার্ড" else "Create Card",
                icon = Icons.Default.Add,
                color = Color(0xFFA78BFA),
                onClick = onNewCardClick
            )
        }
        item {
            QuickActionButton(
                title = if (isBangla) "গুগল সিঙ্ক" else "Google Sync",
                icon = Icons.Default.Sync,
                color = Color(0xFF38BDF8),
                onClick = onGoogleSyncClick
            )
        }
        item {
            QuickActionButton(
                title = if (isBangla) "ক্লাউড ব্যাকআপ" else "Cloud Backup",
                icon = Icons.Default.CloudUpload,
                color = Color(0xFF34D399),
                onClick = onCloudBackupClick
            )
        }
        item {
            QuickActionButton(
                title = if (isBangla) "এক্সপোর্ট vCard" else "Export vCard",
                icon = Icons.Default.FileDownload,
                color = Color(0xFFF472B6),
                onClick = onExportClick
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CategoryChipsRow(
    cards: List<BusinessCard>,
    isBangla: Boolean,
    onCategoryClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val categories = CardCategory.entries.toList()
        items(categories) { category ->
            val count = cards.count { it.category.equals(category.englishName, ignoreCase = true) }
            val name = if (isBangla) category.banglaName else category.englishName
            val color = Color(category.colorHex)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCategoryClick(category.englishName) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = color.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$count",
                            color = color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    isBangla: Boolean,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DocumentScanner,
                contentDescription = null,
                tint = CardMateTealPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isBangla) "কোনো কার্ড পাওয়া যায়নি" else "No Business Cards Yet",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isBangla) "আপনার প্রথম ভিজিটিং কার্ডটি স্ক্যান করুন" else "Scan your first visiting card to get started",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary)
            ) {
                Text(
                    text = if (isBangla) "কার্ড স্ক্যান করুন" else "Scan Card Now",
                    color = Color(0xFF042F2E),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
