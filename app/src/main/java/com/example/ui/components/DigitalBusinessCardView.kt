package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BusinessCard
import com.example.sync.VCardExporter

fun resolveFontFamily(type: String): FontFamily {
    return when (type) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        "condensed" -> FontFamily.SansSerif
        else -> FontFamily.SansSerif
    }
}

@Composable
fun DigitalBusinessCardView(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false,
    onFlipClick: () -> Unit = {},
    showSyncBadges: Boolean = true,
    onPhotoClick: (() -> Unit)? = null
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "cardFlip"
    )

    val sanitizedCard = remember(card) { cleanBusinessCard(card) }
    val primaryBg = Color(sanitizedCard.effectivePrimaryBgColor)
    val secondaryBg = Color(sanitizedCard.effectiveSecondaryBgColor)
    val accentColor = Color(sanitizedCard.effectiveAccentColor)
    val textColor = Color(sanitizedCard.effectiveTextColor)
    val cornerRadius = sanitizedCard.cornerRadiusDp.coerceIn(0, 32).dp
    val fontFamily = resolveFontFamily(sanitizedCard.fontFamilyType)

    val borderWidth = when (sanitizedCard.borderStyle) {
        "none" -> 0.dp
        "bold" -> 2.dp
        "glow" -> 1.5.dp
        else -> 1.dp
    }
    val borderColor = when (sanitizedCard.borderStyle) {
        "none" -> Color.Transparent
        "glow" -> accentColor.copy(alpha = 0.8f)
        "bold" -> accentColor.copy(alpha = 0.7f)
        else -> accentColor.copy(alpha = 0.35f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.75f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable { onFlipClick() }
            .testTag("digital_business_card_${sanitizedCard.id}"),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = if (sanitizedCard.borderStyle == "glow") 10.dp else 6.dp)
    ) {
        if (rotation <= 90f) {
            // Front Side with custom layout, colors, typography, patterns
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        getBackgroundModifier(
                            primary = primaryBg,
                            secondary = secondaryBg,
                            accent = accentColor,
                            pattern = sanitizedCard.bgPattern
                        )
                    )
                    .then(
                        if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
                        else Modifier
                    )
            ) {
                when (sanitizedCard.layoutStyle) {
                    "executive_classic" -> ExecutiveClassicLayout(sanitizedCard, accentColor, textColor, fontFamily, showSyncBadges, onFlipClick)
                    "minimalist_clean" -> MinimalistCleanLayout(sanitizedCard, accentColor, textColor, fontFamily, showSyncBadges, onFlipClick)
                    "cyber_badge" -> CyberBadgeLayout(sanitizedCard, accentColor, textColor, fontFamily, showSyncBadges, onFlipClick)
                    "split_duotone" -> SplitDuotoneLayout(sanitizedCard, accentColor, textColor, fontFamily, showSyncBadges, onFlipClick, onPhotoClick)
                    "vertical_showcase" -> VerticalShowcaseLayout(sanitizedCard, accentColor, textColor, fontFamily, showSyncBadges, onFlipClick)
                    else -> ModernFloatingLayout(sanitizedCard, accentColor, textColor, fontFamily, showSyncBadges, onFlipClick, onPhotoClick)
                }
            }
        } else {
            // Back Side (Rotated 180)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .then(
                        getBackgroundModifier(
                            primary = secondaryBg,
                            secondary = primaryBg,
                            accent = accentColor,
                            pattern = sanitizedCard.bgPattern
                        )
                    )
                    .then(
                        if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
                        else Modifier
                    )
            ) {
                CardBackContent(card = sanitizedCard, accentColor = accentColor, textColor = textColor, fontFamily = fontFamily)
            }
        }
    }
}

