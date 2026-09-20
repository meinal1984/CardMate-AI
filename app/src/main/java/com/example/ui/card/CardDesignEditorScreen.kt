package com.example.ui.card

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessCard
import com.example.data.model.CardBgPattern
import com.example.data.model.CardDesignPreset
import com.example.data.model.CardFontFamily
import com.example.data.model.CardLayoutStyle
import com.example.data.model.CardTemplate
import com.example.image.CardImageProcessor
import com.example.image.CardThemeColorExtractor
import com.example.ui.components.DigitalBusinessCardView
import com.example.ui.components.resolveFontFamily
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel

// Curated Vibrant Color Palettes for Business Cards
data class ColorSwatch(val name: String, val hex: Long)

val PRIMARY_BG_SWATCHES = listOf(
    ColorSwatch("Obsidian", 0xFF0F172A),
    ColorSwatch("Dark Slate", 0xFF1E293B),
    ColorSwatch("Charcoal", 0xFF18181B),
    ColorSwatch("Deep Onyx", 0xFF09090B),
    ColorSwatch("Deep Navy", 0xFF0B192C),
    ColorSwatch("Midnight Blue", 0xFF172554),
    ColorSwatch("Emerald Deep", 0xFF042F2E),
    ColorSwatch("Forest Teal", 0xFF064E3B),
    ColorSwatch("Royal Purple", 0xFF3B0764),
    ColorSwatch("Dark Indigo", 0xFF1E1B4B),
    ColorSwatch("Burgundy Crimson", 0xFF450A0A),
    ColorSwatch("Rich Espresso", 0xFF292524),
    ColorSwatch("Titanium Grey", 0xFF334155),
    ColorSwatch("Clean Snow", 0xFFF8FAFC),
    ColorSwatch("Warm Cream", 0xFFFFFBEB)
)

val SECONDARY_BG_SWATCHES = listOf(
    ColorSwatch("Slate Light", 0xFF1E293B),
    ColorSwatch("Dark Indigo", 0xFF312E81),
    ColorSwatch("Deep Teal", 0xFF115E59),
    ColorSwatch("Zinc Charcoal", 0xFF27272A),
    ColorSwatch("Navy Accent", 0xFF1E3A8A),
    ColorSwatch("Royal Violet", 0xFF581C87),
    ColorSwatch("Emerald Pine", 0xFF134E4A),
    ColorSwatch("Crimson Dark", 0xFF7F1D1D),
    ColorSwatch("Bronze Earth", 0xFF44403C),
    ColorSwatch("Neutral Graphite", 0xFF3F3F46),
    ColorSwatch("Soft Cloud", 0xFFE2E8F0),
    ColorSwatch("Opal Pure", 0xFFFFFFFF)
)

val ACCENT_SWATCHES = listOf(
    ColorSwatch("Teal Mint", 0xFF2DD4BF),
    ColorSwatch("Electric Cyan", 0xFF38BDF8),
    ColorSwatch("Imperial Gold", 0xFFEAB308),
    ColorSwatch("Warm Amber", 0xFFF59E0B),
    ColorSwatch("Rose Coral", 0xFFF43F5E),
    ColorSwatch("Neon Green", 0xFF10B981),
    ColorSwatch("Cyber Violet", 0xFFA855F7),
    ColorSwatch("Sky Blue", 0xFF60A5FA),
    ColorSwatch("Champagne", 0xFFFDE68A),
    ColorSwatch("Vivid Orange", 0xFFFB923C),
    ColorSwatch("Silver Mist", 0xFFCBD5E1),
    ColorSwatch("Pure White", 0xFFFFFFFF)
)

val TEXT_SWATCHES = listOf(
    ColorSwatch("Pure White", 0xFFFFFFFF),
    ColorSwatch("Snow Off-White", 0xFFF8FAFC),
    ColorSwatch("Cream Light", 0xFFFEF08A),
    ColorSwatch("Ice Blue", 0xFFE0E7FF),
    ColorSwatch("Soft Cyan", 0xFFCCFBF1),
    ColorSwatch("Charcoal Dark", 0xFF0F172A),
    ColorSwatch("Pure Black", 0xFF000000)
)

