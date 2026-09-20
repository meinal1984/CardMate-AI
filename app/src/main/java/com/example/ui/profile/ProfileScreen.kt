package com.example.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CardCategory
import com.example.data.model.CardTemplate
import com.example.data.model.ProfileFieldItem
import com.example.data.model.UserProfile
import com.example.sync.AuthManager
import com.example.sync.VCardExporter
import com.example.ui.components.CardShareBottomSheet
import com.example.ui.components.DigitalBusinessCardView
import com.example.ui.components.ProfilePhotoPickerBottomSheet
import com.example.ui.components.QrCodeHelper
import com.example.ui.qr.DynamicQrGeneratorDialog
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel
import java.util.UUID

@Composable
fun ProfileScreen(
    viewModel: CardViewModel,
    onNavigateToNfc: () -> Unit = {},
    onNavigateToSignIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser by AuthManager.currentUserFlow.collectAsState()

    var isFlipped by remember { mutableStateOf(false) }
    // Lock/Unlock State: Saved by default, only editable when user explicitly clicks Edit button
    var isEditing by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showDynamicQrStudio by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showShareBottomSheet by remember { mutableStateOf(false) }

    // Edit form states initialized from current userProfile
    var fullName by remember(userProfile, isEditing) { mutableStateOf(userProfile.fullName) }
    var jobTitle by remember(userProfile, isEditing) { mutableStateOf(userProfile.jobTitle) }
    var company by remember(userProfile, isEditing) { mutableStateOf(userProfile.company) }
    var department by remember(userProfile, isEditing) { mutableStateOf(userProfile.department) }
    var address by remember(userProfile, isEditing) { mutableStateOf(userProfile.address) }
    var category by remember(userProfile, isEditing) { mutableStateOf(userProfile.category) }
    var bio by remember(userProfile, isEditing) { mutableStateOf(userProfile.bio) }
    var selectedTemplate by remember(userProfile, isEditing) { mutableStateOf(userProfile.cardLayoutTemplate) }
    var photoUri by remember(userProfile, isEditing) { mutableStateOf(userProfile.photoUri) }
    var companyLogoUri by remember(userProfile, isEditing) { mutableStateOf(userProfile.companyLogoUri) }

    // Multi-entry lists for Phone numbers, Emails, Websites, and Social Links
    val editPhones = remember(userProfile, isEditing) {
        mutableStateListOf<ProfileFieldItem>().apply {
            addAll(userProfile.getResolvedPhones())
            if (isEmpty()) {
                add(ProfileFieldItem(label = "Mobile", value = ""))
            }
        }
    }

    val editEmails = remember(userProfile, isEditing) {
        mutableStateListOf<ProfileFieldItem>().apply {
            addAll(userProfile.getResolvedEmails())
            if (isEmpty()) {
                add(ProfileFieldItem(label = "Work", value = ""))
            }
        }
    }

    val editWebsites = remember(userProfile, isEditing) {
        mutableStateListOf<ProfileFieldItem>().apply {
            addAll(userProfile.getResolvedWebsites())
            if (isEmpty()) {
                add(ProfileFieldItem(label = "Company", value = ""))
            }
        }
    }

    val editSocials = remember(userProfile, isEditing) {
        mutableStateListOf<ProfileFieldItem>().apply {
            addAll(userProfile.getResolvedSocials())
            if (isEmpty()) {
                add(ProfileFieldItem(label = "WhatsApp", value = ""))
            }
        }
    }

    // Active Profile for live card preview
    val activeProfile = if (isEditing) {
        UserProfile(
            fullName = fullName,
            jobTitle = jobTitle,
            company = company,
            department = department,
            phone = editPhones.firstOrNull()?.value ?: userProfile.phone,
            secondaryPhone = editPhones.getOrNull(1)?.value ?: userProfile.secondaryPhone,
            email = editEmails.firstOrNull()?.value ?: userProfile.email,
            website = editWebsites.firstOrNull()?.value ?: userProfile.website,
            address = address,
            category = category,
            bio = bio,
            socialLinks = editSocials.filter { it.value.isNotBlank() }.joinToString(" | ") { "${it.label}: ${it.value}" },
            photoUri = photoUri,
            companyLogoUri = companyLogoUri,
            cardLayoutTemplate = selectedTemplate,
            phoneList = editPhones.toList(),
            emailList = editEmails.toList(),
            websiteList = editWebsites.toList(),
            socialList = editSocials.toList()
        )
    } else {
        userProfile
    }

    val previewCard = activeProfile.toBusinessCard()
    val vCardString = remember(previewCard) { VCardExporter.toVCard3String(previewCard) }
    val fullQrBitmap = remember(vCardString) {
        QrCodeHelper.generateQrBitmap(vCardString, sizePx = 600)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("profile_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CamCard Premium Header & Mode Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isBangla) "আমার প্রোফাইল ও স্মার্ট কার্ড" else "My Smart ID & Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isEditing) {
                            if (isBangla) "তথ্য পরিবর্তন করুন এবং সংরক্ষণ বাটনে ক্লিক করুন" else "Update fields and tap Save to lock changes"
                        } else {
                            if (isBangla) "CamCard ডিজিটাল ভিজিটিং কার্ড ও কন্টাক্ট হাব" else "CamCard Digital Business Card & Contact Hub"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Header Action Button (Edit / Save Toggle - Icon Only)
                if (isEditing) {
                    IconButton(
                        onClick = {
                            val updated = UserProfile(
                                fullName = fullName.trim().ifBlank { userProfile.fullName },
                                jobTitle = jobTitle.trim(),
                                company = company.trim(),
                                department = department.trim(),
                                phone = editPhones.firstOrNull { it.value.isNotBlank() }?.value ?: "",
                                secondaryPhone = editPhones.getOrNull(1)?.value ?: "",
                                email = editEmails.firstOrNull { it.value.isNotBlank() }?.value ?: "",
                                website = editWebsites.firstOrNull { it.value.isNotBlank() }?.value ?: "",
                                address = address.trim(),
                                category = category,
                                bio = bio.trim(),
                                socialLinks = editSocials.filter { it.value.isNotBlank() }.joinToString(" | ") { "${it.label}: ${it.value}" },
                                photoUri = photoUri,
                                companyLogoUri = companyLogoUri,
                                cardLayoutTemplate = selectedTemplate,
                                phoneList = editPhones.filter { it.value.isNotBlank() },
                                emailList = editEmails.filter { it.value.isNotBlank() },
                                websiteList = editWebsites.filter { it.value.isNotBlank() },
                                socialList = editSocials.filter { it.value.isNotBlank() }
                            )
                            viewModel.saveUserProfile(updated)
                            isEditing = false
                            Toast.makeText(
                                context,
                                if (isBangla) "প্রোফাইল সংরক্ষিত ও লক করা হয়েছে" else "Profile saved & locked",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(CardMateTealPrimary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Profile",
                            tint = Color(0xFF042F2E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { isEditing = true },
                        modifier = Modifier
                            .size(42.dp)
                            .border(1.5.dp, CardMateTealPrimary, CircleShape)
                            .background(CardMateTealPrimary.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = CardMateTealPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // Account & Cloud Status Banner
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToSignIn() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (currentUser != null) CardMateTealPrimary.copy(alpha = 0.2f)
                                    else CardMateGoldAccent.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.Cloud,
                                contentDescription = null,
                                tint = if (currentUser != null) CardMateTealPrimary else CardMateGoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (currentUser != null)
                                    (currentUser?.displayName ?: currentUser?.email ?: "CardMate Account")
                                else
                                    (if (isBangla) "ক্লাউড সিঙ্ক অ্যাকাউন্ট যুক্ত করুন" else "Connect Cloud & Google Sync"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentUser != null)
                                    (if (isBangla) "ক্লাউড ব্যাকআপ সক্রিয় রয়েছে" else "Cloud backup & sync active")
                                else
                                    (if (isBangla) "গুগল বা ইমেইল দিয়ে সাইন ইন করুন" else "Sign in with Google / Email"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Login,
                        contentDescription = "Sign In",
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Digital Card 3D Preview (Flippable Front / Back with QR)
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DigitalBusinessCardView(
                    card = previewCard,
                    isFlipped = isFlipped,
                    onFlipClick = { isFlipped = !isFlipped }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isFlipped = !isFlipped },
                        modifier = Modifier
                            .size(38.dp)
                            .background(CardMateCyanAccent.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, CardMateCyanAccent.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = if (isFlipped) "Flip to Front" else "Flip to Back",
                            modifier = Modifier.size(20.dp),
                            tint = CardMateCyanAccent
                        )
                    }

                    IconButton(
                        onClick = { showQrDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .background(CardMateTealPrimary.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, CardMateTealPrimary.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Full QR Code",
                            modifier = Modifier.size(20.dp),
                            tint = CardMateTealPrimary
                        )
                    }
                }
            }
        }

        // CamCard Quick Share & Contact Action Hub (Icon Only)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share Hub
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CardMateTealPrimary.copy(alpha = 0.15f))
                            .clickable { showShareBottomSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Card",
                            tint = CardMateTealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Direct Image Share
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CardMateCyanAccent.copy(alpha = 0.15f))
                            .clickable { showShareBottomSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Share Image",
                            tint = CardMateCyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Large QR View
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFA78BFA).copy(alpha = 0.15f))
                            .clickable { showQrDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // NFC Beam
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CardMateGoldAccent.copy(alpha = 0.15f))
                            .clickable { onNavigateToNfc() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC Share",
                            tint = CardMateGoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Copy vCard info
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(vCardString))
                                Toast.makeText(
                                    context,
                                    if (isBangla) "vCard তথ্য কপি হয়েছে" else "vCard copied",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // VIEW MODE vs EDIT MODE
        // In View Mode: Read-only details are locked and shown directly.
        // Photo upload and card theme styles are shown ONLY in Edit Mode.
        // =========================================================================

        if (!isEditing) {
            // ---------------------------------------------------------------------
            // VIEW MODE: CamCard Clean Read-Only Profile View
            // ---------------------------------------------------------------------

            // 1. Identity & Organization Card
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "পরিচয় ও প্রতিষ্ঠান" else "Identity & Organization",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CardMateTealPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = userProfile.category.ifBlank { "Tech & IT" },
                                    color = CardMateTealPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileDisplayRow(
                            label = if (isBangla) "নাম" else "Full Name",
                            value = userProfile.fullName.ifBlank { "Mrinal Kanti Roy" },
                            icon = Icons.Default.Person,
                            isHighlighted = true
                        )

                        if (userProfile.jobTitle.isNotBlank()) {
                            ProfileDisplayRow(
                                label = if (isBangla) "পদবী" else "Job Title",
                                value = userProfile.jobTitle,
                                icon = Icons.Default.Work
                            )
                        }

                        if (userProfile.company.isNotBlank()) {
                            ProfileDisplayRow(
                                label = if (isBangla) "প্রতিষ্ঠান" else "Company",
                                value = userProfile.company,
                                icon = Icons.Default.Business
                            )
                        }

                        if (userProfile.department.isNotBlank()) {
                            ProfileDisplayRow(
                                label = if (isBangla) "বিভাগ / ব্রাঞ্চ" else "Department",
                                value = userProfile.department,
                                icon = Icons.Default.Business
                            )
                        }
                    }
                }
            }

            // 2. Phone Numbers Card (Dedicated SEPARATE ROW for every mobile number)
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "ফোন ও মোবাইল নম্বর" else "Phone & Mobile Numbers",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${userProfile.getResolvedPhones().size} ${if (isBangla) "টি নম্বর" else "numbers"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        val phones = userProfile.getResolvedPhones()
                        if (phones.isEmpty()) {
                            Text(
                                text = if (isBangla) "কোনো ফোন নম্বর যুক্ত নেই" else "No phone numbers added",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Render EACH phone number on its OWN DEDICATED SEPARATE ROW
                            phones.forEach { item ->
                                PhoneItemSeparateRow(
                                    item = item,
                                    isBangla = isBangla,
                                    context = context,
                                    clipboardManager = clipboardManager
                                )
                            }
                        }
                    }
                }
            }

            // 3. Email Addresses Card (Dedicated separate rows)
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "ইমেইল ঠিকানা" else "Email Addresses",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        val emails = userProfile.getResolvedEmails()
                        if (emails.isEmpty()) {
                            Text(
                                text = if (isBangla) "কোনো ইমেইল যুক্ত নেই" else "No email addresses added",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            emails.forEach { item ->
                                EmailItemSeparateRow(
                                    item = item,
                                    isBangla = isBangla,
                                    context = context,
                                    clipboardManager = clipboardManager
                                )
                            }
                        }
                    }
                }
            }

            // 4. Websites & Digital Links Card
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "ওয়েবসাইট ও পোর্টফোলিও" else "Websites & Links",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        val websites = userProfile.getResolvedWebsites()
                        if (websites.isEmpty()) {
                            Text(
                                text = if (isBangla) "কোনো ওয়েবসাইট লিংক যুক্ত নেই" else "No websites added",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            websites.forEach { item ->
                                WebsiteItemSeparateRow(
                                    item = item,
                                    isBangla = isBangla,
                                    context = context,
                                    clipboardManager = clipboardManager
                                )
                            }
                        }
                    }
                }
            }

            // 5. Address & Location Card
            if (userProfile.address.isNotBlank()) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "ঠিকানা ও অবস্থান" else "Office & Location",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = userProfile.address,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(userProfile.address)}")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                                context.startActivity(mapIntent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, userProfile.address, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Open Map",
                                            tint = CardMateTealPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(userProfile.address))
                                            Toast.makeText(context, if (isBangla) "ঠিকানা কপি করা হয়েছে" else "Address copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Social Media & Messenger Card
            val socials = userProfile.getResolvedSocials()
            if (socials.isNotEmpty()) {
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
                            Text(
                                text = if (isBangla) "সোশ্যাল মিডিয়া ও মেসেঞ্জার" else "Social Profiles & Messengers",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            socials.forEach { item ->
                                SocialItemSeparateRow(
                                    item = item,
                                    isBangla = isBangla,
                                    context = context,
                                    clipboardManager = clipboardManager
                                )
                            }
                        }
                    }
                }
            }

            // 7. Bio / Summary Card
            if (userProfile.bio.isNotBlank()) {
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isBangla) "সংক্ষিপ্ত বায়ো ও পেশাগত বিবরণ" else "Professional Bio & Summary",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Text(
                                text = userProfile.bio,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

        } else {
            // ---------------------------------------------------------------------
            // EDIT MODE: Photo, Theme, and Dynamic Multi-Field Form with '+' Buttons
            // ---------------------------------------------------------------------

            // Section 0A: Profile Photo & Avatar (Camera/Gallery picker)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clickable { showAvatarPicker = true }
                        ) {
                            if (!photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, CardMateTealPrimary, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(CardMateTealPrimary.copy(alpha = 0.2f))
                                        .border(2.dp, CardMateTealPrimary.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = CardMateTealPrimary,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = CardMateTealPrimary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Upload Photo",
                                        tint = Color(0xFF042F2E),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "প্রোফাইল ছবি ও লোগো" else "Profile Photo & Logo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (!photoUri.isNullOrBlank()) {
                                    if (isBangla) "কাস্টম ছবি সংযুক্ত আছে" else "Custom photo attached"
                                } else {
                                    if (isBangla) "ক্যামেরা বা গ্যালারি থেকে ছবি যোগ করুন" else "Attach photo from camera or gallery"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showAvatarPicker = true },
                            modifier = Modifier
                                .size(38.dp)
                                .background(CardMateTealPrimary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Pick Photo",
                                tint = Color(0xFF042F2E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Section 0B: Card Theme & Design Style Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "কার্ড থিম ও ডিজাইন শৈলী" else "Card Theme & Design Style",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(CardTemplate.entries.toList()) { template ->
                                val isSelected = selectedTemplate == template.id
                                val templateName = if (isBangla) template.displayNameBn else template.displayNameEn
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(template.primaryBgColor),
                                    modifier = Modifier
                                        .width(115.dp)
                                        .height(64.dp)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) CardMateTealPrimary else Color.Gray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedTemplate = template.id
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = templateName,
                                                color = Color(template.textColor),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(template.accentColor),
                                                    modifier = Modifier.size(14.dp)
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

            // Section 1: Personal & Identity
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isBangla) "ব্যক্তিগত ও পেশাগত মূল তথ্য" else "Personal & Professional Identity",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text(if (isBangla) "পূর্ণ নাম *" else "Full Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardMateTealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Job Title
                        OutlinedTextField(
                            value = jobTitle,
                            onValueChange = { jobTitle = it },
                            label = { Text(if (isBangla) "পদবী / ডেজিগনেশন" else "Job Title / Role") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardMateTealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Company
                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text(if (isBangla) "কোম্পানি / প্রতিষ্ঠান" else "Company / Organization") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardMateTealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Department
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text(if (isBangla) "বিভাগ / ডিপার্টমেন্ট" else "Department / Division") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardMateTealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Category
                        Column {
                            Text(
                                text = if (isBangla) "ক্যাটাগরি / ইন্ডাস্ট্রি" else "Category / Industry",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CardCategory.values()) { cat ->
                                    val isSelected = category == cat.englishName
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.clickable { category = cat.englishName }
                                    ) {
                                        Text(
                                            text = if (isBangla) cat.banglaName else cat.englishName,
                                            color = if (isSelected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: MULTI-PHONE NUMBERS (Separate Row for every number + '+' Add Phone Button - Icon Only)
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = CardMateTealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "ফোন ও মোবাইল নম্বর" else "Phone Numbers",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // '+' Add Phone Number Button (Icon only)
                            IconButton(
                                onClick = {
                                    val nextLabel = when (editPhones.size) {
                                        0 -> "Mobile"
                                        1 -> "Work"
                                        2 -> "WhatsApp"
                                        else -> "Other"
                                    }
                                    editPhones.add(ProfileFieldItem(label = nextLabel, value = ""))
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(CardMateTealPrimary.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, CardMateTealPrimary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Phone",
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isBangla) "প্রতিটি নম্বর পৃথক সারিতে যুক্ত করুন এবং ধরন নির্বাচন করুন" else "Each phone number is displayed in its own separate row",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Render each phone on its OWN SEPARATE ROW
                        editPhones.forEachIndexed { index, phoneItem ->
                            DynamicFieldEditorRow(
                                item = phoneItem,
                                labelOptions = listOf("Mobile", "Work", "WhatsApp", "Home", "Fax", "Direct", "Other"),
                                placeholder = if (isBangla) "যেমন: +৮৮০১৭১৯..." else "e.g. +880 1719...",
                                keyboardType = KeyboardType.Phone,
                                canDelete = editPhones.size > 1,
                                onUpdate = { updated ->
                                    val split = com.example.util.PhoneNumberUtils.splitPhoneNumbers(updated.value)
                                    if (split.size > 1) {
                                        editPhones[index] = updated.copy(value = split[0])
                                        split.drop(1).forEachIndexed { subIdx, num ->
                                            editPhones.add(index + 1 + subIdx, com.example.data.model.ProfileFieldItem(label = "Mobile", value = num))
                                        }
                                    } else {
                                        editPhones[index] = updated
                                    }
                                },
                                onDelete = {
                                    editPhones.removeAt(index)
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: MULTI-EMAIL ADDRESSES ('+' Add Email Button - Icon Only)
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = CardMateTealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "ইমেইল ঠিকানা" else "Email Addresses",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // '+' Add Email Button (Icon only)
                            IconButton(
                                onClick = {
                                    val nextLabel = if (editEmails.isEmpty()) "Work" else "Personal"
                                    editEmails.add(ProfileFieldItem(label = nextLabel, value = ""))
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(CardMateTealPrimary.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, CardMateTealPrimary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Email",
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        editEmails.forEachIndexed { index, emailItem ->
                            DynamicFieldEditorRow(
                                item = emailItem,
                                labelOptions = listOf("Work", "Personal", "Other"),
                                placeholder = "name@company.com",
                                keyboardType = KeyboardType.Email,
                                canDelete = editEmails.size > 1,
                                onUpdate = { updated ->
                                    editEmails[index] = updated
                                },
                                onDelete = {
                                    editEmails.removeAt(index)
                                }
                            )
                        }
                    }
                }
            }

            // Section 4: MULTI-WEBSITES ('+' Add Website Button - Icon Only)
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = CardMateTealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "ওয়েবসাইট ও পোর্টফোলিও" else "Websites & Links",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // '+' Add Website Button (Icon only)
                            IconButton(
                                onClick = {
                                    val nextLabel = if (editWebsites.isEmpty()) "Company" else "Portfolio"
                                    editWebsites.add(ProfileFieldItem(label = nextLabel, value = ""))
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(CardMateTealPrimary.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, CardMateTealPrimary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Website",
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        editWebsites.forEachIndexed { index, websiteItem ->
                            DynamicFieldEditorRow(
                                item = websiteItem,
                                labelOptions = listOf("Company", "Portfolio", "Blog", "Shop", "Other"),
                                placeholder = "https://example.com",
                                keyboardType = KeyboardType.Uri,
                                canDelete = editWebsites.size > 1,
                                onUpdate = { updated ->
                                    editWebsites[index] = updated
                                },
                                onDelete = {
                                    editWebsites.removeAt(index)
                                }
                            )
                        }
                    }
                }
            }

            // Section 5: MULTI-SOCIAL & MESSENGERS ('+' Add Social Button - Icon Only)
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = CardMateTealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "সোশ্যাল মিডিয়া ও মেসেঞ্জার" else "Social & Messengers",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // '+' Add Social Button (Icon only)
                            IconButton(
                                onClick = {
                                    val nextLabel = when (editSocials.size) {
                                        0 -> "WhatsApp"
                                        1 -> "LinkedIn"
                                        2 -> "GitHub"
                                        else -> "Twitter"
                                    }
                                    editSocials.add(ProfileFieldItem(label = nextLabel, value = ""))
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(CardMateTealPrimary.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, CardMateTealPrimary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Social",
                                    tint = CardMateTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        editSocials.forEachIndexed { index, socialItem ->
                            DynamicFieldEditorRow(
                                item = socialItem,
                                labelOptions = listOf("WhatsApp", "LinkedIn", "GitHub", "Twitter", "Facebook", "Telegram", "WeChat", "YouTube"),
                                placeholder = if (socialItem.label == "WhatsApp") "+8801719..." else "username or url",
                                keyboardType = KeyboardType.Text,
                                canDelete = editSocials.size > 1,
                                onUpdate = { updated ->
                                    editSocials[index] = updated
                                },
                                onDelete = {
                                    editSocials.removeAt(index)
                                }
                            )
                        }
                    }
                }
            }

            // Section 6: Address & Location Notes
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isBangla) "ঠিকানা ও বায়ো" else "Address & Bio Summary",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Address
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(if (isBangla) "ঠিকানা / অফিস লোকেশন" else "Address / Office Location") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardMateTealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Bio
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text(if (isBangla) "সংক্ষিপ্ত বায়ো / পেশাগত বিবরণ" else "Short Bio / Summary") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardMateTealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            // Section 7: Save & Cancel Buttons Bar (Icon-centric controls)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isEditing = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val updated = UserProfile(
                                fullName = fullName.trim().ifBlank { userProfile.fullName },
                                jobTitle = jobTitle.trim(),
                                company = company.trim(),
                                department = department.trim(),
                                phone = editPhones.firstOrNull { it.value.isNotBlank() }?.value ?: "",
                                secondaryPhone = editPhones.getOrNull(1)?.value ?: "",
                                email = editEmails.firstOrNull { it.value.isNotBlank() }?.value ?: "",
                                website = editWebsites.firstOrNull { it.value.isNotBlank() }?.value ?: "",
                                address = address.trim(),
                                category = category,
                                bio = bio.trim(),
                                socialLinks = editSocials.filter { it.value.isNotBlank() }.joinToString(" | ") { "${it.label}: ${it.value}" },
                                photoUri = photoUri,
                                companyLogoUri = companyLogoUri,
                                cardLayoutTemplate = selectedTemplate,
                                phoneList = editPhones.filter { it.value.isNotBlank() },
                                emailList = editEmails.filter { it.value.isNotBlank() },
                                websiteList = editWebsites.filter { it.value.isNotBlank() },
                                socialList = editSocials.filter { it.value.isNotBlank() }
                            )
                            viewModel.saveUserProfile(updated)
                            isEditing = false
                            Toast.makeText(
                                context,
                                if (isBangla) "প্রোফাইল সংরক্ষিত ও লক করা হয়েছে" else "Profile saved & locked",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                            .background(CardMateTealPrimary, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Profile",
                            tint = Color(0xFF042F2E),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }

    // Full Screen QR Code Dialog (CamCard Scan Style - Icon Only Actions)
    if (showQrDialog) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = fullName.ifBlank { "My Business Card" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = jobTitle.ifBlank { company },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .size(240.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        if (fullQrBitmap != null) {
                            Image(
                                bitmap = fullQrBitmap.asImageBitmap(),
                                contentDescription = "Full Screen QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text("QR Code Error", color = Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isBangla) "যেকোনো স্মার্টফোন ক্যামেরা দিয়ে স্ক্যান করে সাথে সাথে কন্টাক্ট সেভ করুন।"
                        else "Scan with any smartphone camera to instantly save contact details.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy vCard
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(vCardString))
                                Toast.makeText(
                                    context,
                                    if (isBangla) "vCard ডাটা কপি হয়েছে!" else "vCard data copied!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy vCard",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Open in Dynamic QR Studio
                        IconButton(
                            onClick = {
                                showQrDialog = false
                                showDynamicQrStudio = true
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(CardMateCyanAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Customize in Studio",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF082F49)
                            )
                        }

                        // Share
                        IconButton(
                            onClick = {
                                viewModel.exportUserProfileVCard(context)
                                showQrDialog = false
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(CardMateTealPrimary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF042F2E)
                            )
                        }

                        // Close
                        IconButton(
                            onClick = { showQrDialog = false },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Dialog",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Dynamic QR Generator Studio Dialog
    if (showDynamicQrStudio) {
        DynamicQrGeneratorDialog(
            viewModel = viewModel,
            initialCard = null,
            onDismiss = { showDynamicQrStudio = false }
        )
    }

    // Avatar & Phone Photo Picker Bottom Sheet
    if (showAvatarPicker) {
        ProfilePhotoPickerBottomSheet(
            currentPhotoUri = photoUri,
            isBangla = isBangla,
            onPhotoSelected = { newPhotoUri ->
                photoUri = newPhotoUri
                val updated = activeProfile.copy(photoUri = newPhotoUri)
                viewModel.saveUserProfile(updated)
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    // Share Options Bottom Sheet (vCard & Image Export)
    if (showShareBottomSheet) {
        CardShareBottomSheet(
            card = previewCard,
            isBangla = isBangla,
            onDismiss = { showShareBottomSheet = false }
        )
    }
}

/**
 * Clean display row for general text attributes in View Mode
 */
@Composable
private fun ProfileDisplayRow(
    label: String,
    value: String,
    icon: ImageVector,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHighlighted) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = if (isHighlighted) 15.sp else 13.5.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Dedicated SEPARATE ROW for each individual phone number in View Mode with Direct Call, WhatsApp & Copy actions.
 */
@Composable
private fun PhoneItemSeparateRow(
    item: ProfileFieldItem,
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
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.value}"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, item.value, Toast.LENGTH_SHORT).show()
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
                // Label Badge (Mobile, Work, WhatsApp, Home, etc.)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (item.label.lowercase()) {
                        "whatsapp" -> Color(0xFF25D366).copy(alpha = 0.18f)
                        "work" -> CardMateCyanAccent.copy(alpha = 0.18f)
                        "home" -> Color(0xFFA78BFA).copy(alpha = 0.18f)
                        else -> CardMateTealPrimary.copy(alpha = 0.18f)
                    }
                ) {
                    Text(
                        text = item.label,
                        color = when (item.label.lowercase()) {
                            "whatsapp" -> Color(0xFF16A34A)
                            "work" -> CardMateCyanAccent
                            "home" -> Color(0xFFA78BFA)
                            else -> CardMateTealPrimary
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = item.value,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Interactive Action Buttons: Call, SMS, WhatsApp, Copy
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Direct Phone Call Button
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.value}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, item.value, Toast.LENGTH_SHORT).show()
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

                // SMS Direct Message Button
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${item.value}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, item.value, Toast.LENGTH_SHORT).show()
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

                // WhatsApp Direct Chat Button
                IconButton(
                    onClick = {
                        try {
                            val cleanNumber = item.value.replace(Regex("[^0-9+]"), "")
                            val waUri = Uri.parse("https://wa.me/$cleanNumber")
                            val intent = Intent(Intent.ACTION_VIEW, waUri)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, item.value, Toast.LENGTH_SHORT).show()
                        }
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

                // Copy Number
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(item.value))
                        Toast.makeText(
                            context,
                            if (isBangla) "${item.value} কপি হয়েছে" else "Phone number copied",
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
 * Dedicated SEPARATE ROW for Email item in View Mode
 */
@Composable
private fun EmailItemSeparateRow(
    item: ProfileFieldItem,
    isBangla: Boolean,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
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
                        text = item.label,
                        color = CardMateTealPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = item.value,
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
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${item.value}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, item.value, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Send Email",
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(item.value))
                        Toast.makeText(context, if (isBangla) "ইমেইল কপি হয়েছে" else "Email copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated SEPARATE ROW for Website item in View Mode
 */
@Composable
private fun WebsiteItemSeparateRow(
    item: ProfileFieldItem,
    isBangla: Boolean,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
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
                        text = item.label,
                        color = CardMateCyanAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = item.value,
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
                            val url = if (item.value.startsWith("http://") || item.value.startsWith("https://")) item.value else "https://${item.value}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, item.value, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Open URL",
                        tint = CardMateCyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(item.value))
                        Toast.makeText(context, if (isBangla) "লিংক কপি হয়েছে" else "Link copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated SEPARATE ROW for Social Profiles & Messengers
 */
@Composable
private fun SocialItemSeparateRow(
    item: ProfileFieldItem,
    isBangla: Boolean,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val openLink = {
        try {
            val url = when {
                item.value.startsWith("http://", ignoreCase = true) || item.value.startsWith("https://", ignoreCase = true) -> item.value
                item.label.contains("linkedin", ignoreCase = true) -> "https://www.linkedin.com/in/${item.value.removePrefix("@")}"
                item.label.contains("twitter", ignoreCase = true) || item.label.contains("x", ignoreCase = true) -> "https://x.com/${item.value.removePrefix("@")}"
                item.label.contains("facebook", ignoreCase = true) -> "https://www.facebook.com/${item.value.removePrefix("@")}"
                item.label.contains("instagram", ignoreCase = true) -> "https://www.instagram.com/${item.value.removePrefix("@")}"
                item.label.contains("github", ignoreCase = true) -> "https://github.com/${item.value.removePrefix("@")}"
                item.label.contains("telegram", ignoreCase = true) -> "https://t.me/${item.value.removePrefix("@")}"
                else -> "https://${item.value}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, item.value, Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { openLink() }
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
                        text = item.label,
                        color = Color(0xFFA78BFA),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = item.value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(
                    onClick = openLink,
                    modifier = Modifier.size(34.dp)
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
                        clipboardManager.setText(AnnotatedString(item.value))
                        Toast.makeText(context, if (isBangla) "কপি হয়েছে" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dynamic Multi-field item editor row for Edit Mode:
 * Features a Label selector dropdown, full-width value input field, and a delete button!
 */
@Composable
private fun DynamicFieldEditorRow(
    item: ProfileFieldItem,
    labelOptions: List<String>,
    placeholder: String,
    keyboardType: KeyboardType,
    canDelete: Boolean,
    onUpdate: (ProfileFieldItem) -> Unit,
    onDelete: () -> Unit
) {
    var expandedLabelMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label Dropdown Selector
        Box {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { expandedLabelMenu = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CardMateTealPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Label",
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expandedLabelMenu,
                onDismissRequest = { expandedLabelMenu = false }
            ) {
                labelOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, fontSize = 13.sp) },
                        onClick = {
                            expandedLabelMenu = false
                            onUpdate(item.copy(label = opt))
                        }
                    )
                }
            }
        }

        // Value Input Field
        OutlinedTextField(
            value = item.value,
            onValueChange = { onUpdate(item.copy(value = it)) },
            placeholder = { Text(placeholder, fontSize = 12.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CardMateTealPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Delete Button
        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove item",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