private fun getBackgroundModifier(
    primary: Color,
    secondary: Color,
    accent: Color,
    pattern: String
): Modifier {
    return when (pattern) {
        "solid" -> Modifier.background(primary)
        "mesh" -> Modifier.background(
            Brush.radialGradient(
                colors = listOf(secondary, primary, primary.copy(alpha = 0.95f)),
                radius = 600f
            )
        )
        "dots" -> Modifier
            .background(Brush.linearGradient(listOf(primary, secondary)))
            .drawBehind {
                val dotRadius = 1.dp.toPx()
                val spacing = 18.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) {
                        drawCircle(
                            color = accent.copy(alpha = 0.08f),
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += spacing
                    }
                    x += spacing
                }
            }
        "stripes" -> Modifier
            .background(Brush.linearGradient(listOf(primary, secondary)))
            .drawBehind {
                val stripeWidth = 2.dp.toPx()
                val step = 24.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = accent.copy(alpha = 0.06f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = stripeWidth
                    )
                    x += step
                }
            }
        "geometric" -> Modifier
            .background(Brush.linearGradient(listOf(primary, secondary)))
            .drawBehind {
                drawLine(
                    color = accent.copy(alpha = 0.15f),
                    start = Offset(size.width * 0.65f, 0f),
                    end = Offset(size.width, size.height * 0.7f),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = accent.copy(alpha = 0.1f),
                    start = Offset(size.width * 0.75f, 0f),
                    end = Offset(size.width, size.height * 0.45f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        else -> Modifier.background(Brush.linearGradient(listOf(primary, secondary)))
    }
}

// 1. MODERN FLOATING LAYOUT
@Composable
private fun ModernFloatingLayout(
    card: BusinessCard,
    accentColor: Color,
    textColor: Color,
    fontFamily: FontFamily,
    showSyncBadges: Boolean,
    onFlipToQr: () -> Unit,
    onPhotoClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Badges at top right
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (card.showQrBadge) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(30.dp).clickable { onFlipToQr() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCode2, contentDescription = "QR", tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (card.showNfcBadge) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Nfc, contentDescription = "NFC", tint = accentColor, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Category tag & Company
            Row(
                modifier = Modifier.fillMaxWidth(0.72f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (card.showCategoryBadge && card.category.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.22f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = card.category.uppercase(),
                            color = accentColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (card.company.isNotBlank()) {
                    Text(
                        text = card.company,
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Middle: Avatar + Name + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (card.showAvatar) {
                    if (!card.cardFrontImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = card.cardFrontImageUri,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, accentColor, CircleShape)
                                .clickable { onPhotoClick?.invoke() ?: onFlipToQr() }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        val initial = if (card.fullName.isNotBlank()) card.fullName.trim().first().uppercase() else "C"
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.22f))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                                .clickable { onPhotoClick?.invoke() ?: onFlipToQr() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                color = accentColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.fullName.ifBlank { "Full Name" },
                        color = textColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (card.jobTitle.isNotBlank()) {
                        Text(
                            text = card.jobTitle,
                            color = accentColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = fontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom: Contacts & Sync Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (card.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(card.phone, color = textColor.copy(alpha = 0.85f), fontSize = 10.5.sp, fontFamily = fontFamily, maxLines = 1)
                        }
                    }
                    if (card.email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(card.email, color = textColor.copy(alpha = 0.85f), fontSize = 10.5.sp, fontFamily = fontFamily, maxLines = 1)
                        }
                    }
                }

                if (showSyncBadges) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (card.isFavorite) Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(15.dp))
                        if (card.isSyncedWithGoogleContacts) Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                        if (card.isBackedUpToCloud) Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// 2. EXECUTIVE CLASSIC LAYOUT (Formal, Symmetrical, Crest/Divider Line)
@Composable
private fun ExecutiveClassicLayout(
    card: BusinessCard,
    accentColor: Color,
    textColor: Color,
    fontFamily: FontFamily,
    showSyncBadges: Boolean,
    onFlipToQr: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: company / category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card.company.ifBlank { "CORPORATION" }.uppercase(),
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                letterSpacing = 1.2.sp
            )
            if (card.showQrBadge) {
                Icon(
                    Icons.Default.QrCode2,
                    contentDescription = "QR",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp).clickable { onFlipToQr() }
                )
            }
        }

        // Center Identity Block
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = card.fullName.ifBlank { "Executive Name" },
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center
            )
            if (card.jobTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = card.jobTitle.uppercase(),
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = fontFamily,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(1.5.dp)
                    .background(accentColor)
            )
        }

        // Bottom Symmetrical Contact Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (card.phone.isNotBlank()) {
                Text(card.phone, color = textColor.copy(alpha = 0.85f), fontSize = 10.sp, fontFamily = fontFamily)
            }
            if (card.email.isNotBlank()) {
                Text("•", color = accentColor, fontSize = 10.sp)
                Text(card.email, color = textColor.copy(alpha = 0.85f), fontSize = 10.sp, fontFamily = fontFamily)
            }
            if (card.website.isNotBlank()) {
                Text("•", color = accentColor, fontSize = 10.sp)
                Text(card.website.removePrefix("https://").removePrefix("http://"), color = accentColor, fontSize = 10.sp, fontFamily = fontFamily)
            }
        }
    }
}

