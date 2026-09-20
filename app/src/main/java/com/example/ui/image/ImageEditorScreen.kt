package com.example.ui.image

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BusinessCard
import com.example.image.CardImageProcessor
import com.example.image.CardQuadCorners
import com.example.image.CropAspectRatio
import com.example.image.ImageEnhanceParams
import com.example.image.ImageQualityScore
import com.example.ui.navigation.BackNavigationService
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.CardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class ImageEditorTab(val titleEn: String, val titleBn: String) {
    CROP("Crop", "ক্রপ"),
    PERSPECTIVE("Perspective", "পারস্পেক্টিভ"),
    ROTATE("Rotate", "ঘোরান"),
    RESIZE("Resize", "রিসাইজ"),
    ENHANCE("Enhance", "উন্নত করুন"),
    QUALITY_AI("AI Quality", "কোয়ালিটি স্কোর")
}

@Composable
fun ImageEditorScreen(
    viewModel: CardViewModel,
    card: BusinessCard?,
    initialImageUri: String?,
    side: String = "front", // "front" or "back"
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val navService = remember { BackNavigationService.instance }

    // Loading & Bitmaps State
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentWorkingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }
    var isProcessingOperation by remember { mutableStateOf(false) }

    // Active Editor Tool Tab
    var activeTab by remember { mutableStateOf(ImageEditorTab.CROP) }

    // Transform State
    var cropRect by remember { mutableStateOf(RectF(0.04f, 0.04f, 0.96f, 0.96f)) }
    var selectedCropAspect by remember { mutableStateOf<CropAspectRatio>(CropAspectRatio.BUSINESS_CARD_US) }
    var perspectiveQuad by remember { mutableStateOf(CardQuadCorners()) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var fineRotationAngle by remember { mutableFloatStateOf(0f) }
    var isFlippedH by remember { mutableStateOf(false) }
    var enhanceParams by remember { mutableStateOf(ImageEnhanceParams.DEFAULT) }

    // Resize state
    var customWidthInput by remember { mutableStateOf("") }
    var customHeightInput by remember { mutableStateOf("") }
    var isAspectRatioLocked by remember { mutableStateOf(true) }

    // Compare original toggle
    var isComparingOriginal by remember { mutableStateOf(false) }

    // AI Quality Score
    var qualityScore by remember { mutableStateOf<ImageQualityScore?>(null) }

    // Dialogs
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var showOcrConfirmDialog by remember { mutableStateOf(false) }
    var isRunningOcr by remember { mutableStateOf(false) }

    // Has user made changes compared to original state?
    val hasUnsavedChanges = remember(
        cropRect, rotationAngle, fineRotationAngle, isFlippedH, enhanceParams, currentWorkingBitmap, originalBitmap
    ) {
        cropRect.left > 0.05f || cropRect.top > 0.05f || cropRect.right < 0.95f || cropRect.bottom < 0.95f ||
                rotationAngle != 0f || fineRotationAngle != 0f || isFlippedH || !enhanceParams.isDefault ||
                currentWorkingBitmap != originalBitmap
    }

    // Connect with BackNavigationService for system edge-swipe / back button
    DisposableEffect(hasUnsavedChanges, showDiscardConfirmDialog, showSaveConfirmDialog) {
        val unregister = navService.registerModal {
            if (showDiscardConfirmDialog || showSaveConfirmDialog) {
                showDiscardConfirmDialog = false
                showSaveConfirmDialog = false
                true
            } else if (hasUnsavedChanges) {
                showDiscardConfirmDialog = true
                true
            } else {
                false
            }
        }
        onDispose { unregister() }
    }

    // Load Image initially
    LaunchedEffect(initialImageUri) {
        if (!initialImageUri.isNullOrBlank()) {
            isLoadingImage = true
            val bmp = CardImageProcessor.loadBitmapSafely(context, initialImageUri, maxDimension = 2600)
            if (bmp != null) {
                originalBitmap = bmp
                currentWorkingBitmap = bmp
                // Initial Quality analysis and Corner detection in background
                val detected = CardImageProcessor.autoDetectCardCorners(bmp, selectedCropAspect.ratio ?: 1.75f)
                perspectiveQuad = detected
                cropRect = CardImageProcessor.computeCenteredCropRect(bmp.width, bmp.height, selectedCropAspect.ratio ?: 1.75f)
                qualityScore = CardImageProcessor.analyzeCardQuality(bmp)
                customWidthInput = bmp.width.toString()
                customHeightInput = bmp.height.toString()
            } else {
                Toast.makeText(
                    context,
                    if (isBangla) "ছবি লোড করা যায়নি" else "Failed to load image file",
                    Toast.LENGTH_SHORT
                ).show()
            }
            isLoadingImage = false
        }
    }

    // Apply Live Preview Enhancement when parameters change
    val previewBitmap = remember(currentWorkingBitmap, enhanceParams) {
        val base = currentWorkingBitmap
        if (base != null && !enhanceParams.isDefault) {
            CardImageProcessor.enhanceBitmap(base, enhanceParams)
        } else {
            base
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .statusBarsPadding()
            .testTag("image_editor_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                showDiscardConfirmDialog = true
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = if (isBangla) "কার্ড ছবি এডিটর" else "Business Card Editor",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (side == "front") CardMateTealPrimary.copy(alpha = 0.25f) else CardMateGoldAccent.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (side == "front") {
                                        if (isBangla) "সামনের পাশ (Front)" else "Front Side"
                                    } else {
                                        if (isBangla) "পেছনের পাশ (Back)" else "Back Side"
                                    },
                                    color = if (side == "front") CardMateTealPrimary else CardMateGoldAccent,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (hasUnsavedChanges) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "• পরিবর্তন হয়েছে" else "• Modified",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Compare Original Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isComparingOriginal) CardMateGoldAccent else Color(0xFF1E293B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isComparingOriginal = !isComparingOriginal }
                            .testTag("compare_original_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Compare",
                                tint = if (isComparingOriginal) Color(0xFF0F172A) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "আসল" else "Compare",
                                color = if (isComparingOriginal) Color(0xFF0F172A) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Reset All Button
                    IconButton(
                        onClick = {
                            currentWorkingBitmap = originalBitmap
                            cropRect = RectF(0.04f, 0.04f, 0.96f, 0.96f)
                            rotationAngle = 0f
                            fineRotationAngle = 0f
                            isFlippedH = false
                            enhanceParams = ImageEnhanceParams.DEFAULT
                            if (originalBitmap != null) {
                                qualityScore = CardImageProcessor.analyzeCardQuality(originalBitmap!!)
                            }
                            Toast.makeText(
                                context,
                                if (isBangla) "সব পরিবর্তন রিসেট করা হয়েছে" else "Reset to original image",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Reset All",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    // Save Button
                    Button(
                        onClick = { showSaveConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_image_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = Color(0xFF042F2E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "সেভ" else "Save",
                            color = Color(0xFF042F2E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Central Viewport / Workspace Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF050811))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = if (isComparingOriginal) originalBitmap else previewBitmap

                if (isLoadingImage) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CardMateTealPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBangla) "ছবি লোড হচ্ছে..." else "Loading card image...",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                } else if (displayBitmap != null) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val containerW = maxWidth.value
                        val containerH = maxHeight.value
                        val bmpW = displayBitmap.width.toFloat()
                        val bmpH = displayBitmap.height.toFloat()

                        // Calculate exact fitted display dimensions to avoid letterboxing mismatch
                        val scale = minOf(containerW / bmpW, containerH / bmpH)
                        val fittedW = (bmpW * scale).dp
                        val fittedH = (bmpH * scale).dp

                        // Inner Box has exact image aspect ratio so normalized coordinates map 100% accurately to pixels
                        Box(
                            modifier = Modifier
                                .size(fittedW, fittedH)
                                .clipToBounds(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Display Bitmap
                            androidx.compose.foundation.Image(
                                bitmap = displayBitmap.asImageBitmap(),
                                contentDescription = "Card Edit Canvas",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Interactive Overlays based on Active Tab
                            if (!isComparingOriginal) {
                                when (activeTab) {
                                    ImageEditorTab.CROP -> {
                                        InteractiveCropOverlay(
                                            cropRect = cropRect,
                                            onCropRectChanged = { cropRect = it },
                                            aspectRatio = selectedCropAspect.ratio,
                                            imageWidth = displayBitmap.width,
                                            imageHeight = displayBitmap.height,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    ImageEditorTab.PERSPECTIVE -> {
                                        InteractivePerspectiveOverlay(
                                            quad = perspectiveQuad,
                                            onQuadChanged = { perspectiveQuad = it },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> { /* No overlay on other tabs */ }
                                }
                            }
                        }

                        // Top Left Info HUD: Resolution & Quality
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${displayBitmap.width} × ${displayBitmap.height} px",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (displayBitmap.width >= 1050 && displayBitmap.height >= 600) "• 300 DPI High-Res" else "• Standard Res",
                                    color = if (displayBitmap.width >= 1050) CardMateTealPrimary else CardMateGoldAccent,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Comparison Banner indicator
                        if (isComparingOriginal) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CardMateGoldAccent.copy(alpha = 0.95f),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "👁 মূল আনএডিটেড ছবি দেখাচ্ছে" else "👁 Viewing Original Unedited Image",
                                    color = Color(0xFF0F172A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (isBangla) "কোনো ছবি পাওয়া যায়নি" else "No image loaded",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }

                if (isProcessingOperation) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = CardMateTealPrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = if (isBangla) "প্রসেস হচ্ছে..." else "Processing image...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Tool Shelf & Tabs
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Tool Configuration Shelves
                    when (activeTab) {
                        ImageEditorTab.CROP -> {
                            CropControlShelf(
                                isBangla = isBangla,
                                selectedAspect = selectedCropAspect,
                                onAspectSelected = { aspect ->
                                    selectedCropAspect = aspect
                                    val bmp = currentWorkingBitmap
                                    if (bmp != null) {
                                        cropRect = CardImageProcessor.computeCenteredCropRect(bmp.width, bmp.height, aspect.ratio)
                                    }
                                },
                                onAutoDetectCard = {
                                    val bmp = currentWorkingBitmap ?: return@CropControlShelf
                                    isProcessingOperation = true
                                    coroutineScope.launch {
                                        val detected = withContext(Dispatchers.Default) {
                                            CardImageProcessor.autoDetectCardCorners(bmp, selectedCropAspect.ratio ?: 1.75f)
                                        }
                                        cropRect = RectF(
                                            detected.topLeft.x,
                                            detected.topLeft.y,
                                            detected.bottomRight.x,
                                            detected.bottomRight.y
                                        )
                                        isProcessingOperation = false
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "কার্ডের চারপাশ স্বয়ংক্রিয়ভাবে চিহ্নিত হয়েছে" else "Card boundaries auto-detected",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onApplyCrop = {
                                    val bmp = currentWorkingBitmap ?: return@CropControlShelf
                                    isProcessingOperation = true
                                    coroutineScope.launch {
                                        val cropped = withContext(Dispatchers.Default) {
                                            CardImageProcessor.cropBitmap(bmp, cropRect)
                                        }
                                        currentWorkingBitmap = cropped
                                        cropRect = CardImageProcessor.computeCenteredCropRect(cropped.width, cropped.height, selectedCropAspect.ratio)
                                        qualityScore = CardImageProcessor.analyzeCardQuality(cropped)
                                        customWidthInput = cropped.width.toString()
                                        customHeightInput = cropped.height.toString()
                                        isProcessingOperation = false
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "ক্রপ সফলভাবে প্রয়োগ হয়েছে" else "Crop applied successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onResetCrop = {
                                    val bmp = currentWorkingBitmap
                                    if (bmp != null) {
                                        cropRect = CardImageProcessor.computeCenteredCropRect(bmp.width, bmp.height, selectedCropAspect.ratio)
                                    } else {
                                        cropRect = RectF(0.04f, 0.04f, 0.96f, 0.96f)
                                    }
                                }
                            )
                        }

                        ImageEditorTab.PERSPECTIVE -> {
                            PerspectiveControlShelf(
                                isBangla = isBangla,
                                onAutoDetectCorners = {
                                    val bmp = currentWorkingBitmap ?: return@PerspectiveControlShelf
                                    isProcessingOperation = true
                                    coroutineScope.launch {
                                        val detected = withContext(Dispatchers.Default) {
                                            CardImageProcessor.autoDetectCardCorners(bmp, selectedCropAspect.ratio ?: 1.75f)
                                        }
                                        perspectiveQuad = detected
                                        isProcessingOperation = false
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "৪ কোণা স্বয়ংক্রিয়ভাবে শনাক্ত হয়েছে" else "4 Corners auto-detected",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onApplyWarp = {
                                    val bmp = currentWorkingBitmap ?: return@PerspectiveControlShelf
                                    isProcessingOperation = true
                                    coroutineScope.launch {
                                        val warped = withContext(Dispatchers.Default) {
                                            CardImageProcessor.applyPerspectiveCorrection(
                                                bmp,
                                                perspectiveQuad,
                                                targetAspectRatio = selectedCropAspect.ratio ?: 1.75f
                                            )
                                        }
                                        currentWorkingBitmap = warped
                                        perspectiveQuad = CardQuadCorners()
                                        qualityScore = CardImageProcessor.analyzeCardQuality(warped)
                                        customWidthInput = warped.width.toString()
                                        customHeightInput = warped.height.toString()
                                        isProcessingOperation = false
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "কার্ড সোজা ও সমতল করা হয়েছে" else "Card straightened & flattened",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onResetQuad = {
                                    perspectiveQuad = CardQuadCorners()
                                }
                            )
                        }

                        ImageEditorTab.ROTATE -> {
                            RotateControlShelf(
                                isBangla = isBangla,
                                fineAngle = fineRotationAngle,
                                onFineAngleChanged = { angle ->
                                    fineRotationAngle = angle
                                },
                                onRotate90Left = {
                                    val bmp = currentWorkingBitmap ?: return@RotateControlShelf
                                    isProcessingOperation = true
                                    coroutineScope.launch {
                                        val rotated = withContext(Dispatchers.Default) {
                                            CardImageProcessor.rotateBitmap(bmp, -90f)
                                        }
                                        currentWorkingBitmap = rotated
                                        customWidthInput = rotated.width.toString()
                                        customHeightInput = rotated.height.toString()
                                        isProcessingOperation = false
                                    }
                                },
                                onRotate90Right = {
                                    val bmp = currentWorkingBitmap ?: return@RotateControlShelf
                                    isProcessingOperation = true
                                    coroutineScope.launch {
                                        val rotated = withContext(Dispatchers.Default) {
                                            CardImageProcessor.rotateBitmap(bmp, 90f)
                                        }
                                        currentWorkingBitmap = rotated
                                        customWidthInput = rotated.width.toString()
                                        customHeightInput = rotated.height.toString()
                                        isProcessingOperation = false
                                    }
                                },
                                onFlipHorizontal = {
                                    val bmp = currentWorkingBitmap ?: return@RotateControlShelf
                                    isProcessingOperation = true
                                    coroutineScope.launch {
                                        val flipped = withContext(Dispatchers.Default) {
                                            CardImageProcessor.rotateBitmap(bmp, 0f, flipHorizontal = true)
                                        }
                                        currentWorkingBitmap = flipped
                                        isProcessingOperation = false
                                    }
                                },
                                onApplyFineRotation = {
                                    if (fineRotationAngle != 0f) {
                                        val bmp = currentWorkingBitmap ?: return@RotateControlShelf
                                        isProcessingOperation = true
                                        coroutineScope.launch {
                                            val rotated = withContext(Dispatchers.Default) {
                                                CardImageProcessor.rotateBitmap(bmp, fineRotationAngle)
                                            }
                                            currentWorkingBitmap = rotated
                                            fineRotationAngle = 0f
                                            isProcessingOperation = false
                                        }
                                    }
                                }
                            )
                        }

                        ImageEditorTab.RESIZE -> {
                            ResizeControlShelf(
                                isBangla = isBangla,
                                currentWidth = currentWorkingBitmap?.width ?: 0,
                                currentHeight = currentWorkingBitmap?.height ?: 0,
                                customWidth = customWidthInput,
                                customHeight = customHeightInput,
                                isLocked = isAspectRatioLocked,
                                onWidthChange = { wStr ->
                                    customWidthInput = wStr
                                    if (isAspectRatioLocked) {
                                        val wVal = wStr.toIntOrNull()
                                        val curW = currentWorkingBitmap?.width ?: 1
                                        val curH = currentWorkingBitmap?.height ?: 1
                                        if (wVal != null && curW > 0) {
                                            customHeightInput = ((wVal.toFloat() / curW) * curH).roundToInt().toString()
                                        }
                                    }
                                },
                                onHeightChange = { hStr ->
                                    customHeightInput = hStr
                                    if (isAspectRatioLocked) {
                                        val hVal = hStr.toIntOrNull()
                                        val curW = currentWorkingBitmap?.width ?: 1
                                        val curH = currentWorkingBitmap?.height ?: 1
                                        if (hVal != null && curH > 0) {
                                            customWidthInput = ((hVal.toFloat() / curH) * curW).roundToInt().toString()
                                        }
                                    }
                                },
                                onToggleLock = { isAspectRatioLocked = !isAspectRatioLocked },
                                onApplyPreset = { targetMaxDim ->
                                    val bmp = currentWorkingBitmap ?: return@ResizeControlShelf
                                    val maxDim = maxOf(bmp.width, bmp.height)
                                    if (maxDim > targetMaxDim) {
                                        val scale = targetMaxDim.toFloat() / maxDim
                                        val newW = (bmp.width * scale).roundToInt()
                                        val newH = (bmp.height * scale).roundToInt()
                                        isProcessingOperation = true
                                        coroutineScope.launch {
                                            val resized = withContext(Dispatchers.Default) {
                                                CardImageProcessor.resizeBitmap(bmp, newW, newH)
                                            }
                                            currentWorkingBitmap = resized
                                            customWidthInput = newW.toString()
                                            customHeightInput = newH.toString()
                                            qualityScore = CardImageProcessor.analyzeCardQuality(resized)
                                            isProcessingOperation = false
                                            Toast.makeText(
                                                context,
                                                if (isBangla) "রিসাইজ সম্পন্ন ($newW × $newH)" else "Resized to $newW × $newH px",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onApplyCustom = {
                                    val wVal = customWidthInput.toIntOrNull() ?: 0
                                    val hVal = customHeightInput.toIntOrNull() ?: 0
                                    val bmp = currentWorkingBitmap ?: return@ResizeControlShelf
                                    if (wVal in 100..4000 && hVal in 100..4000) {
                                        isProcessingOperation = true
                                        coroutineScope.launch {
                                            val resized = withContext(Dispatchers.Default) {
                                                CardImageProcessor.resizeBitmap(bmp, wVal, hVal)
                                            }
                                            currentWorkingBitmap = resized
                                            qualityScore = CardImageProcessor.analyzeCardQuality(resized)
                                            isProcessingOperation = false
                                            Toast.makeText(
                                                context,
                                                if (isBangla) "কাস্টম সাইজ প্রয়োগ করা হয়েছে" else "Custom dimensions applied",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "অনুগ্রহ করে ১০০ থেকে ৪০০০ পিক্সেল দিন" else "Please enter width & height between 100 and 4000 px",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }

                        ImageEditorTab.ENHANCE -> {
                            EnhanceControlShelf(
                                isBangla = isBangla,
                                params = enhanceParams,
                                onParamsChange = { enhanceParams = it },
                                onApplyPermanent = {
                                    val bmp = currentWorkingBitmap ?: return@EnhanceControlShelf
                                    isProcessingOperation = true
                                    coroutineScope.launch {
                                        val enhanced = withContext(Dispatchers.Default) {
                                            CardImageProcessor.enhanceBitmap(bmp, enhanceParams)
                                        }
                                        currentWorkingBitmap = enhanced
                                        enhanceParams = ImageEnhanceParams.DEFAULT
                                        qualityScore = CardImageProcessor.analyzeCardQuality(enhanced)
                                        isProcessingOperation = false
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "ফিল্টার ও টেক্সট শার্পনিং প্রয়োগ হয়েছে" else "Enhancements & sharpening applied",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }

                        ImageEditorTab.QUALITY_AI -> {
                            QualityAiScorecardShelf(
                                isBangla = isBangla,
                                qualityScore = qualityScore,
                                onReanalyze = {
                                    val bmp = previewBitmap ?: return@QualityAiScorecardShelf
                                    qualityScore = CardImageProcessor.analyzeCardQuality(bmp)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Main Tool Tabs Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImageEditorTab.entries.forEach { tab ->
                            val isSelected = activeTab == tab
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CardMateTealPrimary else Color(0xFF1E293B),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeTab = tab }
                                    .testTag("editor_tab_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (tab) {
                                            ImageEditorTab.CROP -> Icons.Default.Crop
                                            ImageEditorTab.PERSPECTIVE -> Icons.Default.Transform
                                            ImageEditorTab.ROTATE -> Icons.Default.RotateRight
                                            ImageEditorTab.RESIZE -> Icons.Default.PhotoSizeSelectLarge
                                            ImageEditorTab.ENHANCE -> Icons.Default.AutoFixHigh
                                            ImageEditorTab.QUALITY_AI -> Icons.Default.AutoAwesome
                                        },
                                        contentDescription = tab.titleEn,
                                        tint = if (isSelected) Color(0xFF042F2E) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBangla) tab.titleBn else tab.titleEn,
                                        color = if (isSelected) Color(0xFF042F2E) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Discard Confirmation Dialog
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = { Text(if (isBangla) "পরিবর্তন বাতিল করতে চান?" else "Discard Changes?") },
            text = {
                Text(
                    if (isBangla) "আপনার কার্ড ছবির সমস্ত অপ্রয়োগকৃত পরিবর্তন বাতিল হয়ে যাবে।"
                    else "You have unsaved adjustments on this card photo. Do you want to discard them?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(if (isBangla) "বাতিল করুন" else "Discard", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text(if (isBangla) "এডিটিং জারি রাখুন" else "Keep Editing")
                }
            }
        )
    }

    // Save Confirmation Dialog with Re-run OCR prompt option
    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            title = { Text(if (isBangla) "এডিটেড ছবি সেভ করবেন?" else "Save Edited Card Image?") },
            text = {
                Column {
                    Text(
                        if (isBangla) "কার্ডের নতুন এডিটেড ছবিটি উচ্চ মানে সংরক্ষণ করা হবে এবং ক্লাউড ব্যাকআপে আপডেট হবে।"
                        else "The edited card photo will be saved with high fidelity and marked for cloud backup."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CardMateGoldAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "সেভ করার পর আপনি চাইলে AI দিয়ে কার্ডের টেক্সট পুনরায় স্ক্যান (Re-run OCR) করতে পারবেন।"
                                else "After saving, you can re-run Gemini AI OCR on this optimized image.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalBmp = previewBitmap ?: return@Button
                        showSaveConfirmDialog = false
                        isProcessingOperation = true
                        coroutineScope.launch {
                            val savedPath = withContext(Dispatchers.IO) {
                                // Save original if not already present
                                if (originalBitmap != null && card != null) {
                                    CardImageProcessor.saveOriginalBitmap(context, originalBitmap!!, card.id, side)
                                }
                                CardImageProcessor.saveEditedBitmap(context, finalBmp, card?.id ?: System.currentTimeMillis(), side)
                            }

                            // Update in CardViewModel & Database
                            if (card != null) {
                                val updatedCard = if (side == "front") {
                                    card.copy(
                                        cardFrontImageUri = savedPath,
                                        isBackedUpToCloud = false, // Invalidate to trigger new backup
                                        updatedAt = System.currentTimeMillis()
                                    )
                                } else {
                                    card.copy(
                                        cardBackImageUri = savedPath,
                                        isBackedUpToCloud = false,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                }
                                viewModel.saveCard(updatedCard)
                            }

                            isProcessingOperation = false
                            Toast.makeText(
                                context,
                                if (isBangla) "কার্ডের ছবি সফলভাবে সেভ হয়েছে" else "Edited business card photo saved",
                                Toast.LENGTH_SHORT
                            ).show()

                            onSaved(savedPath)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary)
                ) {
                    Text(if (isBangla) "সেভ করুন" else "Save Image", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirmDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Sub-Components / Control Shelves for each Tool Tab
// -------------------------------------------------------------

@Composable
private fun CropControlShelf(
    isBangla: Boolean,
    selectedAspect: CropAspectRatio,
    onAspectSelected: (CropAspectRatio) -> Unit,
    onAutoDetectCard: () -> Unit,
    onApplyCrop: () -> Unit,
    onResetCrop: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBangla) "ক্রপ ও কার্ড ডিটেকশন:" else "Crop & Boundary Detection:",
                color = Color(0xFFCBD5E1),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Auto Detect Card Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardMateGoldAccent.copy(alpha = 0.2f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAutoDetectCard() }
                        .testTag("auto_detect_card_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CardMateGoldAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "অটো ডিটেক্ট" else "Auto Detect",
                            color = CardMateGoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Apply Crop Button
                Button(
                    onClick = onApplyCrop,
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isBangla) "প্রয়োগ" else "Apply Crop", color = Color(0xFF042F2E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Aspect Ratio Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CropAspectRatio.entries.forEach { aspect ->
                val isSelected = selectedAspect == aspect
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) CardMateTealPrimary.copy(alpha = 0.3f) else Color(0xFF1E293B),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CardMateTealPrimary) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAspectSelected(aspect) }
                ) {
                    Text(
                        text = if (isBangla) aspect.titleBn else aspect.titleEn,
                        color = if (isSelected) CardMateTealPrimary else Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PerspectiveControlShelf(
    isBangla: Boolean,
    onAutoDetectCorners: () -> Unit,
    onApplyWarp: () -> Unit,
    onResetQuad: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isBangla) "৪-কোণা পারস্পেক্টিভ সংশোধন:" else "4-Point Perspective Warp:",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBangla) "কোণাগুলো টেনে বাঁকা ছবি সোজা করুন" else "Drag 4 target knobs to flatten angled card",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardMateGoldAccent.copy(alpha = 0.2f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAutoDetectCorners() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CardMateGoldAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isBangla) "অটো কোণা" else "Auto Corners", color = CardMateGoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onApplyWarp,
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isBangla) "সোজা করুন" else "Flatten & Warp", color = Color(0xFF042F2E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RotateControlShelf(
    isBangla: Boolean,
    fineAngle: Float,
    onFineAngleChanged: (Float) -> Unit,
    onRotate90Left: () -> Unit,
    onRotate90Right: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onApplyFineRotation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBangla) "ঘূর্ণন ও ফ্লিপ:" else "Rotate & Flip:",
                color = Color(0xFFCBD5E1),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Rotate Left 90
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRotate90Left() }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = "-90°", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-90°", color = Color.White, fontSize = 11.sp)
                    }
                }

                // Rotate Right 90
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRotate90Right() }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Rotate90DegreesCw, contentDescription = "+90°", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+90°", color = Color.White, fontSize = 11.sp)
                    }
                }

                // Flip H
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onFlipHorizontal() }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Flip, contentDescription = "Flip", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flip", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        // Fine Angle Slider (-45° to +45°)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${String.format("%.1f", fineAngle)}°",
                color = CardMateTealPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(42.dp)
            )

            Slider(
                value = fineAngle,
                onValueChange = onFineAngleChanged,
                valueRange = -45f..45f,
                colors = SliderDefaults.colors(
                    thumbColor = CardMateTealPrimary,
                    activeTrackColor = CardMateTealPrimary,
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier.weight(1f)
            )

            if (fineAngle != 0f) {
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(onClick = onApplyFineRotation, modifier = Modifier.height(28.dp)) {
                    Text(if (isBangla) "প্রয়োগ" else "Apply", color = CardMateTealPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ResizeControlShelf(
    isBangla: Boolean,
    currentWidth: Int,
    currentHeight: Int,
    customWidth: String,
    customHeight: String,
    isLocked: Boolean,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onToggleLock: () -> Unit,
    onApplyPreset: (Int) -> Unit,
    onApplyCustom: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBangla) "রেজোলিউশন ও সাইজ:" else "Resolution & Resize:",
                color = Color(0xFFCBD5E1),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Current: $currentWidth × $currentHeight px",
                color = CardMateTealPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Quick Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Small (1200px)" to 1200,
                "Medium (2000px)" to 2000,
                "Large (3000px)" to 3000
            ).forEach { (label, dim) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onApplyPreset(dim) }
                ) {
                    Text(
                        text = label,
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        // Custom Width × Height Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = customWidth,
                onValueChange = onWidthChange,
                label = { Text("Width", fontSize = 10.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CardMateTealPrimary,
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onToggleLock, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock Ratio",
                    tint = if (isLocked) CardMateTealPrimary else Color(0xFF94A3B8)
                )
            }

            OutlinedTextField(
                value = customHeight,
                onValueChange = onHeightChange,
                label = { Text("Height", fontSize = 10.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CardMateTealPrimary,
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onApplyCustom,
                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(if (isBangla) "প্রয়োগ" else "Apply", color = Color(0xFF042F2E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EnhanceControlShelf(
    isBangla: Boolean,
    params: ImageEnhanceParams,
    onParamsChange: (ImageEnhanceParams) -> Unit,
    onApplyPermanent: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Quick Filter Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Original" to ImageEnhanceParams.DEFAULT,
                "🤖 OCR Booster" to ImageEnhanceParams.OCR_BOOST,
                "📄 Clean Doc" to ImageEnhanceParams.CLEAN_DOCUMENT,
                "📸 Vivid" to ImageEnhanceParams.VIVID_PHOTO,
                "⚡ High B&W" to ImageEnhanceParams.HIGH_CONTRAST_BW
            ).forEach { (label, preset) ->
                val isSelected = params == preset
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) CardMateGoldAccent else Color(0xFF1E293B),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onParamsChange(preset) }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF0F172A) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Fine Adjustment Sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Contrast: ${String.format("%.1f", params.contrast)}x",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.5.sp
                )
                Slider(
                    value = params.contrast,
                    onValueChange = { onParamsChange(params.copy(contrast = it)) },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = CardMateTealPrimary, activeTrackColor = CardMateTealPrimary)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sharpness: ${String.format("%.1f", params.sharpness)}x",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.5.sp
                )
                Slider(
                    value = params.sharpness,
                    onValueChange = { onParamsChange(params.copy(sharpness = it)) },
                    valueRange = 0.0f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = CardMateGoldAccent, activeTrackColor = CardMateGoldAccent)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBangla) "OCR এর জন্য 'OCR Booster' ফিল্টারটি সেরা ফলাফল দেয়।" else "Tip: 'OCR Booster' optimizes text recognition accuracy.",
                color = Color(0xFF94A3B8),
                fontSize = 10.5.sp,
                modifier = Modifier.weight(1f)
            )

            if (!params.isDefault) {
                Button(
                    onClick = onApplyPermanent,
                    colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(if (isBangla) "ফিল্টার যুক্ত করুন" else "Bake Filter", color = Color(0xFF042F2E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QualityAiScorecardShelf(
    isBangla: Boolean,
    qualityScore: ImageQualityScore?,
    onReanalyze: () -> Unit
) {
    if (qualityScore == null) {
        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CardMateGoldAccent, modifier = Modifier.size(24.dp))
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CardMateGoldAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBangla) "এআই কার্ড কোয়ালিটি স্কোর:" else "AI Card Quality Score:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (qualityScore.overallScore >= 75) StatusSuccess.copy(alpha = 0.2f) else CardMateGoldAccent.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "${qualityScore.overallScore}/100 • ★ ${qualityScore.starRating}",
                    color = if (qualityScore.overallScore >= 75) StatusSuccess else CardMateGoldAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // 4 Metric Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                (if (isBangla) "শার্পনেস" else "Sharpness") to "${qualityScore.sharpnessScore}%",
                (if (isBangla) "আলো" else "Lighting") to "${qualityScore.lightingScore}%",
                (if (isBangla) "কনট্রাস্ট" else "Contrast") to "${qualityScore.contrastScore}%",
                (if (isBangla) "পাঠযোগ্যতা" else "Legibility") to "${qualityScore.textLegibilityScore}%"
            ).forEach { (label, value) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = label, color = Color(0xFF94A3B8), fontSize = 9.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = value, color = CardMateTealPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            text = if (isBangla) qualityScore.feedbackBn else qualityScore.feedbackEn,
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}
