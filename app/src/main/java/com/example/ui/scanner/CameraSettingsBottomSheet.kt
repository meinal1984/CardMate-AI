package com.example.ui.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceCardElevated
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.ScanMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSettingsBottomSheet(
    isBangla: Boolean,
    isAdaptiveEnhancementEnabled: Boolean,
    onAdaptiveEnhancementToggled: (Boolean) -> Unit,
    isAutoCaptureEnabled: Boolean,
    onAutoCaptureToggled: (Boolean) -> Unit,
    scanMode: ScanMode = ScanMode.DUAL_SIDE,
    onScanModeChanged: (ScanMode) -> Unit = {},
    lastAppliedAdaptiveEnhancement: String? = null,
    selectedAspectRatio: Float = 1.75f,
    onAspectRatioSelected: (Float, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CardMateTealPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Camera Settings",
                            tint = CardMateTealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isBangla) "ক্যামেরা ও স্ক্যানার সেটিংস" else "Camera & Scanner Settings",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isBangla) "স্ক্যান মোড ও অপ্টিমাইজেশন" else "Scan Mode & Recognition Pipeline",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // FEATURE 0: SCAN MODE SELECTOR (Dual vs Single)
            // ==========================================
            Text(
                text = if (isBangla) "স্ক্যানিং মোড (Scan Mode):" else "Scanning Mode:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDual = scanMode == ScanMode.DUAL_SIDE

                    // Two-Sided Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDual) CardMateTealPrimary.copy(alpha = 0.2f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDual) CardMateTealPrimary else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onScanModeChanged(ScanMode.DUAL_SIDE) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (isDual) CardMateTealPrimary else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "উভয় পাশ" else "Two-Sided",
                                    fontSize = 13.sp,
                                    fontWeight = if (isDual) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDual) CardMateTealPrimary else Color.White
                                )
                                Text(
                                    text = if (isBangla) "সামনে + পেছনে" else "Front + Back",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Single-Sided Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isDual) CardMateTealPrimary.copy(alpha = 0.2f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isDual) CardMateTealPrimary else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onScanModeChanged(ScanMode.SINGLE_SIDE) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewAgenda,
                                contentDescription = null,
                                tint = if (!isDual) CardMateTealPrimary else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "এক পাশ" else "Single Side",
                                    fontSize = 13.sp,
                                    fontWeight = if (!isDual) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isDual) CardMateTealPrimary else Color.White
                                )
                                Text(
                                    text = if (isBangla) "শুধু সামনের পাশ" else "Front side only",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // FEATURE 1: ADAPTIVE ENHANCEMENTS TOGGLE
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isAdaptiveEnhancementEnabled) Color(0xFF132338) else DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAdaptiveEnhancementEnabled) CardMateTealPrimary.copy(alpha = 0.5f) else DarkSurfaceBorder
                ),
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isAdaptiveEnhancementEnabled)
                                            CardMateTealPrimary.copy(alpha = 0.25f)
                                        else
                                            Color(0xFF1E293B)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = "Adaptive Enhancements",
                                    tint = if (isAdaptiveEnhancementEnabled) CardMateTealPrimary else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "অ্যাডাপ্টিভ এনহ্যান্সমেন্ট (Adaptive Enhancements)" else "Adaptive Enhancements",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isBangla) "ডায়নামিক কনট্রাস্ট ও ব্রাইটনেস অটো-টিউনিং" else "Dynamic Contrast & Brightness for Pre-OCR",
                                    fontSize = 12.sp,
                                    color = if (isAdaptiveEnhancementEnabled) CardMateCyanAccent else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Switch(
                            checked = isAdaptiveEnhancementEnabled,
                            onCheckedChange = { onAdaptiveEnhancementToggled(it) },
                            modifier = Modifier.testTag("camera_settings_adaptive_enhancements_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0F172A),
                                checkedTrackColor = CardMateTealPrimary,
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-time Adaptive Pipeline Status Notification Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAdaptiveEnhancementEnabled) CardMateTealPrimary.copy(alpha = 0.14f) else Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAdaptiveEnhancementEnabled) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isAdaptiveEnhancementEnabled) CardMateTealPrimary else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAdaptiveEnhancementEnabled) {
                                    if (isBangla)
                                        "⚡ সক্রিয়: কার্ডের আলো ও কনট্রাস্ট স্বয়ংক্রিয়ভাবে পরিমাপ করে OCR-এর জন্য অপ্টিমাইজ করা হচ্ছে।"
                                    else
                                        "⚡ Active: Dynamically measuring card luma & contrast to auto-correct lighting before Gemini OCR."
                                } else {
                                    if (isBangla)
                                        "⏸️ নিষ্ক্রিয়: র' ক্যামেরা এক্সপোজার সরাসরি AI মডেলে পাঠানো হবে।"
                                    else
                                        "⏸️ Disabled: Raw captured exposure will be fed to AI without dynamic adjustments."
                                },
                                fontSize = 11.5.sp,
                                color = if (isAdaptiveEnhancementEnabled) Color(0xFFCCFBF1) else Color(0xFF94A3B8),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isAdaptiveEnhancementEnabled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = if (isBangla) "পাইপলাইনের কার্যপ্রণালী:" else "What Adaptive Enhancements do:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            PipelineFeatureItem(
                                icon = Icons.Default.Contrast,
                                title = if (isBangla) "ডায়নামিক কনট্রাস্ট বুস্ট" else "Dynamic Contrast Stretch",
                                desc = if (isBangla) "ফ্যাকাশে বা রঙিন ব্যাকগ্রাউন্ডের কার্ডে টেক্সটের ভিজিবিলিটি বাড়ায়" else "Amplifies faint glyphs against colored paper backgrounds"
                            )
                            PipelineFeatureItem(
                                icon = Icons.Default.Lightbulb,
                                title = if (isBangla) "লো-লাইট শ্যাডো কমপেনসেশন" else "Low-Light & Shadow Recovery",
                                desc = if (isBangla) "কম আলো বা ছায়ার ক্ষেত্রে লুমিনেন্স বুস্ট ইনজেক্ট করে" else "Calculates ambient luma deficit to lift underexposed text"
                            )
                            PipelineFeatureItem(
                                icon = Icons.Default.BrightnessAuto,
                                title = if (isBangla) "অ্যান্টি-গ্লেয়ার হাইলাইট টেমিং" else "Anti-Glare Glint Suppression",
                                desc = if (isBangla) "প্লাস্টিক/ল্যামিনেটেড কার্ডের তীব্র ফ্ল্যাশ প্রতিফলন হ্রাস করে" else "Controls washed-out hot spots from flash or glossy lamination"
                            )
                        }
                    }

                    // Last Applied Diagnostics
                    if (!lastAppliedAdaptiveEnhancement.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = if (isBangla) "সর্বশেষ কার্ডে প্রয়োগকৃত অ্যাডজাস্টমেন্ট:" else "Recent Card Calibration:",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CardMateGoldAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lastAppliedAdaptiveEnhancement,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // FEATURE 2: AUTO-CAPTURE TOGGLE
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashAuto,
                                contentDescription = "Auto Capture",
                                tint = if (isAutoCaptureEnabled) CardMateTealPrimary else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isBangla) "অটো-ক্যাপচার (Auto-Capture)" else "Auto-Capture On Steady",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (isBangla) "কার্ড ফ্রেমের সাথে স্থির হলে স্বয়ংক্রিয় ছবি তুলুন" else "Snap automatically once card is aligned & steady",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Switch(
                        checked = isAutoCaptureEnabled,
                        onCheckedChange = { onAutoCaptureToggled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0F172A),
                            checkedTrackColor = CardMateTealPrimary,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // FEATURE 3: ASPECT RATIO / CARD STANDARD SELECTOR
            // ==========================================
            Text(
                text = if (isBangla) "কার্ড সাইজ ও অ্যাসপেক্ট রেশিও স্ট্যান্ডার্ড:" else "Card Standard & Aspect Ratio:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val ratioOptions = listOf(
                Triple("Standard US (3.5\" × 2.0\")", 1.75f, "88.9 × 50.8 mm"),
                Triple("ISO / Credit (85.6 × 54.0 mm)", 1.585f, "CR-80 Standard"),
                Triple("EU Standard (85.0 × 55.0 mm)", 1.545f, "European Format"),
                Triple("Japanese (91.0 × 55.0 mm)", 1.654f, "Meishi Standard")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ratioOptions.take(2).forEach { (label, ratio, subtitle) ->
                    val isSelected = kotlin.math.abs(selectedAspectRatio - ratio) < 0.05f
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CardMateTealPrimary.copy(alpha = 0.18f) else DarkSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CardMateTealPrimary else DarkSurfaceBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAspectRatioSelected(ratio, label) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = label.split(" (")[0],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CardMateTealPrimary else Color.White
                            )
                            Text(
                                text = subtitle,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ratioOptions.drop(2).forEach { (label, ratio, subtitle) ->
                    val isSelected = kotlin.math.abs(selectedAspectRatio - ratio) < 0.05f
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CardMateTealPrimary.copy(alpha = 0.18f) else DarkSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CardMateTealPrimary else DarkSurfaceBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAspectRatioSelected(ratio, label) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = label.split(" (")[0],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CardMateTealPrimary else Color.White
                            )
                            Text(
                                text = subtitle,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineFeatureItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CardMateTealPrimary,
            modifier = Modifier
                .size(15.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0)
            )
            Text(
                text = desc,
                fontSize = 10.5.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 14.sp
            )
        }
    }
}