// 3. MINIMALIST CLEAN LAYOUT (Airy, Left-Aligned, Bold Typography)
@Composable
private fun MinimalistCleanLayout(
    card: BusinessCard,
    accentColor: Color,
    textColor: Color,
    fontFamily: FontFamily,
    showSyncBadges: Boolean,
    onFlipToQr: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = card.fullName.ifBlank { "Full Name" },
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = fontFamily
                )
                if (card.jobTitle.isNotBlank() || card.company.isNotBlank()) {
                    Text(
                        text = "${card.jobTitle} ${if (card.jobTitle.isNotBlank() && card.company.isNotBlank()) "—" else ""} ${card.company}",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = fontFamily
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (card.phone.isNotBlank()) Text(card.phone, color = textColor.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = fontFamily)
                    if (card.email.isNotBlank()) Text(card.email, color = textColor.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = fontFamily)
                    if (card.website.isNotBlank()) Text(card.website, color = accentColor, fontSize = 10.sp, fontFamily = fontFamily)
                }

                if (card.showQrBadge) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = textColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp).clickable { onFlipToQr() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// 4. CYBERPUNK TECH BADGE (Monospace, Telemetry, Neon Accents)
@Composable
private fun CyberBadgeLayout(
    card: BusinessCard,
    accentColor: Color,
    textColor: Color,
    fontFamily: FontFamily,
    showSyncBadges: Boolean,
    onFlipToQr: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Tech Corner Marks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val markLen = 10.dp.toPx()
            val stroke = 1.5.dp.toPx()
            // Top-left
            drawLine(accentColor, Offset(0f, 0f), Offset(markLen, 0f), stroke)
            drawLine(accentColor, Offset(0f, 0f), Offset(0f, markLen), stroke)
            // Top-right
            drawLine(accentColor, Offset(size.width, 0f), Offset(size.width - markLen, 0f), stroke)
            drawLine(accentColor, Offset(size.width, 0f), Offset(size.width, markLen), stroke)
            // Bottom-left
            drawLine(accentColor, Offset(0f, size.height), Offset(markLen, size.height), stroke)
            drawLine(accentColor, Offset(0f, size.height), Offset(0f, size.height - markLen), stroke)
            // Bottom-right
            drawLine(accentColor, Offset(size.width, size.height), Offset(size.width - markLen, size.height), stroke)
            drawLine(accentColor, Offset(size.width, size.height), Offset(size.width, size.height - markLen), stroke)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID//${card.category.uppercase().take(6)}",
                    color = accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SYS.ONLINE",
                    color = Color(0xFF34D399),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text(
                    text = card.fullName.ifBlank { "CYBER_USER" }.uppercase(),
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "> ${card.jobTitle.ifBlank { card.company }}",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    if (card.phone.isNotBlank()) Text("TEL: ${card.phone}", color = textColor.copy(alpha = 0.8f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    if (card.email.isNotBlank()) Text("NET: ${card.email}", color = textColor.copy(alpha = 0.8f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                if (card.showQrBadge) {
                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = "QR",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp).clickable { onFlipToQr() }
                    )
                }
            }
        }
    }
}

// 5. SPLIT DUOTONE LAYOUT (Left Side Color Block + Right Body)
@Composable
private fun SplitDuotoneLayout(
    card: BusinessCard,
    accentColor: Color,
    textColor: Color,
    fontFamily: FontFamily,
    showSyncBadges: Boolean,
    onFlipToQr: () -> Unit,
    onPhotoClick: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column Block
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.35f)
                .background(accentColor.copy(alpha = 0.25f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (card.showAvatar) {
                    if (!card.cardFrontImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = card.cardFrontImageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { onPhotoClick?.invoke() ?: onFlipToQr() }
                        )
                    } else {
                        val initial = if (card.fullName.isNotBlank()) card.fullName.trim().first().uppercase() else "C"
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .clickable { onPhotoClick?.invoke() ?: onFlipToQr() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initial, color = Color(0xFF042F2E), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = card.category.uppercase().take(8),
                    color = accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                )
            }
        }

        // Right Column Body
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.65f)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = card.fullName.ifBlank { "Full Name" },
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    maxLines = 1
                )
                if (card.jobTitle.isNotBlank()) {
                    Text(
                        text = card.jobTitle,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = fontFamily,
                        maxLines = 1
                    )
                }
                if (card.company.isNotBlank()) {
                    Text(
                        text = card.company,
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = fontFamily,
                        maxLines = 1
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (card.phone.isNotBlank()) Text(card.phone, color = textColor.copy(alpha = 0.85f), fontSize = 9.5.sp, fontFamily = fontFamily)
                if (card.email.isNotBlank()) Text(card.email, color = textColor.copy(alpha = 0.85f), fontSize = 9.5.sp, fontFamily = fontFamily)
            }
        }
    }
}

