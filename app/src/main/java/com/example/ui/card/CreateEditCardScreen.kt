package com.example.ui.card

import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.DuplicateDetectionHelper
import com.example.ai.DuplicateMatch
import com.example.data.model.BusinessCard
import com.example.data.model.CardCategory
import com.example.data.model.CardTemplate
import com.example.ui.ai.DuplicateMergeDialog
import com.example.ui.components.DigitalBusinessCardView
import com.example.ui.components.ProfilePhotoPickerBottomSheet
import com.example.ui.navigation.BackNavigationService
import com.example.ui.navigation.UnsavedChangesGuard
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel

data class CustomCardField(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val value: String = ""
)

@Composable
fun CreateEditCardScreen(
    viewModel: CardViewModel,
    cardToEdit: BusinessCard?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val context = LocalContext.current
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()

    val initialPrimaryPhones = com.example.util.PhoneNumberUtils.splitPhoneNumbers(cardToEdit?.phone)
    val initialSecondaryPhones = com.example.util.PhoneNumberUtils.splitPhoneNumbers(cardToEdit?.secondaryPhone)

    var fullName by remember { mutableStateOf(cardToEdit?.fullName ?: "") }
    var jobTitle by remember { mutableStateOf(cardToEdit?.jobTitle ?: "") }
    var company by remember { mutableStateOf(cardToEdit?.company ?: "") }
    var phone by remember { mutableStateOf(initialPrimaryPhones.firstOrNull() ?: cardToEdit?.phone ?: "") }
    var email by remember { mutableStateOf(cardToEdit?.email ?: "") }
    var website by remember { mutableStateOf(cardToEdit?.website ?: "") }
    var address by remember { mutableStateOf(cardToEdit?.address ?: "") }
    var category by remember { mutableStateOf(cardToEdit?.category ?: "Tech & IT") }
    var notes by remember { mutableStateOf(cardToEdit?.notes ?: "") }
    var photoUri by remember { mutableStateOf(cardToEdit?.cardFrontImageUri) }
    var selectedTemplate by remember { mutableStateOf(cardToEdit?.cardLayoutTemplate ?: "modern_slate") }
    var showPhotoPicker by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    var pendingDuplicateMatch by remember { mutableStateOf<DuplicateMatch?>(null) }
    var pendingCardToSave by remember { mutableStateOf<BusinessCard?>(null) }

    // Multi-Phone support - separate fields for multiple alternate numbers
    var additionalPhones by remember {
        mutableStateOf<List<String>>(
            buildList {
                // If primary phone contained multiple numbers, add the rest here
                if (initialPrimaryPhones.size > 1) {
                    addAll(initialPrimaryPhones.drop(1))
                }
                // Add all secondary phone numbers (each as its own separate field!)
                addAll(initialSecondaryPhones)

                if (!cardToEdit?.socialLinks.isNullOrBlank()) {
                    cardToEdit!!.socialLinks.split("\n", "|").forEach { part ->
                        val trimmed = part.trim()
                        val lower = trimmed.lowercase()
                        if (lower.startsWith("ফোন") || lower.startsWith("phone") || lower.startsWith("mobile") || lower.startsWith("মোবাইল") || lower.startsWith("বিকল্প")) {
                            if (trimmed.contains(":")) {
                                val v = trimmed.split(":", limit = 2)[1].trim()
                                val splitList = com.example.util.PhoneNumberUtils.splitPhoneNumbers(v)
                                splitList.forEach { num ->
                                    if (num.isNotBlank() && num != cardToEdit?.phone && !contains(num)) {
                                        add(num)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Multi-Email support
    var additionalEmails by remember {
        mutableStateOf<List<String>>(
            buildList {
                if (!cardToEdit?.socialLinks.isNullOrBlank()) {
                    cardToEdit!!.socialLinks.split("\n", "|").forEach { part ->
                        val trimmed = part.trim()
                        val lower = trimmed.lowercase()
                        if (lower.startsWith("ইমেইল") || lower.startsWith("email") || lower.startsWith("mail")) {
                            if (trimmed.contains(":")) {
                                val v = trimmed.split(":", limit = 2)[1].trim()
                                if (v.isNotBlank() && v != cardToEdit?.email && !contains(v)) {
                                    add(v)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Multi-Company Info support
    var additionalCompanyInfos by remember {
        mutableStateOf<List<CustomCardField>>(
            buildList {
                if (!cardToEdit?.socialLinks.isNullOrBlank()) {
                    cardToEdit!!.socialLinks.split("\n", "|").forEach { part ->
                        val trimmed = part.trim()
                        val lower = trimmed.lowercase()
                        if (lower.startsWith("কোম্পানি") || lower.startsWith("company") ||
                            lower.startsWith("বিভাগ") || lower.startsWith("dept") ||
                            lower.startsWith("branch") || lower.startsWith("শাখা") ||
                            lower.startsWith("tin") || lower.startsWith("tax")
                        ) {
                            if (trimmed.contains(":")) {
                                val split = trimmed.split(":", limit = 2)
                                add(CustomCardField(label = split[0].trim(), value = split[1].trim()))
                            }
                        }
                    }
                }
            }
        )
    }

    // Multi-Address support
    var additionalAddresses by remember {
        mutableStateOf<List<String>>(
            buildList {
                if (!cardToEdit?.socialLinks.isNullOrBlank()) {
                    cardToEdit!!.socialLinks.split("\n", "|").forEach { part ->
                        val trimmed = part.trim()
                        val lower = trimmed.lowercase()
                        if (lower.startsWith("ঠিকানা") || lower.startsWith("address") || lower.startsWith("location")) {
                            if (trimmed.contains(":")) {
                                val v = trimmed.split(":", limit = 2)[1].trim()
                                if (v.isNotBlank() && v != cardToEdit?.address && !contains(v)) {
                                    add(v)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Custom Social & Extra Fields
    var customFields by remember {
        mutableStateOf<List<CustomCardField>>(
            buildList {
                if (!cardToEdit?.socialLinks.isNullOrBlank()) {
                    cardToEdit!!.socialLinks.split("\n", "|").forEach { part ->
                        val trimmed = part.trim()
                        val lower = trimmed.lowercase()
                        val isPhone = lower.startsWith("ফোন") || lower.startsWith("phone") || lower.startsWith("mobile") || lower.startsWith("মোবাইল")
                        val isEmail = lower.startsWith("ইমেইল") || lower.startsWith("email") || lower.startsWith("mail")
                        val isAddr = lower.startsWith("ঠিকানা") || lower.startsWith("address") || lower.startsWith("location")
                        val isComp = lower.startsWith("কোম্পানি") || lower.startsWith("company") ||
                                lower.startsWith("বিভাগ") || lower.startsWith("dept") ||
                                lower.startsWith("branch") || lower.startsWith("শাখা") ||
                                lower.startsWith("tin") || lower.startsWith("tax")

                        if (!isPhone && !isEmail && !isAddr && !isComp) {
                            if (trimmed.contains(":")) {
                                val split = trimmed.split(":", limit = 2)
                                add(CustomCardField(label = split[0].trim(), value = split[1].trim()))
                            } else if (trimmed.isNotBlank()) {
                                add(CustomCardField(label = "Link", value = trimmed))
                            }
                        }
                    }
                }
            }
        )
    }

    val combinedSocialLinks = buildList {
        // Extra phones (3rd+)
        additionalPhones.drop(1).forEachIndexed { idx, p ->
            if (p.isNotBlank()) add("${if (isBangla) "বিকল্প ফোন" else "Phone"} ${idx + 3}: ${p.trim()}")
        }
        // Extra emails
        additionalEmails.forEachIndexed { idx, em ->
            if (em.isNotBlank()) add("${if (isBangla) "বিকল্প ইমেইল" else "Email"} ${idx + 2}: ${em.trim()}")
        }
        // Extra company info
        additionalCompanyInfos.forEach { item ->
            if (item.label.isNotBlank() && item.value.isNotBlank()) add("${item.label.trim()}: ${item.value.trim()}")
        }
        // Extra addresses
        additionalAddresses.forEachIndexed { idx, addr ->
            if (addr.isNotBlank()) add("${if (isBangla) "বিকল্প ঠিকানা" else "Address"} ${idx + 2}: ${addr.trim()}")
        }
        // Custom fields
        customFields.forEach { item ->
            if (item.label.isNotBlank() && item.value.isNotBlank()) add("${item.label.trim()}: ${item.value.trim()}")
        }
    }.joinToString("\n")

    val previewCard = (cardToEdit ?: BusinessCard()).copy(
        id = cardToEdit?.id ?: 0L,
        fullName = fullName.ifBlank { if (isBangla) "আপনার পূর্ণ নাম" else "Full Name" },
        jobTitle = jobTitle.ifBlank { if (isBangla) "পদবী / ডেজিগনেশন" else "Job Title" },
        company = company.ifBlank { if (isBangla) "কোম্পানির নাম" else "Company Name" },
        phone = phone.ifBlank { "+880 1700-000000" },
        secondaryPhone = additionalPhones.firstOrNull()?.trim() ?: "",
        email = email.ifBlank { "contact@example.com" },
        website = website.ifBlank { "https://example.com" },
        address = address.ifBlank { "Dhaka, Bangladesh" },
        category = category,
        notes = notes,
        socialLinks = combinedSocialLinks,
        cardFrontImageUri = photoUri,
        cardLayoutTemplate = selectedTemplate,
        isFavorite = cardToEdit?.isFavorite ?: false,
        isSyncedWithGoogleContacts = cardToEdit?.isSyncedWithGoogleContacts ?: false,
        isBackedUpToCloud = cardToEdit?.isBackedUpToCloud ?: true
    )

    val navService = remember { BackNavigationService.instance }

    // Check for unsaved changes
    val hasUnsavedChanges = remember(
        fullName, jobTitle, company, phone, email, website, address, category, notes, photoUri,
        selectedTemplate, additionalPhones, additionalEmails, additionalCompanyInfos, additionalAddresses,
        customFields
    ) {
        if (cardToEdit == null) {
            fullName.isNotBlank() || jobTitle.isNotBlank() || company.isNotBlank() ||
            phone.isNotBlank() || email.isNotBlank() || website.isNotBlank() ||
            address.isNotBlank() || notes.isNotBlank() || photoUri != null ||
            additionalPhones.isNotEmpty() || additionalEmails.isNotEmpty() ||
            additionalCompanyInfos.isNotEmpty() || additionalAddresses.isNotEmpty() ||
            customFields.isNotEmpty()
        } else {
            fullName != cardToEdit.fullName ||
            jobTitle != cardToEdit.jobTitle ||
            company != cardToEdit.company ||
            phone != cardToEdit.phone ||
            email != cardToEdit.email ||
            website != cardToEdit.website ||
            address != cardToEdit.address ||
            category != cardToEdit.category ||
            notes != cardToEdit.notes ||
            photoUri != cardToEdit.cardFrontImageUri ||
            selectedTemplate != cardToEdit.cardLayoutTemplate
        }
    }

    // Unsaved Changes Guard
    UnsavedChangesGuard(
        hasUnsavedChanges = { hasUnsavedChanges },
        isBangla = isBangla,
        onDiscardConfirmed = onBack
    )

    // Modal back intercepts
    DisposableEffect(showPhotoPicker) {
        if (showPhotoPicker) {
            val unreg = navService.registerModal {
                showPhotoPicker = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(showAddCategoryDialog) {
        if (showAddCategoryDialog) {
            val unreg = navService.registerModal {
                showAddCategoryDialog = false
                newCategoryInput = ""
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    DisposableEffect(pendingDuplicateMatch) {
        if (pendingDuplicateMatch != null) {
            val unreg = navService.registerModal {
                pendingDuplicateMatch = null
                pendingCardToSave = null
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .testTag("create_edit_card_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
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
                text = if (cardToEdit != null) {
                    if (isBangla) "কার্ড সম্পাদনা করুন" else "Edit Business Card"
                } else {
                    if (isBangla) "নতুন কার্ড তৈরি করুন" else "Create Business Card"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Card Preview
            item {
                Text(
                    text = if (isBangla) "লাইভ কার্ড প্রিভিউ" else "Live Digital Card Preview",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                DigitalBusinessCardView(
                    card = previewCard,
                    modifier = Modifier.fillMaxWidth(),
                    onPhotoClick = { showPhotoPicker = true }
                )
            }

            // Card Layout Template Chooser
            item {
                Text(
                    text = if (isBangla) "কার্ডের ডিজাইন ও লেআউট পছন্দ করুন" else "Choose Card Theme & Layout",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(CardTemplate.entries.toList()) { tmpl ->
                        val isSelected = selectedTemplate == tmpl.id
                        val name = if (isBangla) tmpl.displayNameBn else tmpl.displayNameEn

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(tmpl.primaryBgColor),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) CardMateTealPrimary else Color(tmpl.accentColor).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTemplate = tmpl.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.5f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    color = Color(tmpl.textColor),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Category Picker
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "ক্যাটাগরি" else "Category",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddCategoryDialog = true }) {
                        Text(
                            text = if (isBangla) "+ কাস্টম ক্যাটাগরি" else "+ Custom Category",
                            color = CardMateTealPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                val defaultCatNames = CardCategory.entries.map { it.englishName }
                val mergedCatList = (defaultCatNames + customCategories).distinct()

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mergedCatList) { catName ->
                        val isSelected = category.equals(catName, ignoreCase = true)
                        val enumMatch = CardCategory.entries.find { it.englishName.equals(catName, ignoreCase = true) }
                        val displayName = if (isBangla && enumMatch != null) enumMatch.banglaName else catName

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { category = catName }
                        ) {
                            Text(
                                text = if (isSelected) "✓ $displayName" else displayName,
                                color = if (isSelected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 1. Basic Identity
            item {
                CustomFormField(
                    label = if (isBangla) "পূর্ণ নাম *" else "Full Name *",
                    value = fullName,
                    onValueChange = { fullName = it }
                )
            }

            // 2. Company & Job Details (With + Button for extra company info)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeaderWithAdd(
                        title = if (isBangla) "কোম্পানি ও কর্মসংস্থান" else "Company Information",
                        subtitle = if (isBangla) "কোম্পানির নাম, পদবী, ওয়েবসাইট ও শাখা তথ্য" else "Company name, title, website, branch info",
                        onAddClick = {
                            additionalCompanyInfos = additionalCompanyInfos + CustomCardField(
                                label = if (isBangla) "শাখা / বিভাগ" else "Branch / Dept",
                                value = ""
                            )
                        },
                        addTooltip = "Add Company Detail",
                        testTag = "add_company_detail_btn"
                    )

                    CustomFormField(
                        label = if (isBangla) "কোম্পানির নাম" else "Company Name",
                        value = company,
                        onValueChange = { company = it }
                    )

                    CustomFormField(
                        label = if (isBangla) "পদবী / ডেজিগনেশন" else "Job Title",
                        value = jobTitle,
                        onValueChange = { jobTitle = it }
                    )

                    CustomFormField(
                        label = if (isBangla) "ওয়েবসাইট" else "Website URL",
                        value = website,
                        onValueChange = { website = it }
                    )

                    // Render Additional Company Info Fields
                    additionalCompanyInfos.forEachIndexed { index, field ->
                        DeletableCustomFieldRow(
                            label = field.label,
                            value = field.value,
                            labelPlaceholder = if (isBangla) "বিভাগ / ব্রাঞ্চ" else "Label",
                            valuePlaceholder = if (isBangla) "তথ্য লিখুন" else "Value",
                            onLabelChange = { newLbl ->
                                additionalCompanyInfos = additionalCompanyInfos.toMutableList().also {
                                    it[index] = it[index].copy(label = newLbl)
                                }
                            },
                            onValueChange = { newVal ->
                                additionalCompanyInfos = additionalCompanyInfos.toMutableList().also {
                                    it[index] = it[index].copy(value = newVal)
                                }
                            },
                            onDelete = {
                                additionalCompanyInfos = additionalCompanyInfos.toMutableList().also {
                                    it.removeAt(index)
                                }
                            }
                        )
                    }
                }
            }

            // 3. Phone Numbers Section (With + Button)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeaderWithAdd(
                        title = if (isBangla) "ফোন ও মোবাইল নম্বর *" else "Phone Numbers *",
                        subtitle = if (isBangla) "একাধিক মোবাইল নম্বর যোগ করতে + চাপুন" else "Click + on right to add multiple phones",
                        onAddClick = {
                            additionalPhones = additionalPhones + ""
                        },
                        addTooltip = "Add Phone Number",
                        testTag = "add_phone_btn"
                    )

                    CustomFormField(
                        label = if (isBangla) "প্রধান ফোন নম্বর *" else "Primary Phone *",
                        value = phone,
                        onValueChange = { input ->
                            val split = com.example.util.PhoneNumberUtils.splitPhoneNumbers(input)
                            if (split.size > 1) {
                                phone = split[0]
                                additionalPhones = split.drop(1) + additionalPhones
                            } else {
                                phone = input
                            }
                        }
                    )

                    // Render Additional Phones (Each in its own field!)
                    additionalPhones.forEachIndexed { index, ph ->
                        DeletableSingleFieldRow(
                            label = if (isBangla) "বিকল্প ফোন নম্বর ${index + 2}" else "Secondary Phone ${index + 2}",
                            value = ph,
                            onValueChange = { newVal ->
                                additionalPhones = com.example.util.PhoneNumberUtils.handlePhoneListEdit(
                                    currentList = additionalPhones,
                                    index = index,
                                    newVal = newVal
                                )
                            },
                            onDelete = {
                                additionalPhones = additionalPhones.toMutableList().also {
                                    it.removeAt(index)
                                }
                            }
                        )
                    }
                }
            }

            // 4. Email Addresses Section (With + Button)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeaderWithAdd(
                        title = if (isBangla) "ইমেইল এড্রেস" else "Email Addresses",
                        subtitle = if (isBangla) "একাধিক ইমেইল যোগ করতে + চাপুন" else "Click + on right to add multiple emails",
                        onAddClick = {
                            additionalEmails = additionalEmails + ""
                        },
                        addTooltip = "Add Email Address",
                        testTag = "add_email_btn"
                    )

                    CustomFormField(
                        label = if (isBangla) "প্রধান ইমেইল এড্রেস" else "Primary Email Address",
                        value = email,
                        onValueChange = { email = it }
                    )

                    // Render Additional Emails
                    additionalEmails.forEachIndexed { index, em ->
                        DeletableSingleFieldRow(
                            label = if (isBangla) "বিকল্প ইমেইল ${index + 2}" else "Alternative Email ${index + 2}",
                            value = em,
                            onValueChange = { newVal ->
                                additionalEmails = additionalEmails.toMutableList().also {
                                    it[index] = newVal
                                }
                            },
                            onDelete = {
                                additionalEmails = additionalEmails.toMutableList().also {
                                    it.removeAt(index)
                                }
                            }
                        )
                    }
                }
            }

            // 5. Addresses Section (With + Button)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeaderWithAdd(
                        title = if (isBangla) "ঠিকানা ও লোকেশন" else "Addresses & Locations",
                        subtitle = if (isBangla) "একাধিক ঠিকানা বা ব্রাঞ্চ এড্রেস যোগ করতে + চাপুন" else "Click + to add branch or alternate addresses",
                        onAddClick = {
                            additionalAddresses = additionalAddresses + ""
                        },
                        addTooltip = "Add Address",
                        testTag = "add_address_btn"
                    )

                    CustomFormField(
                        label = if (isBangla) "প্রধান অফিস / বাড়ির ঠিকানা" else "Primary Address",
                        value = address,
                        onValueChange = { address = it }
                    )

                    // Render Additional Addresses
                    additionalAddresses.forEachIndexed { index, addr ->
                        DeletableSingleFieldRow(
                            label = if (isBangla) "বিকল্প / শাখা ঠিকানা ${index + 2}" else "Branch / Alternate Address ${index + 2}",
                            value = addr,
                            onValueChange = { newVal ->
                                additionalAddresses = additionalAddresses.toMutableList().also {
                                    it[index] = newVal
                                }
                            },
                            onDelete = {
                                additionalAddresses = additionalAddresses.toMutableList().also {
                                    it.removeAt(index)
                                }
                            }
                        )
                    }
                }
            }

            // 6. Profile Photo / Logo Card (Clickable to change)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Card Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, CardMateTealPrimary, CircleShape)
                                    .clickable { showPhotoPicker = true }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(CardMateTealPrimary.copy(alpha = 0.15f))
                                    .border(2.dp, CardMateTealPrimary.copy(alpha = 0.4f), CircleShape)
                                    .clickable { showPhotoPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "প্রোফাইল ছবি / লোগো" else "Profile Photo / Logo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (!photoUri.isNullOrBlank()) {
                                    if (isBangla) "ছবি যুক্ত আছে (ক্লিক করে পরিবর্তন করুন)" else "Photo attached (Click to change)"
                                } else {
                                    if (isBangla) "ছবি বা লোগো যুক্ত করুন" else "Add photo or logo from gallery/camera"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showPhotoPicker = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (photoUri.isNullOrBlank()) {
                                    if (isBangla) "আপলোড" else "Upload"
                                } else {
                                    if (isBangla) "পরিবর্তন" else "Change"
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 7. Dynamic Custom Fields ("Add Field") Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "অতিরিক্ত ফিল্ড ও সোশ্যাল লিংক" else "Custom Fields & Social Links",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBangla) "WhatsApp, LinkedIn, Telegram ইত্যাদি যুক্ত করুন" else "Add WhatsApp, LinkedIn, Telegram, etc.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                customFields = customFields + CustomCardField(
                                    label = if (isBangla) "কাস্টম ফিল্ড" else "Custom Field",
                                    value = ""
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardMateTealPrimary),
                            modifier = Modifier.testTag("add_custom_field_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "+ ফিল্ড" else "+ Field",
                                fontSize = 12.sp,
                                color = CardMateTealPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Quick Suggested Field Presets
                    Text(
                        text = if (isBangla) "দ্রুত ফিল্ড নির্বাচন:" else "Quick Suggestions:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val suggestions = if (isBangla) {
                            listOf("WhatsApp", "LinkedIn", "Telegram", "Twitter / X", "GitHub", "Facebook", "Instagram", "ট্যাক্স / TIN", "জরুরি যোগাযোগ", "ফ্যাক্স")
                        } else {
                            listOf("WhatsApp", "LinkedIn", "Telegram", "Twitter / X", "GitHub", "Facebook", "Instagram", "Tax / TIN", "Emergency", "Fax")
                        }
                        items(suggestions) { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        customFields = customFields + CustomCardField(label = suggestion, value = "")
                                    }
                            ) {
                                Text(
                                    text = "+ $suggestion",
                                    fontSize = 11.sp,
                                    color = CardMateTealPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Render Dynamic Custom Fields
                    customFields.forEachIndexed { index, field ->
                        DeletableCustomFieldRow(
                            label = field.label,
                            value = field.value,
                            labelPlaceholder = if (isBangla) "ফিল্ডের নাম" else "Label",
                            valuePlaceholder = if (isBangla) "তথ্য / মান" else "Value",
                            onLabelChange = { newLbl ->
                                customFields = customFields.toMutableList().also {
                                    it[index] = it[index].copy(label = newLbl)
                                }
                            },
                            onValueChange = { newVal ->
                                customFields = customFields.toMutableList().also {
                                    it[index] = it[index].copy(value = newVal)
                                }
                            },
                            onDelete = {
                                customFields = customFields.toMutableList().also {
                                    it.removeAt(index)
                                }
                            }
                        )
                    }
                }
            }

            // 8. Notes
            item {
                CustomFormField(
                    label = if (isBangla) "নোট / মন্তব্য" else "Notes / Description",
                    value = notes,
                    onValueChange = { notes = it },
                    singleLine = false
                )
            }

            // Save Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (fullName.isBlank()) {
                            Toast.makeText(context, if (isBangla) "অনুগ্রহ করে নামটি লিখুন" else "Please enter a name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val cardToSave = (cardToEdit ?: BusinessCard()).copy(
                            fullName = fullName.trim(),
                            jobTitle = jobTitle.trim(),
                            company = company.trim(),
                            phone = phone.trim(),
                            secondaryPhone = additionalPhones.firstOrNull()?.trim() ?: "",
                            email = email.trim(),
                            website = website.trim(),
                            address = address.trim(),
                            category = category,
                            notes = notes.trim(),
                            socialLinks = combinedSocialLinks,
                            cardFrontImageUri = photoUri,
                            cardLayoutTemplate = selectedTemplate,
                            updatedAt = System.currentTimeMillis()
                        )

                        // Check for duplicate contacts if creating new card
                        if (cardToEdit == null) {
                            val potentialDuplicate = DuplicateDetectionHelper.findBestMatch(cardToSave, allCards, minScore = 70)
                            if (potentialDuplicate != null) {
                                pendingDuplicateMatch = potentialDuplicate
                                pendingCardToSave = cardToSave
                                return@Button
                            }
                        }

                        viewModel.saveCard(cardToSave) { newId ->
                            Toast.makeText(context, if (isBangla) "কার্ড সফলভাবে সেভ হয়েছে!" else "Business Card Saved!", Toast.LENGTH_SHORT).show()
                            onSaved(newId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardMateTealPrimary,
                        contentColor = Color(0xFF042F2E)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_card_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "কার্ড সংরক্ষণ করুন" else "Save Business Card",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showPhotoPicker) {
        ProfilePhotoPickerBottomSheet(
            currentPhotoUri = photoUri,
            isBangla = isBangla,
            onPhotoSelected = { newUri ->
                photoUri = newUri
            },
            onDismiss = { showPhotoPicker = false }
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
                            "যেমন: Government, Engineering, Consultancy, Legal ইত্যাদি।"
                        else
                            "e.g. Government, Engineering, Consultancy, Legal, etc.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        placeholder = { Text(if (isBangla) "ক্যাটাগরির নাম..." else "Category Name...") },
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
                            category = trimmed
                            showAddCategoryDialog = false
                            newCategoryInput = ""
                            Toast.makeText(context, if (isBangla) "ক্যাটাগরি '$trimmed' নির্বাচিত হয়েছে" else "Category '$trimmed' added & selected", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary)
                ) {
                    Text(if (isBangla) "যুক্ত করুন" else "Add & Select", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
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

    // Duplicate Merge Prompt Dialog
    pendingDuplicateMatch?.let { match ->
        DuplicateMergeDialog(
            match = match,
            isBangla = isBangla,
            onMerge = { mergedCard ->
                viewModel.mergeDuplicateCards(match.cardA, pendingCardToSave ?: match.cardB, mergedCard)
                pendingDuplicateMatch = null
                pendingCardToSave = null
                Toast.makeText(
                    context,
                    if (isBangla) "বিদ্যমান কন্টাক্টের সাথে সফলভাবে মার্জ হয়েছে!" else "Merged with existing contact successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                onSaved(mergedCard.id)
            },
            onKeepSeparate = {
                val cardToSave = pendingCardToSave ?: return@DuplicateMergeDialog
                pendingDuplicateMatch = null
                pendingCardToSave = null
                viewModel.saveCard(cardToSave) { newId ->
                    Toast.makeText(context, if (isBangla) "নতুন কার্ড হিসেবে সেভ করা হয়েছে" else "Saved as a new card", Toast.LENGTH_SHORT).show()
                    onSaved(newId)
                }
            },
            onDismiss = {
                pendingDuplicateMatch = null
                pendingCardToSave = null
            }
        )
    }
}

@Composable
private fun SectionHeaderWithAdd(
    title: String,
    subtitle: String? = null,
    onAddClick: () -> Unit,
    addTooltip: String,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = CardMateTealPrimary.copy(alpha = 0.15f),
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onAddClick() }
                .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = addTooltip,
                    tint = CardMateTealPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DeletableSingleFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CardMateTealPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete field",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DeletableCustomFieldRow(
    label: String,
    value: String,
    labelPlaceholder: String,
    valuePlaceholder: String,
    onLabelChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                label = { Text(labelPlaceholder, fontSize = 10.sp) },
                modifier = Modifier.weight(0.42f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CardMateTealPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(valuePlaceholder, fontSize = 10.sp) },
                modifier = Modifier.weight(0.58f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CardMateTealPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Field",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 3,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CardMateTealPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
