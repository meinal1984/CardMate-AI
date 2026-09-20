package com.example.ui.cards

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BusinessCard
import com.example.data.model.CardCategory
import com.example.sync.VCardExporter
import com.example.ui.components.CardShareBottomSheet
import com.example.ui.ai.CardMateAiAssistantBottomSheet
import com.example.ui.components.DigitalBusinessCardView
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel
import com.example.ui.viewmodel.SortOption

@Composable
fun CardListScreen(
    viewModel: CardViewModel,
    onSelectCard: (BusinessCard) -> Unit,
    onNavigateToNewCard: () -> Unit
) {
    val context = LocalContext.current
    val cards by viewModel.filteredCards.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isBangla by viewModel.isBanglaLanguage.collectAsState()

    var isCompactMode by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showAiAssistantBottomSheet by remember { mutableStateOf(false) }
    var sharingCard by remember { mutableStateOf<BusinessCard?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("card_list_screen")
        ) {
            // Search Bar & Filter Header
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = if (isBangla) "নাম, কোম্পানি, ফোন বা পদবী দিয়ে খুঁজুন..." else "Search by name, company, phone, email...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CardMateTealPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CardMateTealPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bar_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Pills
            CategoryFilterBar(
                selectedCategory = selectedCategory,
                customCategories = customCategories,
                isBangla = isBangla,
                onCategorySelected = { viewModel.setSelectedCategory(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Controls Bar: Result Count, Sort Dropdown & Layout Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${cards.size} ${if (isBangla) "টি কার্ড পাওয়া গেছে" else "cards found"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort Button
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showSortMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (sortOption) {
                                        SortOption.NEWEST -> if (isBangla) "নতুন" else "Newest"
                                        SortOption.OLDEST -> if (isBangla) "পুরাতন" else "Oldest"
                                        SortOption.NAME_ASC -> "A-Z"
                                        SortOption.NAME_DESC -> "Z-A"
                                        SortOption.COMPANY -> if (isBangla) "কোম্পানি" else "Company"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isBangla) "সর্বশেষ যুক্ত (Newest)" else "Newest First") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.NEWEST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBangla) "নামের ক্রমানুসারে (A-Z)" else "Name (A-Z)") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.NAME_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBangla) "কোম্পানির নামানুসারে" else "Company Name") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.COMPANY)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // CardMate AI Assistant Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardMateTealPrimary.copy(alpha = 0.18f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showAiAssistantBottomSheet = true }
                            .testTag("list_ai_assistant_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "CardMate AI Assistant",
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "AI সহকারী" else "AI Assistant",
                                fontSize = 11.5.sp,
                                color = CardMateTealPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Layout Mode Switch
                    IconButton(
                        onClick = { isCompactMode = !isCompactMode },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompactMode) Icons.Default.GridView else Icons.Default.ViewAgenda,
                            contentDescription = "Toggle Layout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Export Button
                    IconButton(
                        onClick = { viewModel.exportAllCardsAsVCard(context) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Cards",
                            tint = CardMateCyanAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cards List
            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBangla) "কোনো কার্ড মেলেনি" else "No matching cards",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
                        if (isCompactMode) {
                            CompactCardRow(
                                card = card,
                                isBangla = isBangla,
                                onSelect = { onSelectCard(card) },
                                onToggleFav = { viewModel.toggleFavorite(card) },
                                onGoogleSync = { viewModel.syncCardToGoogleContacts(context, card) }
                            )
                        } else {
                            Column {
                                DigitalBusinessCardView(
                                    card = card,
                                    onFlipClick = { onSelectCard(card) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                CardQuickActionBar(
                                    card = card,
                                    onCall = {
                                        if (card.phone.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${card.phone}"))
                                            context.startActivity(intent)
                                        }
                                    },
                                    onWhatsApp = {
                                        val targetPhone = card.phone.ifBlank { card.secondaryPhone }
                                        val cleanPhone = targetPhone.replace(Regex("[^0-9+]"), "").let {
                                            if (it.startsWith("+")) it.substring(1) else it
                                        }
                                        try {
                                            val uri = if (cleanPhone.isNotBlank()) {
                                                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                                            } else {
                                                Uri.parse("https://api.whatsapp.com/send")
                                            }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        } catch (e: Exception) {}
                                    },
                                    onEmail = {
                                        if (card.email.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${card.email}"))
                                            context.startActivity(intent)
                                        }
                                    },
                                    onToggleFav = { viewModel.toggleFavorite(card) },
                                    onGoogleSync = { viewModel.syncCardToGoogleContacts(context, card) },
                                    onShare = { sharingCard = card }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to Add New Card
        FloatingActionButton(
            onClick = onNavigateToNewCard,
            containerColor = CardMateTealPrimary,
            contentColor = Color(0xFF042F2E),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp)
                .testTag("add_card_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create New Card",
                modifier = Modifier.size(24.dp)
            )
        }

        // Share Bottom Sheet (vCard & Image Export)
        sharingCard?.let { cardToShare ->
            CardShareBottomSheet(
                card = cardToShare,
                isBangla = isBangla,
                onDismiss = { sharingCard = null }
            )
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
private fun CategoryFilterBar(
    selectedCategory: String,
    customCategories: List<String> = emptyList(),
    isBangla: Boolean,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val baseOptions = listOf("All", "Favorites") + CardCategory.entries.map { it.englishName }
        val filterOptions = (baseOptions + customCategories).distinct()

        items(filterOptions) { cat ->
            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
            val displayName = when (cat) {
                "All" -> if (isBangla) "সবগুলো" else "All"
                "Favorites" -> if (isBangla) "★ পছন্দের" else "★ Favorites"
                else -> {
                    val enumCat = CardCategory.entries.find { it.englishName.equals(cat, ignoreCase = true) }
                    if (isBangla && enumCat != null) enumCat.banglaName else cat
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCategorySelected(cat) }
            ) {
                Text(
                    text = displayName,
                    color = if (isSelected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CardQuickActionBar(
    card: BusinessCard,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit = {},
    onEmail: () -> Unit,
    onToggleFav: () -> Unit,
    onGoogleSync: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (card.phone.isNotBlank()) {
                IconButton(onClick = onCall, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (card.phone.isNotBlank() || card.secondaryPhone.isNotBlank()) {
                IconButton(onClick = onWhatsApp, modifier = Modifier.size(34.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (card.email.isNotBlank()) {
                IconButton(onClick = onEmail, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = CardMateCyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = onGoogleSync, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Google Sync",
                    tint = if (card.isSyncedWithGoogleContacts) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = onToggleFav, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = if (card.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (card.isFavorite) CardMateGoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactCardRow(
    card: BusinessCard,
    isBangla: Boolean,
    onSelect: () -> Unit,
    onToggleFav: () -> Unit,
    onGoogleSync: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.fullName.ifBlank { "Unknown" },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CardMateTealPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = card.category,
                            color = CardMateTealPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = card.displaySubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (card.phone.isNotBlank()) {
                    Text(
                        text = card.phone,
                        color = CardMateCyanAccent,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.isSyncedWithGoogleContacts) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Synced",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = onToggleFav, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (card.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (card.isFavorite) CardMateGoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
