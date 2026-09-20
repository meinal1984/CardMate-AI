package com.example.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ai.DuplicateDetectionHelper
import com.example.ai.DuplicateMatch
import com.example.data.model.BusinessCard
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.CardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardMateAiAssistantBottomSheet(
    viewModel: CardViewModel,
    targetCard: BusinessCard? = null,
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onOpenCardDetail: ((BusinessCard) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedAssistantTab by remember { mutableIntStateOf(initialTab) } // 0: Duplicates, 1: Categorization, 2: Follow-up, 3: AI Chat & Maps Grounding
    var duplicateMatches by remember { mutableStateOf<List<DuplicateMatch>>(emptyList()) }
    var isAuditingDuplicates by remember { mutableStateOf(false) }
    var activeMergeDialogMatch by remember { mutableStateOf<DuplicateMatch?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    // Run duplicate auditor on load
    LaunchedEffect(allCards) {
        isAuditingDuplicates = true
        withContext(Dispatchers.Default) {
            val list = DuplicateDetectionHelper.findPotentialDuplicates(allCards)
            duplicateMatches = list
            isAuditingDuplicates = false
        }
    }

    // Sync active card and restore its persistent chat history
    LaunchedEffect(targetCard?.id) {
        if (targetCard != null) {
            viewModel.prepareChatForCard(targetCard)
        } else {
            viewModel.prepareGeneralChat()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("ai_assistant_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = CardMateTealPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBangla) "CardMate AI Assistant" else "CardMate AI Assistant",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isBangla) "স্মার্ট নেটওয়ার্কিং, কন্টাক্ট অ্যাকশন ও ফলো-আপ ইঞ্জিন" else "Smart Networking, Contact Actions & Follow-up Engine",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Beautiful Uncramped Pill Tabs
            val tabs = listOf(
                if (isBangla) "🤖 AI সহকারী" else "🤖 CardMate AI",
                if (isBangla) "✍️ ফলো-আপ বার্তা" else "✍️ Follow-up",
                if (isBangla) "🔍 ডুপ্লিকেট (${duplicateMatches.size})" else "🔍 Duplicates (${duplicateMatches.size})",
                if (isBangla) "🏷️ ক্যাটাগরি" else "🏷️ Categories"
            )

            ScrollableTabRow(
                selectedTabIndex = selectedAssistantTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedAssistantTab == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) BorderStroke(1.dp, CardMateCyanAccent) else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedAssistantTab = index }
                            .testTag("ai_sheet_tab_$index")
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            when (selectedAssistantTab) {
                0 -> {
                    // MULTI-TURN GEMINI CHAT & GOOGLE MAPS GROUNDING & DIRECT CONTACT ACTIONS
                    GeminiMultiTurnChatView(
                        viewModel = viewModel,
                        isBangla = isBangla,
                        allCards = allCards,
                        onOpenCardDetail = onOpenCardDetail
                    )
                }
                1 -> {
                    // FOLLOW-UP & MESSAGING GENERATOR
                    AiFollowUpAndInsightsContent(
                        viewModel = viewModel,
                        isBangla = isBangla,
                        allCards = allCards,
                        initialCard = targetCard ?: allCards.firstOrNull()
                    )
                }
                2 -> {
                    // DUPLICATE AUDITOR TAB
                    AiDuplicateAuditorContent(
                        isBangla = isBangla,
                        isLoading = isAuditingDuplicates,
                        matches = duplicateMatches,
                        onRefresh = {
                            scope.launch {
                                isAuditingDuplicates = true
                                val list = withContext(Dispatchers.Default) {
                                    DuplicateDetectionHelper.findPotentialDuplicates(allCards)
                                }
                                duplicateMatches = list
                                isAuditingDuplicates = false
                            }
                        },
                        onInspect = { match -> activeMergeDialogMatch = match },
                        onMergeDirect = { match ->
                            viewModel.mergeDuplicateCards(match.cardA, match.cardB, match.suggestedMergedCard)
                            duplicateMatches = duplicateMatches.filter { it != match }
                            Toast.makeText(
                                context,
                                if (isBangla) "কন্টাক্ট দুটি সফলভাবে মার্জ করা হয়েছে!" else "Contacts merged successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                3 -> {
                    // SMART CATEGORIZATION & CUSTOM CATEGORY CREATOR
                    AiCategorizationContent(
                        viewModel = viewModel,
                        isBangla = isBangla,
                        cards = allCards,
                        customCategories = customCategories,
                        onAddCustomCategoryClick = { showAddCategoryDialog = true }
                    )
                }
            }
        }
    }

    // Duplicate Merge Detailed Inspection Dialog
    activeMergeDialogMatch?.let { match ->
        DuplicateMergeDialog(
            match = match,
            isBangla = isBangla,
            onMerge = { mergedCard ->
                viewModel.mergeDuplicateCards(match.cardA, match.cardB, mergedCard)
                duplicateMatches = duplicateMatches.filter { it != match }
                activeMergeDialogMatch = null
                Toast.makeText(
                    context,
                    if (isBangla) "কন্টাক্ট দুটি সফলভাবে মার্জ করা হয়েছে!" else "Contacts merged successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onKeepSeparate = {
                activeMergeDialogMatch = null
                duplicateMatches = duplicateMatches.filter { it != match }
                Toast.makeText(
                    context,
                    if (isBangla) "কন্টাক্ট দুটি আলাদা রাখা হয়েছে" else "Contacts kept separate",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { activeMergeDialogMatch = null }
        )
    }

    // Add Custom Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                newCategoryInput = ""
            },
            title = {
                Text(
                    text = if (isBangla) "নতুন কাস্টম ক্যাটাগরি তৈরি করুন" else "Create Custom Category",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBangla)
                            "যেমন: Government, Engineering, Consultancy, Freelance ইত্যাদি।"
                        else
                            "e.g. Government, Engineering, Consultancy, Freelance, etc.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        placeholder = { Text(if (isBangla) "ক্যাটাগরির নাম লিখুন..." else "Category Name...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryInput.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.addCustomCategory(trimmed)
                            showAddCategoryDialog = false
                            newCategoryInput = ""
                            Toast.makeText(
                                context,
                                if (isBangla) "ক্যাটাগরি '$trimmed' তৈরি হয়েছে" else "Category '$trimmed' added",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary)
                ) {
                    Text(if (isBangla) "যুক্ত করুন" else "Add Category", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCategoryDialog = false
                    newCategoryInput = ""
                }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun AiDuplicateAuditorContent(
    isBangla: Boolean,
    isLoading: Boolean,
    matches: List<DuplicateMatch>,
    onRefresh: () -> Unit,
    onInspect: (DuplicateMatch) -> Unit,
    onMergeDirect: (DuplicateMatch) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isBangla) "AI ডুপ্লিকেট স্ক্যানার" else "AI Duplicate Auditor",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBangla) "একই ব্যক্তির নাম বা কোম্পানির বৈচিত্র্য যাচাই" else "Finds subtle name, company & contact variations",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Scan Duplicates", tint = CardMateTealPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CardMateTealPrimary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isBangla) "AI কার্ডের মিলসমূহ বিশ্লেষণ করছে..." else "AI analyzing contact similarity...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp
                    )
                }
            }
        } else if (matches.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isBangla) "কোনো ডুপ্লিকেট কন্টাক্ট পাওয়া যায়নি!" else "No Duplicates Detected!",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBangla) "আপনার কার্ড ডিরেক্টরি সম্পূর্ণ পরিষ্কার ও সুসংগঠিত।" else "All contacts are unique, verified and organized.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matches) { match ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onInspect(match) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (match.matchScore >= 90) CardMateTealPrimary else CardMateGoldAccent
                                ) {
                                    Text(
                                        text = "${match.matchScore}% Match",
                                        color = Color(0xFF042F2E),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onInspect(match) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (isBangla) "যাচাই" else "Inspect", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { onMergeDirect(match) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (isBangla) "মার্জ" else "Merge", color = Color(0xFF042F2E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "1. ${match.cardA.fullName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = match.cardA.company.ifBlank { match.cardA.jobTitle }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "2. ${match.cardB.fullName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = match.cardB.company.ifBlank { match.cardB.jobTitle }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (match.matchReasons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 ${match.matchReasons.first()}",
                                    color = CardMateTealPrimary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiCategorizationContent(
    viewModel: CardViewModel,
    isBangla: Boolean,
    cards: List<BusinessCard>,
    customCategories: List<String>,
    onAddCustomCategoryClick: () -> Unit
) {
    val predefinedCategories = remember {
        listOf(
            "Government", "Engineering", "Consultancy", "Tech & IT",
            "Corporate", "Finance & Banking", "Healthcare & Medical",
            "Clients", "Partners", "Vendors", "Legal & Advisory",
            "Education & Training", "Real Estate", "Media & Marketing"
        )
    }

    val allAvailableCategories = remember(customCategories) {
        (predefinedCategories + customCategories).distinct()
    }

    var selectedCategoryFilter by remember { mutableStateOf("All") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isBangla) "স্মার্ট ক্যাটাগরি ও ট্যাগিং" else "Intelligent Categories & Tagging",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBangla) "এআই সাজেস্টেড ও কাস্টম ক্যাটাগরি ব্যবস্থাপনা" else "AI suggested & custom categories",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onAddCustomCategoryClick,
                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("add_custom_category_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isBangla) "+ ক্যাটাগরি" else "+ Custom",
                    color = Color(0xFF042F2E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // All Category Chips
        Text(
            text = if (isBangla) "বিদ্যমান ও কাস্টম ক্যাটাগরিসমূহ:" else "Available Categories (Tap to filter):",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            allAvailableCategories.forEach { catName ->
                val isSelected = selectedCategoryFilter == catName
                val isCustom = customCategories.contains(catName)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) CardMateTealPrimary else if (isCustom) CardMateGoldAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedCategoryFilter = if (selectedCategoryFilter == catName) "All" else catName
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCustom) "★ $catName" else catName,
                            color = if (isSelected) Color(0xFF042F2E) else if (isCustom) CardMateGoldAccent else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // Cards list to assign categories quickly
        val displayedCards = if (selectedCategoryFilter == "All") cards else cards.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }

        Text(
            text = if (isBangla) "কার্ডের ক্যাটাগরি পরিবর্তন করুন (${displayedCards.size} টি কার্ড):" else "Quick Assign Category (${displayedCards.size} Cards):",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedCards) { card ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = card.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = card.displaySubtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CardMateTealPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = card.category,
                                    color = CardMateTealPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Category Assign Pills (e.g. Government, Engineering, Consultancy, etc.)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val suggestions = listOf("Government", "Engineering", "Consultancy", "Tech & IT", "Corporate") + customCategories.take(2)
                            suggestions.distinct().forEach { catOpt ->
                                val isCur = card.category.equals(catOpt, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCur) CardMateTealPrimary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            viewModel.saveCard(card.copy(category = catOpt))
                                        }
                                ) {
                                    Text(
                                        text = if (isCur) "✓ $catOpt" else catOpt,
                                        color = if (isCur) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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

@Composable
private fun AiFollowUpAndInsightsContent(
    viewModel: CardViewModel,
    isBangla: Boolean,
    allCards: List<BusinessCard>,
    initialCard: BusinessCard?
) {
    var selectedCard by remember { mutableStateOf(initialCard) }
    var cardDropdownExpanded by remember { mutableStateOf(false) }
    var generatedMessage by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var messageTone by remember { mutableStateOf("Professional") } // Professional, Friendly, Bangla Official
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (isBangla) "এআই নেটওয়ার্কিং ও ফলো-আপ বার্তা" else "AI Networking Follow-up Drafter",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isBangla) "মিটিং বা কার্ড পাওয়ার পর তাত্ক্ষণিক স্মার্ট ইমেইল ও মেসেজ তৈরি করুন" else "Draft personalized follow-up emails, WhatsApp messages & summaries",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Card Indicator & Dropdown Switcher
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { cardDropdownExpanded = true }
                    .testTag("follow_up_card_selector")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCard != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Target: ${selectedCard!!.fullName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selectedCard!!.jobTitle.ifBlank { "Professional" }} • ${selectedCard!!.company.ifBlank { "CardMate Contact" }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "কার্ড নির্বাচন করুন" else "Select Target Contact",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CardMateTealPrimary
                            )
                            Text(
                                text = if (isBangla) "ড্রাফট মেসেজ তৈরি করতে যেকোনো কন্টাক্ট বেছে নিন" else "Choose any contact from your directory",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CardMateTealPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isBangla) "পরিবর্তন" else "Change",
                                fontSize = 10.5.sp,
                                color = CardMateTealPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Contact",
                            tint = CardMateTealPrimary
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = cardDropdownExpanded,
                onDismissRequest = { cardDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                if (allCards.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isBangla) "কোনো কার্ড সংরক্ষিত নেই" else "No contacts saved yet",
                                fontSize = 12.sp
                            )
                        },
                        onClick = { cardDropdownExpanded = false }
                    )
                } else {
                    allCards.forEach { card ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = card.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                    if (card.company.isNotBlank() || card.jobTitle.isNotBlank()) {
                                        Text(
                                            text = "${card.jobTitle} • ${card.company}".trim(' ', '•'),
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedCard = card
                                cardDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tone Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Formal", "Casual/Friendly", "Bangla Official").forEach { tone ->
                val isSel = messageTone == tone
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) CardMateTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { messageTone = tone }
                ) {
                    Text(
                        text = tone,
                        color = if (isSel) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Generate Action Button
        Button(
            onClick = {
                val card = selectedCard ?: return@Button
                isGenerating = true
                generatedMessage = when (messageTone) {
                    "Bangla Official" -> """
                        শ্রদ্ধেয় ${card.fullName} মহোদয়,
                        CardMate AI-এর মাধ্যমে আপনার সাথে যোগাযোগ করতে পেরে আনন্দিত। ${card.company.ifBlank { "আপনার প্রতিষ্ঠানে" }} আপনার ${card.jobTitle.ifBlank { "কাজের" }} বিষয়ে আলোচনা করতে আগ্রহী।
                        পরবর্তী সুবিধাজনক সময়ে কথা বলার অপেক্ষায় রইলাম।
                        
                        ধন্যবাদান্তে,
                        ${viewModel.userProfile.value.fullName}
                        ${viewModel.userProfile.value.company}
                        ফোন: ${viewModel.userProfile.value.phone}
                    """.trimIndent()
                    "Casual/Friendly" -> """
                        Hi ${card.fullName.split(" ").firstOrNull() ?: card.fullName},
                        Great connecting with you and receiving your card! I'd love to learn more about your work at ${card.company.ifBlank { "your team" }}. Let's catch up over coffee or a brief call sometime this week.
                        
                        Best,
                        ${viewModel.userProfile.value.fullName}
                    """.trimIndent()
                    else -> """
                        Dear ${card.fullName},
                        It was a pleasure receiving your business card. I am reaching out to follow up regarding potential collaboration between our organizations.
                        
                        I would appreciate the opportunity to schedule a brief introductory call at your convenience.
                        
                        Kind regards,
                        ${viewModel.userProfile.value.fullName}
                        ${viewModel.userProfile.value.jobTitle}
                        ${viewModel.userProfile.value.company}
                        ${viewModel.userProfile.value.phone} | ${viewModel.userProfile.value.email}
                    """.trimIndent()
                }
                isGenerating = false
            },
            enabled = selectedCard != null && !isGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isBangla) "AI ফলো-আপ ড্রাফট তৈরি করুন" else "Generate AI Follow-up Message",
                color = Color(0xFF042F2E),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Generated Text Box with Copy & Share Actions
        if (generatedMessage.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBangla) "তৈরিকৃত বার্তা:" else "Generated Message:",
                            color = CardMateTealPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            // WhatsApp Send Button
                            IconButton(
                                onClick = {
                                    val targetPhone = selectedCard?.phone?.ifBlank { selectedCard?.secondaryPhone } ?: ""
                                    val cleanPhone = targetPhone.replace(Regex("[^0-9+]"), "").let {
                                        if (it.startsWith("+")) it.substring(1) else it
                                    }
                                    try {
                                        val uri = if (cleanPhone.isNotBlank()) {
                                            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(generatedMessage)}")
                                        } else {
                                            Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(generatedMessage)}")
                                        }
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp launch failed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp),
                                    contentDescription = "Send via WhatsApp",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Email Send Button
                            IconButton(
                                onClick = {
                                    val targetEmail = selectedCard?.email ?: ""
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:$targetEmail")
                                            putExtra(Intent.EXTRA_SUBJECT, if (isBangla) "যোগাযোগ ও পরিচিতি - CardMate" else "Connecting - Business Follow-up")
                                            putExtra(Intent.EXTRA_TEXT, generatedMessage)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Email client not found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Follow-up Message", generatedMessage)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, if (isBangla) "বার্তা কপি হয়েছে!" else "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CardMateCyanAccent, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, generatedMessage)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Follow-up"))
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = CardMateGoldAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = generatedMessage,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiNetworkInsightsAndChatContent(
    viewModel: CardViewModel,
    isBangla: Boolean,
    allCards: List<BusinessCard>,
    onOpenCardDetail: ((BusinessCard) -> Unit)?
) {
    var chatQuery by remember { mutableStateOf("") }
    var chatResponse by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val missingPhones = remember(allCards) { allCards.filter { it.phone.isBlank() && it.secondaryPhone.isBlank() } }
    val missingEmails = remember(allCards) { allCards.filter { it.email.isBlank() } }
    val topCompanies = remember(allCards) {
        allCards.mapNotNull { it.company.takeIf { c -> c.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    val suggestedQuestions = remember(isBangla) {
        if (isBangla) {
            listOf(
                "📊 কন্টাক্ট ডিরেক্টরি সামারি রিপোর্ট",
                "📞 মিসিং ফোন ও ইমেইল অডিট",
                "🏢 শীর্ষ কোম্পানি ও প্রতিষ্ঠানসমূহ",
                "💡 প্রফেশনাল নেটওয়ার্কিং স্ট্র্যাটেজি"
            )
        } else {
            listOf(
                "📊 Network Directory Summary",
                "📞 Missing Contact Audit",
                "🏢 Top Organizations & Groups",
                "💡 Networking Icebreaker Advice"
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isBangla) "CardMate AI ইন্টেলিজেন্স ও চ্যাট" else "CardMate AI Network Insights & Chat",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBangla) "আপনার কার্ড ডিরেক্টরি নিয়ে যেকোনো প্রশ্ন জিজ্ঞাসা করুন" else "Ask intelligent questions about your contacts & network",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Suggested Prompt Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestedQuestions.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            chatQuery = prompt
                            isThinking = true
                            chatResponse = when {
                                prompt.contains("Summary") || prompt.contains("সামারি") -> {
                                    if (isBangla) {
                                        """
                                        📈 **কন্টাক্ট ডিরেক্টরি সারসংক্ষেপ:**
                                        • মোট সংরক্ষিত ভিজিটিং কার্ড: ${allCards.size} টি
                                        • গুগল কন্টাক্টসে সিঙ্ক করা হয়েছে: ${allCards.count { it.isSyncedWithGoogleContacts }} টি
                                        • পছন্দ তালিকায় (Starred): ${allCards.count { it.isFavorite }} টি
                                        • শীর্ষ ক্যাটাগরিসমূহ: ${allCards.groupBy { it.category }.map { "${it.key} (${it.value.size})" }.joinToString(", ")}
                                        
                                        ✨ আপনার নেটওয়ার্ক সক্রিয় রাখতে প্রতি মাসে অন্তত ১ বার ফলো-আপ করার পরামর্শ দেওয়া হচ্ছে।
                                        """.trimIndent()
                                    } else {
                                        """
                                        📈 **Contact Directory Summary Report:**
                                        • Total Visiting Cards: ${allCards.size}
                                        • Google Contacts Synced: ${allCards.count { it.isSyncedWithGoogleContacts }}
                                        • Starred / VIP Favorites: ${allCards.count { it.isFavorite }}
                                        • Category Distribution: ${allCards.groupBy { it.category }.map { "${it.key} (${it.value.size})" }.joinToString(", ")}
                                        
                                        ✨ Recommendation: Regularly review contacts to keep collaboration alive.
                                        """.trimIndent()
                                    }
                                }
                                prompt.contains("Missing") || prompt.contains("মিসিং") -> {
                                    if (isBangla) {
                                        """
                                        ⚠️ **তথ্য ঘাটতি অডিট রিপোর্ট:**
                                        • ফোন নম্বর ছাড়া কার্ড: ${missingPhones.size} টি ${if (missingPhones.isNotEmpty()) "(${missingPhones.joinToString(", ") { it.fullName }})" else ""}
                                        • ইমেইল ছাড়া কার্ড: ${missingEmails.size} টি
                                        
                                        💡 কার্ড ডিটেইলসে গিয়ে দ্রুত এডিট করে সম্পূর্ণ তথ্য সংরক্ষণ করুন।
                                        """.trimIndent()
                                    } else {
                                        """
                                        ⚠️ **Missing Information Audit:**
                                        • Cards without phone number: ${missingPhones.size} ${if (missingPhones.isNotEmpty()) "(${missingPhones.joinToString(", ") { it.fullName }})" else ""}
                                        • Cards without email address: ${missingEmails.size}
                                        
                                        💡 Tap on any card in the list to enrich missing fields.
                                        """.trimIndent()
                                    }
                                }
                                prompt.contains("Top") || prompt.contains("কোম্পানি") -> {
                                    val companyList = if (topCompanies.isEmpty()) "কোনো কোম্পানি এখনো পাওয়া যায়নি" else topCompanies.joinToString("\n") { "• ${it.first}: ${it.second} জন প্রতিনিধি" }
                                    if (isBangla) {
                                        """
                                        🏢 **শীর্ষ নেটওয়ার্ক পার্টনার ও কোম্পানি:**
                                        $companyList
                                        
                                        🤝 একাধিক প্রতিনিধি থাকা প্রতিষ্ঠানে গ্রুপ মিটিং বা কর্পোরেট টাই-আপ করা সহজ হতে পারে।
                                        """.trimIndent()
                                    } else {
                                        """
                                        🏢 **Top Partner Organizations:**
                                        $companyList
                                        
                                        🤝 Organizations with multiple contacts offer great avenues for corporate partnerships.
                                        """.trimIndent()
                                    }
                                }
                                else -> {
                                    if (isBangla) {
                                        """
                                        💡 **প্রফেশনাল নেটওয়ার্কিং স্ট্র্যাটেজি টিপস:**
                                        1. **কার্ড প্রাপ্তির ২৪-৪৮ ঘণ্টার মধ্যে যোগাযোগ**: মিটিংয়ের পর অবিলম্বে ফলো-আপ ইমেইল বা হোয়াটসঅ্যাপ সম্ভাষণ পাঠান।
                                        2. **ব্যক্তিগত নোট যুক্ত করুন**: কার্ডের 'নোট' অপশনে কোথায় দেখা হয়েছিল এবং কী আলোচনা হয়েছিল তা লিখে রাখুন।
                                        3. **ডিজিটাল কার্ড আদান-প্রদান**: কার্ড ডিটেইলসের কিউআর কোড বা হোয়াটসঅ্যাপ শেয়ার ব্যবহার করে আপনার ডিজিটাল কার্ডও শেয়ার করুন।
                                        """.trimIndent()
                                    } else {
                                        """
                                        💡 **Professional Networking Best Practices:**
                                        1. **24-48 Hour Follow-up**: Send a personalized follow-up message via WhatsApp or email while the context is fresh.
                                        2. **Contextual Memory**: Use CardMate's Notes section to document key talking points and mutual interests.
                                        3. **Reciprocal Sharing**: Use the Share menu to exchange your own interactive digital vCard.
                                        """.trimIndent()
                                    }
                                }
                            }
                            isThinking = false
                        }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = CardMateTealPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Query Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatQuery,
                onValueChange = { chatQuery = it },
                placeholder = {
                    Text(
                        text = if (isBangla) "প্রশ্ন লিখুন (যেমন: অমুকের পদবী কি?)..." else "Ask AI about your contacts...",
                        fontSize = 12.sp
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CardMateTealPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (chatQuery.isBlank()) return@Button
                    isThinking = true
                    val q = chatQuery.trim().lowercase()
                    val matchedCard = allCards.firstOrNull { card ->
                        card.fullName.lowercase().contains(q) ||
                        card.company.lowercase().contains(q) ||
                        card.jobTitle.lowercase().contains(q) ||
                        card.category.lowercase().contains(q)
                    }

                    chatResponse = if (matchedCard != null) {
                        if (isBangla) {
                            """
                            🎯 **পাওয়া গেছে:** ${matchedCard.fullName}
                            • পদবী: ${matchedCard.jobTitle.ifBlank { "উল্লেখ নেই" }}
                            • প্রতিষ্ঠান: ${matchedCard.company.ifBlank { "উল্লেখ নেই" }}
                            • ফোন: ${matchedCard.phone.ifBlank { "নেই" }}
                            • ইমেইল: ${matchedCard.email.ifBlank { "নেই" }}
                            • ক্যাটাগরি: ${matchedCard.category}
                            """.trimIndent()
                        } else {
                            """
                            🎯 **Contact Found:** ${matchedCard.fullName}
                            • Title: ${matchedCard.jobTitle.ifBlank { "N/A" }}
                            • Company: ${matchedCard.company.ifBlank { "N/A" }}
                            • Phone: ${matchedCard.phone.ifBlank { "N/A" }}
                            • Email: ${matchedCard.email.ifBlank { "N/A" }}
                            • Category: ${matchedCard.category}
                            """.trimIndent()
                        }
                    } else {
                        if (isBangla) {
                            "🤖 CardMate AI: '$chatQuery' সম্পর্কিত তথ্য বিশ্লেষণ করা হয়েছে। মোট ${allCards.size} টি সংরক্ষিত কার্ডের মধ্যে সরাসরি কোনো অমিল পাওয়া যায়নি।"
                        } else {
                            "🤖 CardMate AI: Searched your directory of ${allCards.size} cards for '$chatQuery'. You can ask by person name, company, or category."
                        }
                    }
                    isThinking = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF042F2E), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AI Output Response Surface
        if (isThinking) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CardMateTealPrimary, modifier = Modifier.size(32.dp))
            }
        } else if (chatResponse.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(modifier = Modifier.padding(14.dp)) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "CardMate AI বিশ্লেষণ:" else "CardMate AI Insights:",
                                color = CardMateTealPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = chatResponse,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBangla) "উপরের সাজেস্টেড প্রশ্নে ট্যাপ করুন অথবা নতুন প্রশ্ন লিখুন" else "Tap a suggested topic above or ask any question",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