// 6. VERTICAL SHOWCASE LAYOUT (Centered Banner, Prominent Brand)
@Composable
private fun VerticalShowcaseLayout(
    card: BusinessCard,
    accentColor: Color,
    textColor: Color,
    fontFamily: FontFamily,
    showSyncBadges: Boolean,
    onFlipToQr: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = card.category.uppercase(),
                    color = accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if (card.showQrBadge) {
                Icon(Icons.Default.QrCode2, contentDescription = "QR", tint = accentColor, modifier = Modifier.size(16.dp).clickable { onFlipToQr() })
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = card.fullName.ifBlank { "Full Name" },
                color = textColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (card.jobTitle.isNotBlank() && card.company.isNotBlank()) "${card.jobTitle} • ${card.company}"
                else card.jobTitle.ifBlank { card.company },
                color = accentColor,
                fontSize = 11.sp,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (card.phone.isNotBlank()) Text(card.phone, color = textColor.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = fontFamily)
            if (card.email.isNotBlank()) Text(card.email, color = textColor.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = fontFamily)
        }
    }
}

@Composable
private fun CardBackContent(
    card: BusinessCard,
    accentColor: Color,
    textColor: Color,
    fontFamily: FontFamily
) {
    val vCardString = remember(card) { VCardExporter.toVCard3String(card) }
    val qrBitmap = remember(vCardString) {
        QrCodeHelper.generateQrBitmap(
            content = vCardString,
            sizePx = 256,
            darkColor = android.graphics.Color.BLACK,
            lightColor = android.graphics.Color.WHITE
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (card.website.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(card.website, color = textColor, fontSize = 11.sp, fontFamily = fontFamily, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (card.address.isNotBlank()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp).padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(card.address, color = textColor.copy(alpha = 0.85f), fontSize = 10.sp, fontFamily = fontFamily, maxLines = 2)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text("Standard: ${card.cardStandardName}", color = textColor.copy(alpha = 0.6f), fontSize = 9.sp, fontFamily = fontFamily)
                Text("Tap to flip back", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, fontFamily = fontFamily)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(0.8f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    modifier = Modifier.size(76.dp).padding(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR",
                                modifier = Modifier.fillMaxSize().padding(4.dp)
                            )
                        } else {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.Black, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text("Scan to Save", color = textColor.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
            }
        }
    }
}

private fun cleanBusinessCard(card: BusinessCard): BusinessCard {
    fun clean(text: String?): String {
        if (text == null) return ""
        val trimmed = text.trim()
        return if (trimmed.equals("null", ignoreCase = true) ||
            trimmed.equals("n/a", ignoreCase = true) ||
            trimmed.equals("none", ignoreCase = true) ||
            trimmed.equals("nil", ignoreCase = true) ||
            trimmed.equals("undefined", ignoreCase = true)
        ) "" else trimmed
    }

    return card.copy(
        fullName = clean(card.fullName),
        jobTitle = clean(card.jobTitle),
        company = clean(card.company),
        phone = clean(card.phone),
        secondaryPhone = clean(card.secondaryPhone),
        email = clean(card.email),
        website = clean(card.website),
        address = clean(card.address),
        notes = clean(card.notes),
        socialLinks = clean(card.socialLinks),
        rawOcrText = clean(card.rawOcrText)
    )
}

