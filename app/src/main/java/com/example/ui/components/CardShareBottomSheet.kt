package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BusinessCard
import com.example.sync.CardImageGenerator
import com.example.sync.VCardExporter
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardShareBottomSheet(
    card: BusinessCard,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onOpenQrStudio: ((BusinessCard) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(CardMateTealPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = CardMateTealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBangla) "কার্ড শেয়ার করুন" else "Share Business Card",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = card.fullName.ifBlank { "Smart Card" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CardMateTealPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isBangla) "ইমেজ প্রস্তুত হচ্ছে..." else "Preparing high-resolution card...",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: vCard Contact File (.vcf)
                    item {
                        ShareOptionItem(
                            icon = Icons.Default.ContactPage,
                            iconColor = CardMateTealPrimary,
                            title = if (isBangla) "vCard কন্টাক্ট ফাইল (.vcf)" else "vCard Contact File (.vcf)",
                            subtitle = if (isBangla) "WhatsApp বা ইমেইলে সরাসরি ফোনে কন্টাক্ট সেভ করার ফাইল পাঠান" else "Send direct 1-tap contact import file for iOS & Android",
                            badge = if (isBangla) "জনপ্রিয়" else "Popular",
                            onClick = {
                                VCardExporter.shareCardAsVCardFile(context, card)
                                onDismiss()
                            }
                        )
                    }

                    // Option 2: Card Image (Front Side HD)
                    item {
                        ShareOptionItem(
                            icon = Icons.Default.Image,
                            iconColor = CardMateCyanAccent,
                            title = if (isBangla) "কার্ড ইমেজ - সামনের দিক (HD PNG)" else "Card Image - Front Side (HD PNG)",
                            subtitle = if (isBangla) "হাই-রেজোলিউশন প্রফেশনাল ভিজিটিং কার্ড ছবি" else "High-resolution digital business card graphic",
                            badge = "HD",
                            onClick = {
                                isExporting = true
                                coroutineScope.launch {
                                    CardImageGenerator.shareCardAsImage(
                                        context = context,
                                        card = card,
                                        style = CardImageGenerator.ShareImageStyle.FRONT_ONLY,
                                        onComplete = {
                                            isExporting = false
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        )
                    }

                    // Option 3: Full Dual Card Portfolio (Front + Back QR Code)
                    item {
                        ShareOptionItem(
                            icon = Icons.Default.CreditCard,
                            iconColor = CardMateGoldAccent,
                            title = if (isBangla) "উভয় দিকসহ কার্ড পোর্টফোলিও" else "Full Card Portfolio (Front + Back QR)",
                            subtitle = if (isBangla) "সামনের ডিজাইন ও পেছনের স্ক্যানযোগ্য QR কোডসহ সম্পূর্ণ কার্ড ইমেজ" else "Complete graphic with front design & scannable vCard QR code",
                            badge = if (isBangla) "প্রো" else "Pro",
                            onClick = {
                                isExporting = true
                                coroutineScope.launch {
                                    CardImageGenerator.shareCardAsImage(
                                        context = context,
                                        card = card,
                                        style = CardImageGenerator.ShareImageStyle.DUAL_CARD,
                                        onComplete = {
                                            isExporting = false
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        )
                    }

                    // Option 3.5: Dynamic QR Code Generator & Studio
                    if (onOpenQrStudio != null) {
                        item {
                            ShareOptionItem(
                                icon = Icons.Default.QrCode2,
                                iconColor = CardMateTealPrimary,
                                title = if (isBangla) "ডায়নামিক কিউআর জেনারেটর স্টুডিও" else "Dynamic QR Generator & Studio",
                                subtitle = if (isBangla) "কাস্টম কালার, সেন্টার লোগো ও vCard/MECARD ফরম্যাটে লাইভ কিউআর কোড তৈরি করুন" else "Customize colors, center emblems, and live generate styled contact QR code",
                                badge = "ZXing AI",
                                onClick = {
                                    onDismiss()
                                    onOpenQrStudio(card)
                                }
                            )
                        }
                    }

                    // Option 4: Scannable QR Code Image
                    item {
                        ShareOptionItem(
                            icon = Icons.Default.QrCode2,
                            iconColor = Color(0xFFA78BFA),
                            title = if (isBangla) "স্ক্যানযোগ্য QR কোড ইমেজ" else "Scannable QR Code Image",
                            subtitle = if (isBangla) "ক্যামেরা দিয়ে স্ক্যান করে তথ্য সংরক্ষণের জন্য কিউআর কোড ছবি" else "Share QR code graphic for instant mobile camera scanning",
                            onClick = {
                                isExporting = true
                                coroutineScope.launch {
                                    CardImageGenerator.shareCardAsImage(
                                        context = context,
                                        card = card,
                                        style = CardImageGenerator.ShareImageStyle.BACK_QR_ONLY,
                                        onComplete = {
                                            isExporting = false
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        )
                    }

                    // Option 5: Save Card Image to Gallery
                    item {
                        ShareOptionItem(
                            icon = Icons.Default.Download,
                            iconColor = Color(0xFF34D399),
                            title = if (isBangla) "গ্যালারিতে কার্ড ইমেজ সেভ করুন" else "Save Card Image to Gallery",
                            subtitle = if (isBangla) "ফোনের ফটো গ্যালারিতে PNG ইমেজ আকারে ডাউনলোড করুন" else "Download crisp PNG image to your device storage / Photos",
                            onClick = {
                                coroutineScope.launch {
                                    CardImageGenerator.saveCardImageToGallery(
                                        context = context,
                                        card = card,
                                        style = CardImageGenerator.ShareImageStyle.FRONT_ONLY
                                    )
                                    onDismiss()
                                }
                            }
                        )
                    }

                    // Option 6: Direct WhatsApp Share
                    item {
                        ShareOptionItem(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            iconColor = Color(0xFF25D366),
                            title = if (isBangla) "হোয়াটসঅ্যাপে সরাসরি শেয়ার" else "Share directly to WhatsApp",
                            subtitle = if (isBangla) "হোয়াটসঅ্যাপে কার্ডের ডিজিটাল পরিচিতি ও তথ্য সেন্ড করুন" else "Share visiting card details and vCard text directly in WhatsApp chats",
                            badge = "WhatsApp",
                            onClick = {
                                val shareText = buildString {
                                    append("📇 *${card.fullName}*\n")
                                    if (card.jobTitle.isNotBlank()) append("💼 ${card.jobTitle}\n")
                                    if (card.company.isNotBlank()) append("🏢 ${card.company}\n")
                                    if (card.phone.isNotBlank()) append("📞 ${card.phone}\n")
                                    if (card.email.isNotBlank()) append("✉️ ${card.email}\n")
                                    if (card.website.isNotBlank()) append("🌐 ${card.website}\n")
                                    if (card.address.isNotBlank()) append("📍 ${card.address}\n")
                                    append("\n_Shared via CardMate - Smart Visiting Card & Cloud Vault_")
                                }
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    setPackage("com.whatsapp")
                                }
                                try {
                                    context.startActivity(sendIntent)
                                } catch (e: Exception) {
                                    val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(fallbackIntent, "Share with"))
                                }
                                onDismiss()
                            }
                        )
                    }

                    // Option 7: Raw Text / Snippet
                    item {
                        ShareOptionItem(
                            icon = Icons.Default.TextFields,
                            iconColor = Color(0xFF94A3B8),
                            title = if (isBangla) "vCard টেক্সট শেয়ার" else "Share vCard Text Snippet",
                            subtitle = if (isBangla) "মেসেজ বা টেক্সট আকারে কন্টাক্ট ফিল্ড শেয়ার করুন" else "Plain vCard text code for clipboard and messaging",
                            onClick = {
                                VCardExporter.shareCardAsVCard(context, card)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareOptionItem(
    icon: ImageVector? = null,
    painter: Painter? = null,
    iconColor: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = iconColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
