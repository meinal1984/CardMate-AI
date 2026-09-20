package com.example.ui.components

import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.image.CardDetectionState
import com.example.image.CardQuadCorners
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess

@Composable
fun ScannerReticleView(
    detectedWidthMm: Float,
    detectedHeightMm: Float,
    standardSizeName: String,
    detectionState: CardDetectionState,
    quadCorners: CardQuadCorners,
    isAutoCaptureEnabled: Boolean,
    stableProgress: Float, // 0.0f to 1.0f
    hasGlareWarning: Boolean = false,
    isBlurWarning: Boolean = false,
    isScanningActive: Boolean = true,
    isBangla: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laserScan")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Animated Corner Coordinate Transitions for silky-smooth tracking
    val animTLX by animateFloatAsState(targetValue = quadCorners.topLeft.x, animationSpec = tween(120), label = "tlx")
    val animTLY by animateFloatAsState(targetValue = quadCorners.topLeft.y, animationSpec = tween(120), label = "tly")
    val animTRX by animateFloatAsState(targetValue = quadCorners.topRight.x, animationSpec = tween(120), label = "trx")
    val animTRY by animateFloatAsState(targetValue = quadCorners.topRight.y, animationSpec = tween(120), label = "try")
    val animBRX by animateFloatAsState(targetValue = quadCorners.bottomRight.x, animationSpec = tween(120), label = "brx")
    val animBRY by animateFloatAsState(targetValue = quadCorners.bottomRight.y, animationSpec = tween(120), label = "bry")
    val animBLX by animateFloatAsState(targetValue = quadCorners.bottomLeft.x, animationSpec = tween(120), label = "blx")
    val animBLY by animateFloatAsState(targetValue = quadCorners.bottomLeft.y, animationSpec = tween(120), label = "bly")

    // Dynamic State Colors
    val stateColor by animateColorAsState(
        targetValue = when (detectionState) {
            CardDetectionState.SEARCHING -> Color(0xFF38BDF8) // Soft Cyan
            CardDetectionState.CARD_DETECTED -> CardMateGoldAccent // Gold
            CardDetectionState.CARD_STABLE -> Color(0xFF10B981) // Emerald Green
            CardDetectionState.CAPTURING -> Color(0xFF2DD4BF) // Teal
            CardDetectionState.PROCESSING -> CardMateCyanAccent
            CardDetectionState.READY_FOR_REVIEW -> Color(0xFF10B981)
            CardDetectionState.FAILED -> Color(0xFFF43F5E)
        },
        animationSpec = tween(250),
        label = "stateColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Business card 1.75 aspect ratio framing container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.75f)
        ) {
            // Live Dynamic Quadrilateral & Reticle Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val tl = Offset(animTLX * w, animTLY * h)
                val tr = Offset(animTRX * w, animTRY * h)
                val br = Offset(animBRX * w, animBRY * h)
                val bl = Offset(animBLX * w, animBLY * h)

                // 1. Draw Semi-transparent shaded polygon inside detected card boundaries
                if (detectionState != CardDetectionState.SEARCHING) {
                    val cardPath = Path().apply {
                        moveTo(tl.x, tl.y)
                        lineTo(tr.x, tr.y)
                        lineTo(br.x, br.y)
                        lineTo(bl.x, bl.y)
                        close()
                    }

                    drawPath(
                        path = cardPath,
                        color = stateColor.copy(alpha = if (detectionState == CardDetectionState.CARD_STABLE) 0.18f else 0.08f)
                    )

                    // Draw Quadrilateral boundary lines
                    drawPath(
                        path = cardPath,
                        color = stateColor,
                        style = Stroke(
                            width = if (detectionState == CardDetectionState.CARD_STABLE) 3.5.dp.toPx() else 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // 2. Outer Static Viewfinder Reticles (Corner Brackets)
                val cornerLength = 28.dp.toPx()
                val bracketStroke = 3.dp.toPx()
                val bracketColor = if (detectionState == CardDetectionState.SEARCHING) Color(0xFF2DD4BF) else stateColor.copy(alpha = 0.6f)

                // Outer TL
                drawLine(bracketColor, Offset(0f, cornerLength), Offset(0f, 0f), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(0f, 0f), Offset(cornerLength, 0f), bracketStroke, StrokeCap.Round)

                // Outer TR
                drawLine(bracketColor, Offset(w - cornerLength, 0f), Offset(w, 0f), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(w, 0f), Offset(w, cornerLength), bracketStroke, StrokeCap.Round)

                // Outer BL
                drawLine(bracketColor, Offset(0f, h - cornerLength), Offset(0f, h), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(0f, h), Offset(cornerLength, h), bracketStroke, StrokeCap.Round)

                // Outer BR
                drawLine(bracketColor, Offset(w - cornerLength, h), Offset(w, h), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(w, h - cornerLength), Offset(w, h), bracketStroke, StrokeCap.Round)

                // 3. Dynamic Corner Pin Markers for Detected Card
                if (detectionState != CardDetectionState.SEARCHING) {
                    val pinRadius = 7.dp.toPx() * (if (detectionState == CardDetectionState.CARD_STABLE) pulseScale else 1f)
                    val corners = listOf(tl, tr, br, bl)

                    corners.forEach { pt ->
                        // Glow circle
                        drawCircle(
                            color = stateColor.copy(alpha = 0.35f),
                            radius = pinRadius * 1.8f,
                            center = pt
                        )
                        // Solid inner ring
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = pinRadius,
                            center = pt
                        )
                        drawCircle(
                            color = stateColor,
                            radius = pinRadius,
                            center = pt,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                }

                // 4. Laser Scanning Sweep Line
                if (isScanningActive && (detectionState == CardDetectionState.SEARCHING || detectionState == CardDetectionState.CARD_DETECTED)) {
                    val laserY = h * laserPosition
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                stateColor.copy(alpha = 0.9f),
                                Color.White,
                                stateColor.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(10.dp.toPx(), laserY),
                        end = Offset(w - 10.dp.toPx(), laserY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = stateColor.copy(alpha = 0.25f),
                        radius = 20.dp.toPx(),
                        center = Offset(w / 2f, laserY)
                    )
                }
            }

            // Top Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEE0B132B),
                border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.8f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (detectionState) {
                        CardDetectionState.SEARCHING -> {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "কার্ডটি ফ্রেমের ভেতরে রাখুন" else "Place card inside frame",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        CardDetectionState.CARD_DETECTED -> {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CardMateGoldAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "কার্ড সনাক্ত হয়েছে" else "Card detected",
                                color = CardMateGoldAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CardDetectionState.CARD_STABLE -> {
                            if (isAutoCaptureEnabled) {
                                CircularProgressIndicator(
                                    progress = { stableProgress.coerceIn(0.1f, 1f) },
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFF10B981),
                                    strokeWidth = 2.dp,
                                    trackColor = Color(0xFF10B981).copy(alpha = 0.25f)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) {
                                    if (isAutoCaptureEnabled) "স্থির রাখুন... অটো ক্যাপচার হচ্ছে" else "স্থির রাখুন ✓ ক্যাপচার করুন"
                                } else {
                                    if (isAutoCaptureEnabled) "Hold steady... auto-capturing" else "Hold steady ✓ Ready to capture"
                                },
                                color = Color(0xFF10B981),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CardDetectionState.CAPTURING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = CardMateTealPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "ছবি তোলা হচ্ছে..." else "Capturing card...",
                                color = CardMateTealPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CardDetectionState.PROCESSING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = CardMateCyanAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "কার্ড প্রস্তুত করা হচ্ছে..." else "Preparing your card...",
                                color = CardMateCyanAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CardDetectionState.READY_FOR_REVIEW -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "প্রস্তুত ✓" else "Ready ✓",
                                color = Color(0xFF10B981),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CardDetectionState.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "কার্ড সনাক্ত ব্যর্থ" else "Detection failed",
                                color = Color(0xFFF43F5E),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Real-time Dimension & Standard Size Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "Card Dimensions",
                        tint = stateColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${String.format("%.1f", detectedWidthMm)} × ${String.format("%.1f", detectedHeightMm)} mm",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• $standardSizeName",
                        color = CardMateCyanAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Live Environmental Quality Warning Chips (Blur & Glare)
        if (hasGlareWarning || isBlurWarning) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xEE7C2D12),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFB923C)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFDBA74),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (hasGlareWarning) {
                            if (isBangla) "⚠️ তীব্র আলো/গ্লেয়ার সনাক্ত হয়েছে" else "⚠️ Strong glare detected"
                        } else {
                            if (isBangla) "⚠️ কিছুটা ঝাপসা হতে পারে, ক্যামেরা স্থির রাখুন" else "⚠️ Motion blur detected, hold steady"
                        },
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
