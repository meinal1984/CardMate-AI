package com.example.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ChatMessage
import com.example.ai.ChatPersona
import com.example.ai.ChatPersonaType
import com.example.ai.ChatRole
import com.example.ai.GeminiChatService
import com.example.ai.GeminiModels
import com.example.ai.GroundingPlaceSource
import com.example.data.model.BusinessCard
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeminiMultiTurnChatView(
    viewModel: CardViewModel,
    isBangla: Boolean,
    allCards: List<BusinessCard>,
    onOpenCardDetail: ((BusinessCard) -> Unit)? = null
) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isChatThinking.collectAsState()
    val activePersona by viewModel.activeChatPersona.collectAsState()
    val activeModel by viewModel.activeChatModel.collectAsState()
    val isMapsGroundingEnabled by viewModel.isMapsGroundingEnabled.collectAsState()
    val activeChatCard by viewModel.activeChatCard.collectAsState()
    val chatInputDraft by viewModel.chatInputDraft.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var showPersonaMenu by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Sync input field when a draft prompt is prepared (e.g. from card detail or follow-up)
    LaunchedEffect(chatInputDraft) {
        if (chatInputDraft.isNotBlank()) {
            inputPrompt = chatInputDraft
            viewModel.setChatInputDraft("")
        }
    }

    // Auto-scroll to bottom when a new message arrives or state changes
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestedPrompts = remember(activePersona.type, isBangla, allCards.size) {
        when (activePersona.type) {
            ChatPersonaType.MAPS_SCOUT -> {
                if (isBangla) listOf(
                    "☕ গুলশান ও বনানীতে শান্ত মিটিং ক্যাফে খুঁজুন",
                    "🏢 সংরক্ষিত কার্ডের অফিস লোকেশন ও রুট দেখুন",
                    "📍 ধানমন্ডিতে বিজনেস ডিনারের ভালো রেস্তোরাঁ",
                    "🚗 মতিঝিল ও কাওরানবাজার কর্পোরেট হাব তথ্য"
                ) else listOf(
                    "☕ Find quiet meeting cafes in Gulshan / Banani",
                    "🏢 Look up office locations for saved contacts",
                    "📍 Best corporate dinner venues in Dhanmondi",
                    "🚗 Meeting venues & transit directions in Motijheel"
                )
            }
            ChatPersonaType.EXECUTIVE_ADVISOR -> {
                if (isBangla) listOf(
                    "🧠 বি২বি ক্লায়েন্ট একুইজিশন স্ট্র্যাটেজি দিন",
                    "📊 সেভ করা কোম্পানিগুলোর মধ্যে পার্টনারশিপ সম্ভাবনা",
                    "✍️ উচ্চপদস্থ কর্মকর্তার সাথে মিটিংয়ের পিচ ডেক",
                    "💡 দীর্ঘমেয়াদী ব্যবসায়িক সম্পর্ক তৈরির গাইড"
                ) else listOf(
                    "🧠 Formulate a B2B client acquisition roadmap",
                    "📊 Evaluate partnership synergy across my cards",
                    "✍️ Pitch deck talking points for enterprise clients",
                    "💡 High-value networking & deal closing tactics"
                )
            }
            ChatPersonaType.FAST_LOOKUP -> {
                if (isBangla) listOf(
                    "📞 মিসিং ফোন নম্বর ও ইমেইল তালিকা",
                    "👥 টেক্সটাইল ও আইটি সেক্টরের কন্টাক্টসমূহ",
                    "⭐ ফেভারিট কন্টাক্টগুলোর সংক্ষিপ্ত তালিকা"
                ) else listOf(
                    "📞 Contacts with missing phone numbers",
                    "👥 List IT & Software company contacts",
                    "⭐ Quick summary of Starred VIP contacts"
                )
            }
            else -> {
                if (isBangla) listOf(
                    "📊 কন্টাক্ট ডিরেক্টরি সামারি রিপোর্ট",
                    "✍️ মিটিংয়ের পর ফলো-আপ হোয়াটসঅ্যাপ ড্রাফট",
                    "☕ গুগল ম্যাপসে কাছাকাছি ক্যাফে খুঁজুন",
                    "💡 কার্ডমেট ব্যবহার করে নেটওয়ার্কিং বাড়ানোর টিপস"
                ) else listOf(
                    "📊 Contact Directory Summary Report",
                    "✍️ Draft post-meeting WhatsApp follow-up",
                    "☕ Find nearby business cafes on Google Maps",
                    "💡 Best practices for networking follow-ups"
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- Unified Elegant Control Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Persona Selector (Pill with Dropdown Menu)
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardMateTealPrimary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showPersonaMenu = true }
                        .testTag("persona_selector_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = activePersona.iconEmoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) activePersona.titleBn else activePersona.titleEn,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CardMateTealPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Persona",
                            tint = CardMateTealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showPersonaMenu,
                    onDismissRequest = { showPersonaMenu = false }
                ) {
                    GeminiChatService.ALL_PERSONAS.forEach { persona ->
                        val isSelected = persona.type == activePersona.type
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = persona.iconEmoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isBangla) persona.titleBn else persona.titleEn,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if (isBangla) persona.subtitleBn else persona.subtitleEn,
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.setChatPersona(persona)
                                showPersonaMenu = false
                            }
                        )
                    }
                }
            }

            // 2. Model & Maps Grounding Quick Toggle Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (showSettingsPanel) CardMateTealPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (showSettingsPanel) CardMateTealPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showSettingsPanel = !showSettingsPanel }
                    .testTag("ai_settings_toggle_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (activeModel) {
                            GeminiModels.GEMINI_3_1_PRO -> "3.1 Pro"
                            GeminiModels.GEMINI_3_1_FLASH_LITE -> "3.1 Lite"
                            else -> "3.5 Flash"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isMapsGroundingEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Maps Grounding",
                            tint = CardMateGoldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tune",
                        tint = if (showSettingsPanel) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Expandable Settings Panel for Model Selection & Google Maps Grounding
        AnimatedVisibility(
            visible = showSettingsPanel,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Models
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val models = listOf(
                            GeminiModels.GEMINI_3_5_FLASH to "3.5 Flash",
                            GeminiModels.GEMINI_3_1_PRO to "3.1 Pro",
                            GeminiModels.GEMINI_3_1_FLASH_LITE to "3.1 Lite"
                        )
                        models.forEach { (modelId, label) ->
                            val isCurrent = activeModel == modelId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) CardMateTealPrimary.copy(alpha = 0.25f) else Color.Transparent,
                                border = if (isCurrent) BorderStroke(1.dp, CardMateTealPrimary) else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setChatModel(modelId) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Maps Grounding Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.toggleMapsGrounding(!isMapsGroundingEnabled) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Maps Grounding",
                            tint = if (isMapsGroundingEnabled) CardMateGoldAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isBangla) "ম্যাপস" else "Maps",
                            fontSize = 11.sp,
                            fontWeight = if (isMapsGroundingEnabled) FontWeight.Bold else FontWeight.Normal,
                            color = if (isMapsGroundingEnabled) CardMateGoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isMapsGroundingEnabled,
                            onCheckedChange = { viewModel.toggleMapsGrounding(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CardMateGoldAccent,
                                checkedTrackColor = CardMateGoldAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .height(20.dp)
                                .testTag("maps_grounding_switch")
                        )
                    }
                }
            }
        }

        // Active Card Context Banner (If chatting specifically for a contact card)
        if (activeChatCard != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CardMateTealPrimary.copy(alpha = 0.1f),
                border = BorderStroke(0.8.dp, CardMateTealPrimary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CardMateTealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${if (isBangla) "টার্গেট কন্টাক্ট:" else "Target:"} ${activeChatCard!!.fullName}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeChatCard!!.jobTitle.ifBlank { "Professional" }} • ${activeChatCard!!.company.ifBlank { "Contact" }} • ${if (isBangla) "💾 হিস্ট্রি সংরক্ষিত" else "💾 History Saved"}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.prepareGeneralChat() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Switch to general assistant",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Message Thread or Welcome Screen
        if (messages.isEmpty() && !isThinking) {
            ChatWelcomeScreen(
                persona = activePersona,
                isBangla = isBangla,
                cardsCount = allCards.size,
                suggestedPrompts = suggestedPrompts,
                onSelectPrompt = { prompt ->
                    // Draft prompt into the text field so user can edit and tap Send manually!
                    inputPrompt = prompt
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("chat_messages_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        isBangla = isBangla,
                        context = context,
                        onOpenCardDetail = onOpenCardDetail,
                        onDraftFollowUp = { card ->
                            val prompt = if (isBangla) {
                                "${card.fullName} (${card.company})-এর সাথে মিটিং পরবর্তী একটি চমৎকার প্রফেশনাল ফলো-আপ মেসেজ লিখে দাও।"
                            } else {
                                "Draft a high-impact professional follow-up message for ${card.fullName} at ${card.company} after our recent meeting."
                            }
                            inputPrompt = prompt
                        }
                    )
                }

                if (isThinking) {
                    item {
                        ThinkingBubbleItem(
                            persona = activePersona,
                            model = activeModel,
                            isBangla = isBangla
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Prompt Suggestions Ribbon (When conversation is active)
        if (messages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestedPrompts.forEach { prompt ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // Populate prompt into draft input box without sending automatically!
                                inputPrompt = prompt
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
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Input Field & Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear History Button
            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearChatHistory() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = {
                    Text(
                        text = if (isBangla) "যেকোনো প্রশ্ন লিখুন বা স্থান খুঁজুন..." else "Ask Gemini or search places on Google Maps...",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CardMateTealPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("gemini_chat_input")
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            Surface(
                shape = CircleShape,
                color = if (inputPrompt.isNotBlank() && !isThinking) CardMateTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = inputPrompt.isNotBlank() && !isThinking) {
                        val textToSend = inputPrompt
                        inputPrompt = ""
                        viewModel.sendChatMessage(textToSend)
                    }
                    .testTag("gemini_chat_send_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isThinking) {
                        CircularProgressIndicator(
                            color = CardMateTealPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputPrompt.isNotBlank()) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatWelcomeScreen(
    persona: ChatPersona,
    isBangla: Boolean,
    cardsCount: Int,
    suggestedPrompts: List<String>,
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = CardMateTealPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = persona.iconEmoji, fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isBangla) persona.titleBn else persona.titleEn,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isBangla) persona.subtitleBn else persona.subtitleEn,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CardMateGoldAccent.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = CardMateGoldAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBangla) "$cardsCount টি কার্ড সংরক্ষিত • গুগল ম্যাপস সক্রিয়" else "$cardsCount cards indexed • Google Maps Grounding Active",
                        fontSize = 10.5.sp,
                        color = CardMateGoldAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isBangla) "নিচের যেকোনো প্রশ্নে ট্যাপ করে শুরু করুন:" else "Tap a suggested topic to get started:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                suggestedPrompts.forEach { prompt ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectPrompt(prompt) }
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    isBangla: Boolean,
    context: Context,
    onOpenCardDetail: ((BusinessCard) -> Unit)?,
    onDraftFollowUp: (BusinessCard) -> Unit
) {
    val isUser = message.role == ChatRole.USER
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = CardMateTealPrimary.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(30.dp)
                    .padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.90f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 14.dp
                ),
                color = if (isUser) {
                    CardMateTealPrimary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                border = if (!isUser && message.isGroundedWithMaps) {
                    BorderStroke(1.dp, CardMateGoldAccent.copy(alpha = 0.5f))
                } else null,
                modifier = Modifier.testTag(if (isUser) "user_chat_bubble" else "model_chat_bubble")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Model Tag & Grounding Indicator (for AI bubble)
                    if (!isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = message.modelUsed.replace("-preview", ""),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CardMateTealPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                if (message.isGroundedWithMaps) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CardMateGoldAccent.copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = CardMateGoldAccent,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Google Maps",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CardMateGoldAccent
                                            )
                                        }
                                    }
                                }
                            }

                            // Copy & Share buttons
                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("AI Response", message.text)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (isBangla) "কপি হয়েছে!" else "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, message.text)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Gemini Insights"))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Main Text Content
                    Text(
                        text = message.text,
                        color = if (isUser) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                    )

                    // Grounding Sources & Google Maps Places Cards
                    if (message.groundingSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBangla) "📍 গুগল ম্যাপস ভেন্যু ও লোকেশন লিংক:" else "📍 Google Maps Grounded Places & Locations:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CardMateGoldAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            message.groundingSources.forEach { source ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, CardMateGoldAccent.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.uri))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not open map link", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = CardMateGoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = source.title,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (source.address.isNotBlank()) {
                                                    Text(
                                                        text = source.address,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CardMateGoldAccent.copy(alpha = 0.2f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Navigation,
                                                    contentDescription = "Open Maps",
                                                    tint = CardMateGoldAccent,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = if (isBangla) "ম্যাপ খুলুন" else "Maps",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CardMateGoldAccent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Matched Business Cards (CardMate Core Actionable Contacts)
                    if (message.matchedCards.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBangla) "📇 সংশ্লিষ্ট বিজনেস কার্ড (${message.matchedCards.size} টি):" else "📇 Relevant Contacts (${message.matchedCards.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CardMateTealPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            message.matchedCards.forEach { card ->
                                CardMateContactActionCard(
                                    card = card,
                                    isBangla = isBangla,
                                    context = context,
                                    onOpenCardDetail = onOpenCardDetail,
                                    onDraftFollowUp = onDraftFollowUp
                                )
                            }
                        }
                    }

                    // Suggested Follow-up Draft Action Bar
                    val followUpDraft = message.suggestedFollowUpDraft
                    if (followUpDraft != null) {
                        FollowUpDraftActionBar(
                            draftText = followUpDraft,
                            recipientCard = message.draftRecipientCard,
                            isBangla = isBangla,
                            context = context
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formattedTime,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun CardMateContactActionCard(
    card: BusinessCard,
    isBangla: Boolean,
    context: Context,
    onOpenCardDetail: ((BusinessCard) -> Unit)?,
    onDraftFollowUp: (BusinessCard) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Initial circle
                Surface(
                    shape = CircleShape,
                    color = CardMateTealPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = card.fullName.take(1).uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CardMateTealPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = card.fullName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (card.category.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CardMateTealPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = card.category,
                                    fontSize = 8.5.sp,
                                    color = CardMateTealPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (card.jobTitle.isNotBlank() || card.company.isNotBlank()) {
                        Text(
                            text = listOf(card.jobTitle, card.company).filter { it.isNotBlank() }.joinToString(" • "),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(5.dp))

            // Action buttons strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call
                if (card.phone.isNotBlank()) {
                    ContactActionButton(
                        label = if (isBangla) "কল" else "Call",
                        icon = Icons.Default.Call,
                        color = CardMateTealPrimary
                    ) {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${card.phone}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not dial", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // WhatsApp
                    ContactActionButton(
                        label = "WhatsApp",
                        icon = Icons.Default.Message,
                        color = Color(0xFF25D366)
                    ) {
                        try {
                            val cleanPhone = card.phone.replace("+", "").replace("-", "").replace(" ", "").trim()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not available", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Email
                if (card.email.isNotBlank()) {
                    ContactActionButton(
                        label = if (isBangla) "ইমেইল" else "Email",
                        icon = Icons.Default.Email,
                        color = CardMateCyanAccent
                    ) {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${card.email}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open email", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Maps
                if (card.address.isNotBlank()) {
                    ContactActionButton(
                        label = if (isBangla) "ম্যাপ" else "Maps",
                        icon = Icons.Default.LocationOn,
                        color = CardMateGoldAccent
                    ) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(card.address)}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open map", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // View Details
                ContactActionButton(
                    label = if (isBangla) "কার্ড দেখুন" else "View",
                    icon = Icons.Default.Visibility,
                    color = MaterialTheme.colorScheme.onSurface
                ) {
                    onOpenCardDetail?.invoke(card)
                }

                // Follow-up
                ContactActionButton(
                    label = if (isBangla) "✍️ ফলো-আপ" else "✍️ Follow-up",
                    icon = Icons.Default.Edit,
                    color = CardMateTealPrimary
                ) {
                    onDraftFollowUp(card)
                }
            }
        }
    }
}

@Composable
private fun FollowUpDraftActionBar(
    draftText: String,
    recipientCard: BusinessCard?,
    isBangla: Boolean,
    context: Context
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CardMateTealPrimary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = if (isBangla) "⚡ ড্রাফট দিয়ে সরাসরি যোগাযোগ করুন:" else "⚡ Direct Actions for this Draft:",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = CardMateTealPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp
                ContactActionButton(
                    label = if (isBangla) "WhatsApp-এ পাঠান" else "Send WhatsApp",
                    icon = Icons.Default.Message,
                    color = Color(0xFF25D366)
                ) {
                    try {
                        val cleanPhone = recipientCard?.phone?.replace("+", "")?.replace("-", "")?.replace(" ", "")?.trim() ?: ""
                        val uri = if (cleanPhone.isNotBlank()) {
                            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(draftText)}")
                        } else {
                            Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(draftText)}")
                        }
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "WhatsApp not available", Toast.LENGTH_SHORT).show()
                    }
                }

                // Email
                ContactActionButton(
                    label = if (isBangla) "ইমেইলে পাঠান" else "Send Email",
                    icon = Icons.Default.Email,
                    color = CardMateCyanAccent
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${recipientCard?.email ?: ""}")
                            putExtra(Intent.EXTRA_SUBJECT, "Follow-up: Networking & Collaboration")
                            putExtra(Intent.EXTRA_TEXT, draftText)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open email app", Toast.LENGTH_SHORT).show()
                    }
                }

                // SMS
                ContactActionButton(
                    label = if (isBangla) "SMS ড্রাফট" else "SMS Draft",
                    icon = Icons.Default.Call,
                    color = CardMateGoldAccent
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("sms:${recipientCard?.phone ?: ""}")
                            putExtra("sms_body", draftText)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open SMS app", Toast.LENGTH_SHORT).show()
                    }
                }

                // Copy
                ContactActionButton(
                    label = if (isBangla) "টেক্সট কপি" else "Copy Text",
                    icon = Icons.Default.ContentCopy,
                    color = MaterialTheme.colorScheme.onSurface
                ) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("CardMate AI Draft", draftText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, if (isBangla) "মেসেজ কপি হয়েছে!" else "Message copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun ContactActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.6.dp, color.copy(alpha = 0.35f)),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun ThinkingBubbleItem(
    persona: ChatPersona,
    model: String,
    isBangla: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = CircleShape,
            color = CardMateTealPrimary.copy(alpha = 0.2f),
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = CardMateTealPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = CardMateTealPrimary,
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBangla) "Gemini বিশ্লেষণ ও ম্যাপস ডেটা যাচাই করছে..." else "Gemini is analyzing & grounding with Google Maps...",
                    fontSize = 11.sp,
                    color = CardMateTealPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