enum class DesignEditorTab(val titleEn: String, val titleBn: String, val icon: ImageVector) {
    THEMES("Themes", "থিম প্রিসেট", Icons.Default.AutoAwesome),
    COLORS("Colors", "রং ও প্যালেট", Icons.Default.Palette),
    FONTS("Typography", "ফন্ট ও টাইপো", Icons.Default.TextFields),
    LAYOUT("Layout", "লেআউট ও বর্ডার", Icons.Default.ViewQuilt),
    ELEMENTS("Badges", "উপাদানসমূহ", Icons.Default.Layers)
}

@Composable
fun CardDesignEditorScreen(
    viewModel: CardViewModel,
    card: BusinessCard,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val savedPresets by viewModel.allDesignPresets.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Local mutable state for real-time live preview
    var editedCard by remember(card) { mutableStateOf(card) }
    var isFlipped by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(DesignEditorTab.THEMES) }

    // Dialog state for saving custom theme preset
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("card_design_editor_screen")
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("design_editor_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = if (isBangla) "কার্ড ডিজাইন স্টুডিও" else "Card Design Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = editedCard.fullName.ifBlank { "Virtual Card" },
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMateCyanAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Reset Button
                IconButton(
                    onClick = {
                        val defaultTemplate = CardTemplate.fromId(card.cardLayoutTemplate)
                        editedCard = editedCard.copy(
                            customPrimaryBgColor = null,
                            customSecondaryBgColor = null,
                            customAccentColor = null,
                            customTextColor = null,
                            fontFamilyType = "sans_serif",
                            layoutStyle = "modern_floating",
                            bgPattern = "gradient",
                            cornerRadiusDp = 18,
                            borderStyle = "subtle",
                            textAlignment = "left",
                            showQrBadge = true,
                            showNfcBadge = true,
                            showAvatar = true,
                            showCategoryBadge = true
                        )
                        Toast.makeText(
                            context,
                            if (isBangla) "ডিফল্ট সেটিংসে রিসেট হয়েছে" else "Reset to default styling",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.testTag("design_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Save to Room DB Action
                Button(
                    onClick = {
                        viewModel.updateCardDesign(editedCard) {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("design_save_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBangla) "সেভ করুন" else "Save",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // --- Sticky Top Section: Real-Time Live Interactive Card Preview ---
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Card View with Live Styling
                DigitalBusinessCardView(
                    card = editedCard,
                    isFlipped = isFlipped,
                    onFlipClick = { isFlipped = !isFlipped },
                    showSyncBadges = true,
                    modifier = Modifier.testTag("live_card_preview")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Flip and Info Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardMateTealPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { isFlipped = !isFlipped }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFlipped) (if (isBangla) "সামনের দিক দেখুন" else "Viewing Back (vCard QR)")
                                else (if (isBangla) "পিছনের দিক দেখুন (QR)" else "Viewing Front (Tap to flip)"),
                                color = CardMateTealPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = CardLayoutStyle.fromId(editedCard.layoutStyle).let {
                            if (isBangla) it.displayNameBn else it.displayNameEn
                        } + " • " + CardFontFamily.fromId(editedCard.fontFamilyType).let {
                            if (isBangla) it.displayNameBn else it.displayNameEn
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Category Tabs ---
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = CardMateTealPrimary,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = CardMateTealPrimary,
                    height = 3.dp
                )
            }
        ) {
            DesignEditorTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) tab.titleBn else tab.titleEn,
                                fontSize = 12.5.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                )
            }
        }

        // --- Tab Content Panels (Scrollable) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                DesignEditorTab.THEMES -> ThemesTabContent(
                    editedCard = editedCard,
                    savedPresets = savedPresets,
                    isBangla = isBangla,
                    onApplyTemplate = { template ->
                        editedCard = editedCard.copy(
                            cardLayoutTemplate = template.id,
                            customPrimaryBgColor = template.primaryBgColor,
                            customSecondaryBgColor = template.secondaryBgColor,
                            customAccentColor = template.accentColor,
                            customTextColor = template.textColor
                        )
                    },
                    onApplyPreset = { preset ->
                        editedCard = editedCard.copy(
                            customPrimaryBgColor = preset.primaryBgColor,
                            customSecondaryBgColor = preset.secondaryBgColor,
                            customAccentColor = preset.accentColor,
                            customTextColor = preset.textColor,
                            fontFamilyType = preset.fontFamilyType,
                            layoutStyle = preset.layoutStyle,
                            bgPattern = preset.bgPattern,
                            cornerRadiusDp = preset.cornerRadiusDp,
                            borderStyle = preset.borderStyle,
                            textAlignment = preset.textAlignment,
                            showQrBadge = preset.showQrBadge,
                            showNfcBadge = preset.showNfcBadge
                        )
                        Toast.makeText(
                            context,
                            if (isBangla) "'${preset.name}' থিম প্রয়োগ করা হয়েছে" else "Applied '${preset.name}' theme",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onOpenSaveDialog = { showSavePresetDialog = true },
                    onDeletePreset = { presetId -> viewModel.deleteCustomDesignPreset(presetId) },
                    onExtractOriginalCardTheme = {
                        coroutineScope.launch {
                            val uriStr = editedCard.cardFrontImageUri
                            if (!uriStr.isNullOrBlank()) {
                                try {
                                    val bitmap = CardImageProcessor.loadBitmapSafely(context, uriStr)
                                    if (bitmap != null) {
                                        val extracted = CardThemeColorExtractor.extractThemeFromBitmap(bitmap)
                                        editedCard = editedCard.copy(
                                            customPrimaryBgColor = extracted.primaryBgColor,
                                            customSecondaryBgColor = extracted.secondaryBgColor,
                                            customAccentColor = extracted.accentColor,
                                            customTextColor = extracted.textColor,
                                            cardLayoutTemplate = extracted.matchedTemplate,
                                            layoutStyle = extracted.layoutStyle,
                                            bgPattern = extracted.bgPattern
                                        )
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "✨ স্ক্যান করা আসল কার্ডের থিম প্রয়োগ করা হয়েছে!" else "✨ Matched original scanned card theme!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                )

                DesignEditorTab.COLORS -> ColorsTabContent(
                    editedCard = editedCard,
                    isBangla = isBangla,
                    onUpdateCard = { updated -> editedCard = updated }
                )

                DesignEditorTab.FONTS -> TypographyTabContent(
                    editedCard = editedCard,
                    isBangla = isBangla,
                    onUpdateCard = { updated -> editedCard = updated }
                )

                DesignEditorTab.LAYOUT -> LayoutTabContent(
                    editedCard = editedCard,
                    isBangla = isBangla,
                    onUpdateCard = { updated -> editedCard = updated }
                )

                DesignEditorTab.ELEMENTS -> ElementsTabContent(
                    editedCard = editedCard,
                    isBangla = isBangla,
                    onUpdateCard = { updated -> editedCard = updated }
                )
            }
        }

        // --- Bottom Sticky Save Bar ---
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.8f)
                ) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }

                Button(
                    onClick = {
                        viewModel.updateCardDesign(editedCard) {
                            onBack()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("apply_and_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla) "ডাটাবেজে সংরক্ষণ করুন" else "Save to Database",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // --- Save Custom Theme Dialog ---
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = {
                Text(
                    text = if (isBangla) "কাস্টম থিম সংরক্ষণ করুন" else "Save Custom Theme Preset",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBangla) "বর্তমান কালার, ফন্ট ও লেআউট স্টাইল একটি নতুন রিইউজেবল থিম হিসেবে লোকাল Room ডাটাবেজে সংরক্ষণ করুন।"
                        else "Save your current colors, font families, and layout configurations into the local Room database to reuse on any card.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text(if (isBangla) "থিমের নাম" else "Theme Name") },
                        placeholder = { Text("e.g. My Executive Blue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPresetName.ifBlank { "Custom Theme ${System.currentTimeMillis() % 1000}" }
                        viewModel.saveCustomDesignPreset(
                            name = name,
                            primaryBg = editedCard.effectivePrimaryBgColor,
                            secondaryBg = editedCard.effectiveSecondaryBgColor,
                            accent = editedCard.effectiveAccentColor,
                            text = editedCard.effectiveTextColor,
                            fontFamily = editedCard.fontFamilyType,
                            layoutStyle = editedCard.layoutStyle,
                            bgPattern = editedCard.bgPattern,
                            cornerRadiusDp = editedCard.cornerRadiusDp,
                            borderStyle = editedCard.borderStyle,
                            textAlignment = editedCard.textAlignment
                        )
                        showSavePresetDialog = false
                        newPresetName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary)
                ) {
                    Text(if (isBangla) "সংরক্ষণ" else "Save Theme", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

// ---------------- TAB 1: THEMES & PRESETS ----------------
@Composable
private fun ThemesTabContent(
    editedCard: BusinessCard,
    savedPresets: List<CardDesignPreset>,
    isBangla: Boolean,
    onApplyTemplate: (CardTemplate) -> Unit,
    onApplyPreset: (CardDesignPreset) -> Unit,
    onOpenSaveDialog: () -> Unit,
    onDeletePreset: (Long) -> Unit,
    onExtractOriginalCardTheme: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Save current as custom preset banner
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = CardMateTealPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = CardMateTealPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isBangla) "বর্তমান ডিজাইন সেভ করুন" else "Save Current Style as Preset",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = if (isBangla) "অন্যান্য কার্ডে সহজে ব্যবহারের জন্য" else "Reusable across your card library",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onOpenSaveDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBangla) "সেভ" else "Save", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Extract Original Theme from Scanned Photo (if card was scanned from photo)
        if (!editedCard.cardFrontImageUri.isNullOrBlank() && onExtractOriginalCardTheme != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardMateTealPrimary.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = CardMateTealPrimary.copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = CardMateTealPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla) "স্ক্যান করা কার্ডের হুবহু থিম" else "Match Scanned Card Theme",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBangla) "আসল কার্ডের ছবি থেকে কালার ও স্টাইল এক্সট্র্যাক্ট করুন" else "Re-extract exact colors & styles from card photo",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onExtractOriginalCardTheme,
                        colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "ম্যাচ করুন" else "Match", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Saved Themes from Local Room Database
        if (savedPresets.isNotEmpty()) {
            Text(
                text = if (isBangla) "সংরক্ষিত কাস্টম থিমসমূহ (Room DB)" else "Saved Custom Themes (Local DB)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CardMateTealPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            savedPresets.forEach { preset ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onApplyPreset(preset) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Mini Color preview palette
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                            ) {
                                Box(modifier = Modifier.size(16.dp, 24.dp).background(Color(preset.primaryBgColor)))
                                Box(modifier = Modifier.size(16.dp, 24.dp).background(Color(preset.secondaryBgColor)))
                                Box(modifier = Modifier.size(16.dp, 24.dp).background(Color(preset.accentColor)))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = preset.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text(
                                    text = "${preset.layoutStyle} • ${preset.fontFamilyType}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { onApplyPreset(preset) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(if (isBangla) "প্রয়োগ" else "Apply", fontSize = 11.5.sp)
                            }
                            if (preset.isUserCreated) {
                                IconButton(onClick = { onDeletePreset(preset.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Built-in Curated Templates
        Text(
            text = if (isBangla) "ডিজাইনার প্রিসেট কালেকশন" else "Curated Designer Themes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        CardTemplate.entries.forEach { template ->
            val isSelected = editedCard.cardLayoutTemplate == template.id &&
                    editedCard.customPrimaryBgColor == template.primaryBgColor

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CardMateTealPrimary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onApplyTemplate(template) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Swatch Triple
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        ) {
                            Box(modifier = Modifier.size(18.dp, 26.dp).background(Color(template.primaryBgColor)))
                            Box(modifier = Modifier.size(18.dp, 26.dp).background(Color(template.secondaryBgColor)))
                            Box(modifier = Modifier.size(18.dp, 26.dp).background(Color(template.accentColor)))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isBangla) template.displayNameBn else template.displayNameEn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Accent: #${java.lang.Long.toHexString(template.accentColor).takeLast(6).uppercase()}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = CardMateTealPrimary)
                    }
                }
            }
        }
    }
}

// ---------------- TAB 2: COLOR PALETTES & GRADIENTS ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorsTabContent(
    editedCard: BusinessCard,
    isBangla: Boolean,
    onUpdateCard: (BusinessCard) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. Background Fill / Pattern Style
        Text(
            text = if (isBangla) "১. ব্যাকগ্রাউন্ড প্যাটার্ন ও ফিনিশ" else "1. Background Style & Pattern",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CardBgPattern.entries.forEach { pattern ->
                val isSelected = editedCard.bgPattern == pattern.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onUpdateCard(editedCard.copy(bgPattern = pattern.id)) },
                    label = { Text(if (isBangla) pattern.displayNameBn else pattern.displayNameEn, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardMateTealPrimary.copy(alpha = 0.25f),
                        selectedLabelColor = CardMateTealPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Primary Canvas Background Color
        Text(
            text = if (isBangla) "২. প্রাইমারি ব্যাকগ্রাউন্ড রং" else "2. Primary Background Color",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ColorSwatchRow(
            swatches = PRIMARY_BG_SWATCHES,
            selectedHex = editedCard.effectivePrimaryBgColor,
            onSelect = { hex -> onUpdateCard(editedCard.copy(customPrimaryBgColor = hex)) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Secondary Gradient Background Color
        Text(
            text = if (isBangla) "৩. সেকেন্ডারি গ্রেডিয়েন্ট রং" else "3. Secondary Gradient Color",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ColorSwatchRow(
            swatches = SECONDARY_BG_SWATCHES,
            selectedHex = editedCard.effectiveSecondaryBgColor,
            onSelect = { hex -> onUpdateCard(editedCard.copy(customSecondaryBgColor = hex)) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Accent & Brand Highlight Color
        Text(
            text = if (isBangla) "৪. ব্র্যান্ড অ্যাকসেন্ট রং (আইকন ও হাইলাইট)" else "4. Accent & Highlight Color",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ColorSwatchRow(
            swatches = ACCENT_SWATCHES,
            selectedHex = editedCard.effectiveAccentColor,
            onSelect = { hex -> onUpdateCard(editedCard.copy(customAccentColor = hex)) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Main Typography Text Color
        Text(
            text = if (isBangla) "৫. লেখার টেক্সট রং" else "5. Typography Text Color",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ColorSwatchRow(
            swatches = TEXT_SWATCHES,
            selectedHex = editedCard.effectiveTextColor,
            onSelect = { hex -> onUpdateCard(editedCard.copy(customTextColor = hex)) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Card Border & Glow Effect
        Text(
            text = if (isBangla) "৬. বর্ডার ও নিয়ন গ্লো এফেক্ট" else "6. Card Border & Glow Effect",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "none" to (if (isBangla) "বর্ডার নেই" else "None"),
                "subtle" to (if (isBangla) "সাবটল ১dp" else "Subtle 1dp"),
                "bold" to (if (isBangla) "বোল্ড ২dp" else "Bold 2dp"),
                "glow" to (if (isBangla) "নিয়ন গ্লো" else "Neon Glow")
            ).forEach { (styleKey, label) ->
                val isSelected = editedCard.borderStyle == styleKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onUpdateCard(editedCard.copy(borderStyle = styleKey)) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardMateTealPrimary.copy(alpha = 0.25f),
                        selectedLabelColor = CardMateTealPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    swatches: List<ColorSwatch>,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(swatches) { swatch ->
            val isSelected = selectedHex == swatch.hex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(swatch.hex) }
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(swatch.hex))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (swatch.hex == 0xFFFFFFFFL || swatch.hex == 0xFFF8FAFC || swatch.hex == 0xFFFFFBEBL) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = swatch.name,
                    fontSize = 9.5.sp,
                    color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------- TAB 3: TYPOGRAPHY & FONTS ----------------
@Composable
private fun TypographyTabContent(
    editedCard: BusinessCard,
    isBangla: Boolean,
    onUpdateCard: (BusinessCard) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isBangla) "ফন্ট ফ্যামিলি নির্বাচন করুন" else "Select Font Family",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        CardFontFamily.entries.forEach { fontOption ->
            val isSelected = editedCard.fontFamilyType == fontOption.id
            val resolvedFamily = resolveFontFamily(fontOption.id)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CardMateTealPrimary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onUpdateCard(editedCard.copy(fontFamilyType = fontOption.id)) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) fontOption.displayNameBn else fontOption.displayNameEn,
                            fontWeight = FontWeight.Bold,
                            fontFamily = resolvedFamily,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "CardMate Executive • NeuralSphere AI Ltd.",
                            fontFamily = resolvedFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = CardMateTealPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Text Alignment Selection
        Text(
            text = if (isBangla) "টেক্সট অ্যালাইনমেন্ট" else "Text Alignment",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "left" to (if (isBangla) "বামপাশে (Left)" else "Left Aligned"),
                "center" to (if (isBangla) "মাঝখানে (Center)" else "Center Aligned"),
                "right" to (if (isBangla) "ডানপাশে (Right)" else "Right Aligned")
            ).forEach { (alignKey, label) ->
                val isSelected = editedCard.textAlignment == alignKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onUpdateCard(editedCard.copy(textAlignment = alignKey)) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardMateTealPrimary.copy(alpha = 0.25f),
                        selectedLabelColor = CardMateTealPrimary
                    )
                )
            }
        }
    }
}

// ---------------- TAB 4: LAYOUT STYLES & SHAPES ----------------
@Composable
private fun LayoutTabContent(
    editedCard: BusinessCard,
    isBangla: Boolean,
    onUpdateCard: (BusinessCard) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isBangla) "লেআউট আর্কিটেকচার" else "Layout Architecture & Structure",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        CardLayoutStyle.entries.forEach { style ->
            val isSelected = editedCard.layoutStyle == style.id

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CardMateTealPrimary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onUpdateCard(editedCard.copy(layoutStyle = style.id)) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) style.displayNameBn else style.displayNameEn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isBangla) style.descriptionBn else style.descriptionEn,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }

                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = CardMateTealPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Corner Radius Slider
        Text(
            text = (if (isBangla) "কার্ড কর্নার রেডিয়াস (গোলাকার কোণ): " else "Corner Radius: ") + "${editedCard.cornerRadiusDp} dp",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = editedCard.cornerRadiusDp.toFloat(),
            onValueChange = { onUpdateCard(editedCard.copy(cornerRadiusDp = it.toInt())) },
            valueRange = 0f..28f,
            steps = 6,
            colors = SliderDefaults.colors(
                thumbColor = CardMateTealPrimary,
                activeTrackColor = CardMateTealPrimary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0 to "Sharp (0dp)", 8 to "Subtle (8dp)", 16 to "Modern (16dp)", 22 to "Pill (22dp)").forEach { (r, label) ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = if (editedCard.cornerRadiusDp == r) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onUpdateCard(editedCard.copy(cornerRadiusDp = r)) }
                )
            }
        }
    }
}

// ---------------- TAB 5: ELEMENTS & BADGES TOGGLES ----------------
@Composable
private fun ElementsTabContent(
    editedCard: BusinessCard,
    isBangla: Boolean,
    onUpdateCard: (BusinessCard) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isBangla) "কার্ডের উপাদান ও ব্যাজ ভিজিবিলিটি" else "Card Elements & Badge Visibility",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isBangla) "আপনার কার্ডে কোন কোন উপাদান দৃশ্যমান থাকবে তা নিয়ন্ত্রণ করুন"
            else "Toggle the visual indicators and badges rendered on your virtual card",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        ElementToggleItem(
            title = if (isBangla) "প্রোফাইল ছবি / ইনিশিয়াল ব্যাজ" else "Profile Photo / Monogram Avatar",
            subtitle = if (isBangla) "কার্ডের সামনে ছবি বা আদ্যক্ষরের বৃত্তাকার ব্যাজ" else "Show uploaded photo or initials avatar on card front",
            checked = editedCard.showAvatar,
            onCheckedChange = { onUpdateCard(editedCard.copy(showAvatar = it)) }
        )

        ElementToggleItem(
            title = if (isBangla) "ক্যাটাগরি ট্যাগ ব্যাজ" else "Category Pill Badge",
            subtitle = if (isBangla) "যেমন: TECH, CORPORATE, CLIENTS" else "Display category badge pill on top header",
            checked = editedCard.showCategoryBadge,
            onCheckedChange = { onUpdateCard(editedCard.copy(showCategoryBadge = it)) }
        )

        ElementToggleItem(
            title = if (isBangla) "কুইক QR কোড ফ্লিপ ট্রিগার" else "Quick QR Code Badge",
            subtitle = if (isBangla) "কার্ড ফ্লিপ করে সরাসরি স্ক্যানযোগ্য ডিজিটাল QR কোড প্রদর্শন" else "Show top-right QR badge to flip to vCard QR code",
            checked = editedCard.showQrBadge,
            onCheckedChange = { onUpdateCard(editedCard.copy(showQrBadge = it)) }
        )

        ElementToggleItem(
            title = if (isBangla) "NFC চিপ ইন্ডিকেটর" else "NFC Contactless Chip Indicator",
            subtitle = if (isBangla) "কার্ডে NFC সক্রিয় থাকার দৃশ্যমান প্রতীক" else "Show contactless NFC wave icon on card front",
            checked = editedCard.showNfcBadge,
            onCheckedChange = { onUpdateCard(editedCard.copy(showNfcBadge = it)) }
        )
    }
}

@Composable
private fun ElementToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text(text = subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CardMateTealPrimary,
                    checkedTrackColor = CardMateTealPrimary.copy(alpha = 0.4f)
                )
            )
        }
    }
}
