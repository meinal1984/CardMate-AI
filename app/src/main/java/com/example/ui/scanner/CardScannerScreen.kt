package com.example.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.ai.DuplicateDetectionHelper
import com.example.ai.DuplicateMatch
import com.example.data.model.BusinessCard
import com.example.data.model.CardCategory
import com.example.data.model.CardTemplate
import com.example.image.CardDetectionState
import com.example.image.SmartCardDetectionEngine
import com.example.image.DetectedCardFrame
import com.example.ui.ai.DuplicateMergeDialog
import com.example.ui.components.DigitalBusinessCardView
import com.example.ui.components.ScannerReticleView
import com.example.ui.components.ZoomableCardImageView
import com.example.ui.image.ImageEditorScreen
import com.example.ui.navigation.BackNavigationService
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel
import com.example.ui.viewmodel.ScanMode
import com.example.ui.viewmodel.ScanStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScannerScreen(
    viewModel: CardViewModel,
    onCardSaved: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scannerState by viewModel.scannerState.collectAsState()
    val isBangla by viewModel.isBanglaLanguage.collectAsState()

    val isAutoSyncToPhoneContactsEnabled by viewModel.isAutoSyncToPhoneContactsEnabled.collectAsState()

    var isTorchOn by remember { mutableStateOf(false) }
    var showScanResultSheet by remember { mutableStateOf(false) }
    var activePresetSizeIndex by remember { mutableIntStateOf(0) }
    var useFrontCamera by remember { mutableStateOf(false) }

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                if (isBangla) "ক্যামেরা চালু করার জন্য অনুমতি প্রয়োজন" else "Camera permission is required to scan cards",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Auto-request permission on screen load if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val navService = remember { BackNavigationService.instance }

    // Register active modal sheet with centralized back navigation
    DisposableEffect(showScanResultSheet) {
        if (showScanResultSheet) {
            val unreg = navService.registerModal {
                showScanResultSheet = false
                true
            }
            onDispose { unreg() }
        } else onDispose {}
    }

    // Register camera active state and cleanup handler
    DisposableEffect(hasCameraPermission) {
        navService.isCameraActive = hasCameraPermission
        val unreg = navService.registerResourceCleanups(
            onCameraCleanup = {
                if (showScanResultSheet) {
                    showScanResultSheet = false
                    true
                } else if (scannerState.currentStep != ScanStep.FRONT) {
                    viewModel.retakeFrontSide()
                    true
                } else {
                    false
                }
            }
        )
        onDispose {
            navService.isCameraActive = false
            unreg()
        }
    }

    // Predefined standard physical dimensions
    val sizePresets = listOf(
        Triple(88.9f, 50.8f, "Standard US (3.5\" × 2.0\")"),
        Triple(85.6f, 53.98f, "ISO Credit Card (85.6 × 54.0 mm)"),
        Triple(90.0f, 50.0f, "European (90 × 50 mm)"),
        Triple(91.0f, 55.0f, "Asian Meishi (91 × 55 mm)"),
        Triple(65.0f, 65.0f, "Square (65 × 65 mm)")
    )

    // CameraX and Smart Detection references
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detectionEngine = remember { SmartCardDetectionEngine() }

    // Editor modal target state
    var editorTargetUri by remember { mutableStateOf<String?>(null) }
    var editorIsFrontSide by remember { mutableStateOf(true) }

    // Camera & Pre-OCR Settings Modal Sheet State
    var showCameraSettingsSheet by remember { mutableStateOf(false) }

    // Auto-capture flag to prevent re-triggering during active capture
    var isAutoCapturingNow by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            analysisExecutor.shutdown()
        }
    }

    // Gallery Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processImageUri(
                context = context,
                uri = uri,
                onFrontCapturedForDual = {
                    Toast.makeText(
                        context,
                        if (isBangla) "সামনের ছবি নেওয়া হয়েছে! এবার পেছনের ছবি সিলেক্ট করুন" else "Front side selected! Now pick the back side image.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onReadyToReview = {
                    showScanResultSheet = true
                }
            )
        }
    }

    // System Native Camera Launcher (Take Photo directly via default camera app)
    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.handleCapturedImage(
                context = context,
                rawBitmap = bitmap,
                onFrontCapturedForDual = {
                    Toast.makeText(
                        context,
                        if (isBangla) "সামনের ছবি তোলা হয়েছে! এবার পেছনের ছবি তুলুন" else "Front photo taken! Now capture the back side.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onReadyToReview = {
                    showScanResultSheet = true
                }
            )
        }
    }

    // Live Camera capture handler
    val takeLivePhoto = {
        val capture = imageCapture
        if (capture != null) {
            capture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val bitmap = image.toBitmap()
                            val rotationDegrees = image.imageInfo.rotationDegrees
                            val finalBitmap = if (rotationDegrees != 0) {
                                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            } else {
                                bitmap
                            }
                            image.close()

                            ContextCompat.getMainExecutor(context).execute {
                                viewModel.handleCapturedImage(
                                    context = context,
                                    rawBitmap = finalBitmap,
                                    onFrontCapturedForDual = {
                                        Toast.makeText(
                                            context,
                                            if (isBangla) "সামনের দিক স্ক্যান সম্পন্ন! এবার কার্ডটি উল্টে পেছনের দিক ধরুন।" else "Front side captured! Now flip card to scan the back side.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onReadyToReview = {
                                        showScanResultSheet = true
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            image.close()
                            ContextCompat.getMainExecutor(context).execute {
                                Toast.makeText(
                                    context,
                                    if (isBangla) "ছবি প্রসেস করতে ত্রুটি হয়েছে: ${e.message}" else "Capture error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        ContextCompat.getMainExecutor(context).execute {
                            Toast.makeText(
                                context,
                                if (isBangla) "ক্যামেরা ছবি তুলতে পারেনি, সিস্টেম ক্যামেরা ব্যবহার করুন" else "Camera capture failed, using system camera",
                                Toast.LENGTH_SHORT
                            ).show()
                            systemCameraLauncher.launch(null)
                        }
                    }
                }
            )
        } else {
            systemCameraLauncher.launch(null)
        }
    }

    // Auto-capture automated trigger when card is continuously stable
    LaunchedEffect(scannerState.detectionState, scannerState.isAutoCaptureEnabled, scannerState.isStable, isAutoCapturingNow, showScanResultSheet) {
        if (scannerState.isAutoCaptureEnabled &&
            scannerState.isStable &&
            scannerState.detectionState == CardDetectionState.CARD_STABLE &&
            !scannerState.isProcessingAi &&
            !isAutoCapturingNow &&
            !showScanResultSheet &&
            editorTargetUri == null
        ) {
            isAutoCapturingNow = true
            delay(400) // Brief stability confirmation delay
            if (scannerState.isStable && !showScanResultSheet && editorTargetUri == null) {
                takeLivePhoto()
            }
            delay(1200)
            isAutoCapturingNow = false
        }
    }

    // Demo simulated dual-sided card capture
    val takeDemoPhoto = {
        val currentPreset = sizePresets[activePresetSizeIndex]
        val frontBitmap = createSimulatedCardFrontBitmap(
            name = if (isBangla) "ইঞ্জিঃ তানভীর আহমেদ (Tanvir Ahmed)" else "Engr. Tanvir Ahmed",
            title = if (isBangla) "হেড অফ এন্টারপ্রাইজ ফিনটেক" else "Head of Enterprise FinTech",
            company = if (isBangla) "প্রাইম ফিনটেক গ্লোবাল লিমিটেড" else "Prime FinTech Global Ltd.",
            phone = "+880 1715-443322",
            email = "tanvir.ahmed@primefintech.bd",
            widthMm = currentPreset.first,
            heightMm = currentPreset.second
        )
        val backBitmap = createSimulatedCardBackBitmap(
            company = if (isBangla) "প্রাইম ফিনটেক গ্লোবাল লিমিটেড" else "Prime FinTech Global Ltd.",
            services = if (isBangla) "কোর ব্যাংকিং, এআই পেমেন্ট গেটওয়ে, এনএফসি স্মার্ট ওয়ালেট ও ব্লকচেইন সলিউশনস" else "Core Banking, AI Payment Gateway, NFC Smart Wallet & Blockchain Solutions",
            address = if (isBangla) "বাড়ি ৪২, রোড ১১, ব্লক ডি, বনানী, ঢাকা ১২১৩" else "House 42, Road 11, Block D, Banani, Dhaka-1213, Bangladesh",
            website = "https://primefintech.bd",
            secondaryPhone = "+880 1915-443322",
            widthMm = currentPreset.first,
            heightMm = currentPreset.second
        )

        viewModel.saveBitmapToStorage(context, frontBitmap, "demo_front")
        viewModel.saveBitmapToStorage(context, backBitmap, "demo_back")

        if (scannerState.scanMode == ScanMode.DUAL_SIDE) {
            viewModel.processDualImageScan(context, frontBitmap, backBitmap)
        } else {
            viewModel.processSingleImageScan(context, frontBitmap)
        }
        showScanResultSheet = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("scanner_screen")
    ) {
        // Live Camera Preview Background
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                try {
                                    val detected = detectionEngine.analyzeImageProxy(imageProxy)
                                    if (detected != null) {
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            viewModel.updateDetectionFrame(detected)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = if (useFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }

                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture,
                                imageAnalysis
                            )
                            camera = cam
                            imageCapture = capture
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                update = {
                    try {
                        camera?.cameraControl?.enableTorch(isTorchOn)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )
        }

        // Overlay & Controls UI
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Scanner Top Bar: Controls, Mode Selector & Flash
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xEE090D16), Color(0x99090D16), Color.Transparent)
                        )
                    )
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App / Mode badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xCC0F172A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = CardMateGoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "এআই কার্ড স্ক্যানার" else "AI Card Scanner",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Top Action Icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Adaptive Enhancements Status Chip / Quick Toggle
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (scannerState.isAdaptiveEnhancementEnabled) CardMateCyanAccent.copy(alpha = 0.22f) else Color(0xCC0F172A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (scannerState.isAdaptiveEnhancementEnabled) CardMateCyanAccent else Color(0x5564748B)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    showCameraSettingsSheet = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = "Adaptive Enhancements",
                                    tint = if (scannerState.isAdaptiveEnhancementEnabled) CardMateCyanAccent else Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (scannerState.isAdaptiveEnhancementEnabled) (if (isBangla) "✨ এনহ্যান্স" else "✨ Adaptive") else (if (isBangla) "অফ" else "Off"),
                                    color = if (scannerState.isAdaptiveEnhancementEnabled) CardMateCyanAccent else Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Auto-Capture vs Manual Toggle Chip
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (scannerState.isAutoCaptureEnabled) CardMateTealPrimary.copy(alpha = 0.22f) else Color(0xCC0F172A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (scannerState.isAutoCaptureEnabled) CardMateTealPrimary else Color(0x5564748B)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.setAutoCaptureEnabled(!scannerState.isAutoCaptureEnabled)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (scannerState.isAutoCaptureEnabled) Icons.Default.FlashAuto else Icons.Default.TouchApp,
                                    contentDescription = "Auto Capture Toggle",
                                    tint = if (scannerState.isAutoCaptureEnabled) CardMateTealPrimary else Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (scannerState.isAutoCaptureEnabled) (if (isBangla) "⚡ অটো" else "⚡ Auto") else (if (isBangla) "ম্যানুয়াল" else "Manual"),
                                    color = if (scannerState.isAutoCaptureEnabled) CardMateTealPrimary else Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Camera & Pre-OCR Settings Button
                        IconButton(
                            onClick = { showCameraSettingsSheet = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC0F172A))
                                .border(1.dp, if (scannerState.isAdaptiveEnhancementEnabled) CardMateCyanAccent.copy(alpha = 0.5f) else Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Camera Settings",
                                tint = if (scannerState.isAdaptiveEnhancementEnabled) CardMateCyanAccent else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Switch Camera (Front/Back)
                        IconButton(
                            onClick = { useFrontCamera = !useFrontCamera },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC0F172A))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Flash / Torch Toggle
                        IconButton(
                            onClick = {
                                isTorchOn = !isTorchOn
                                try {
                                    camera?.cameraControl?.enableTorch(isTorchOn)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isTorchOn) CardMateGoldAccent else Color(0xCC0F172A))
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = if (isTorchOn) Color(0xFF0F172A) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Dual-Side Progress Steps Indicator
                if (scannerState.scanMode == ScanMode.DUAL_SIDE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isFrontDone = scannerState.frontBitmap != null || scannerState.currentStep == ScanStep.BACK
                        val isBackStep = scannerState.currentStep == ScanStep.BACK

                        // Step 1: Front Side
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isFrontDone && isBackStep) Color(0x3334D399) else if (!isBackStep) CardMateCyanAccent.copy(alpha = 0.25f) else Color(0x331E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (!isBackStep) CardMateCyanAccent else if (isFrontDone) Color(0xFF34D399) else Color(0x3364748B)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (isBackStep) {
                                        viewModel.retakeFrontSide()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isFrontDone && isBackStep) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(13.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(if (!isBackStep) CardMateCyanAccent else Color.Gray, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("1", color = Color(0xFF0F172A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isBangla) "১. সামনের পাশ" else "1. Front Side",
                                    color = if (!isBackStep) CardMateCyanAccent else if (isFrontDone) Color(0xFF34D399) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = if (!isBackStep) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(14.dp)
                        )

                        // Step 2: Back Side
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isBackStep) CardMateGoldAccent.copy(alpha = 0.25f) else Color(0x331E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isBackStep) CardMateGoldAccent else Color(0x3364748B)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(if (isBackStep) CardMateGoldAccent else Color.Gray, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("2", color = Color(0xFF0F172A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isBangla) "২. পেছনের পাশ" else "2. Back Side",
                                    color = if (isBackStep) CardMateGoldAccent else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = if (isBackStep) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Central Viewfinder with Reticles or Permission Notice
            if (!hasCameraPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(CardMateTealPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = CardMateTealPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isBangla) "ক্যামেরা এক্সেস প্রয়োজন" else "Camera Permission Required",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBangla) "ভিজিটিং কার্ডের উভয় পাশ তাৎক্ষণিক স্ক্যান ও মার্জ করার জন্য ক্যামেরা অনুমতি দিন।" else "Grant camera permission to capture and synthesize both sides of business cards with AI OCR.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF042F2E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "ক্যামেরা অনুমতি দিন" else "Grant Permission",
                                color = Color(0xFF042F2E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { systemCameraLauncher.launch(null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBangla) "সিস্টেম ক্যামেরা দিয়ে ছবি তুলুন" else "Use System Camera App",
                                color = CardMateCyanAccent,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val preset = sizePresets[activePresetSizeIndex]
                    val isBackStep = scannerState.scanMode == ScanMode.DUAL_SIDE && scannerState.currentStep == ScanStep.BACK

                    // Interactive Step Guide Banner on Viewfinder
                    if (isBackStep) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xE60F172A),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, CardMateGoldAccent),
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Front Thumbnail Preview
                                scannerState.frontBitmap?.let { frontBmp ->
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp, 34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFF34D399), RoundedCornerShape(6.dp))
                                    ) {
                                        Image(
                                            bitmap = frontBmp.asImageBitmap(),
                                            contentDescription = "Captured Front",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(14.dp)
                                                .background(Color(0xFF34D399), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(10.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Autorenew, contentDescription = null, tint = CardMateGoldAccent, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBangla) "কার্ডটি উল্টে পেছনের পাশ ধরুন" else "Flip Card: Scan Back Side",
                                            color = CardMateGoldAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = if (isBangla) "ঠিকানা, সেবাসমূহ ও দ্বিতীয় নম্বর স্ক্যান হবে" else "Captures services, QR, website & branches",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                }

                                // Quick Skip / Done button if user has no back side
                                TextButton(
                                    onClick = {
                                        viewModel.skipBackSideAndProcess(context) {
                                            showScanResultSheet = true
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (isBangla) "পেছন বাদ দিন" else "Skip",
                                        color = CardMateCyanAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    ScannerReticleView(
                        detectedWidthMm = preset.first,
                        detectedHeightMm = preset.second,
                        standardSizeName = if (isBackStep) {
                            if (isBangla) "পেছনের পাশ স্ক্যানিং (${preset.third})" else "Back Side Alignment (${preset.third})"
                        } else {
                            if (isBangla) "সামনের পাশ স্ক্যানিং (${preset.third})" else "Front Side Alignment (${preset.third})"
                        },
                        isScanningActive = true,
                        isBangla = isBangla,
                        detectionState = scannerState.detectionState,
                        quadCorners = scannerState.detectedCorners,
                        isAutoCaptureEnabled = scannerState.isAutoCaptureEnabled,
                        stableProgress = scannerState.stableProgress,
                        hasGlareWarning = scannerState.hasGlare,
                        isBlurWarning = scannerState.isBlurry
                    )
                }
            }

            // Bottom Shutter Controls & Gallery Pick
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xEE0F172A))
                    .padding(vertical = 18.dp, horizontal = 24.dp)
            ) {
                val isBackStep = scannerState.scanMode == ScanMode.DUAL_SIDE && scannerState.currentStep == ScanStep.BACK

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pick from Gallery
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Upload Card Image",
                                tint = CardMateCyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBangla) "গ্যালারি" else "Gallery",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    // Shutter Capture Button (Live Camera or System fallback)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val shutterBorderColor = if (isBackStep) CardMateGoldAccent else CardMateTealPrimary

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(3.5.dp, shutterBorderColor, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    if (hasCameraPermission) {
                                        takeLivePhoto()
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                                .testTag("camera_shutter_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBackStep) Icons.Default.Autorenew else Icons.Default.CameraAlt,
                                contentDescription = "Capture Card",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBackStep) {
                                if (isBangla) "পেছন স্ক্যান" else "Capture Back"
                            } else {
                                if (isBangla) "সামনে স্ক্যান" else "Capture Front"
                            },
                            color = if (isBackStep) CardMateGoldAccent else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick AI Demo Dual-Sided Scan
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { takeDemoPhoto() },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Instant AI Demo Scan",
                                tint = CardMateGoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBangla) "ডেমো কার্ড" else "Demo Card",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Quality Warning Alert Dialog
        if (scannerState.hasQualityWarning) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissQualityWarning() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Quality Warning",
                        tint = Color(0xFFFB923C),
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isBangla) "ছবির মান সংক্রান্ত সতর্কতা" else "Image Quality Alert",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = scannerState.qualityWarningMessage
                                ?: (if (isBangla) "ছবিটি কিছুটা ঝাপসা বা আলো কম হতে পারে।" else "The captured photo may have glare or motion blur."),
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isBangla)
                                "আপনি কি পুনরায় ছবি তুলতে চান নাকি এআই রিকগনিশন চালিয়ে যাবেন?"
                            else
                                "You can retake the photo for better accuracy or continue with AI extraction.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissQualityWarning() },
                        colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary)
                    ) {
                        Text(
                            text = if (isBangla) "চালিয়ে যান" else "Continue",
                            color = Color(0xFF042F2E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.dismissQualityWarning()
                                if (scannerState.currentStep == ScanStep.FRONT) {
                                    viewModel.retakeFrontSide()
                                } else {
                                    viewModel.retakeBackSide()
                                }
                            }
                        ) {
                            Text(
                                text = if (isBangla) "পুনরায় তুলুন" else "Retake",
                                color = Color(0xFFF43F5E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Dedicated Crop / Perspective Adjuster Fullscreen Modal
        editorTargetUri?.let { targetUri ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
            ) {
                ImageEditorScreen(
                    viewModel = viewModel,
                    card = null,
                    initialImageUri = targetUri,
                    side = if (editorIsFrontSide) "front" else "back",
                    onBack = {
                        editorTargetUri = null
                    },
                    onSaved = { savedPath ->
                        val isFront = editorIsFrontSide
                        editorTargetUri = null
                        viewModel.applyAdjustedCropToScanResult(context, isFront, savedPath) {
                            showScanResultSheet = true
                        }
                    }
                )
            }
        }

        // Post-Scan Review Modal Bottom Sheet
        if (showScanResultSheet && editorTargetUri == null && (scannerState.isProcessingAi || scannerState.scanResult != null || scannerState.errorMessage != null)) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = {
                    showScanResultSheet = false
                    viewModel.clearScanner()
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ScanResultEditorContent(
                    viewModel = viewModel,
                    scannerState = scannerState,
                    isBangla = isBangla,
                    isAutoSyncEnabled = isAutoSyncToPhoneContactsEnabled,
                    onRetakeFront = {
                        showScanResultSheet = false
                        viewModel.retakeFrontSide()
                    },
                    onRetakeBack = {
                        showScanResultSheet = false
                        viewModel.retakeBackSide()
                    },
                    onAdjustCrop = { uri, isFront ->
                        editorTargetUri = uri
                        editorIsFrontSide = isFront
                    },
                    onSave = { card ->
                        viewModel.saveCard(card) { savedId ->
                            showScanResultSheet = false
                            viewModel.clearScanner()
                            onCardSaved(savedId)
                        }
                    },
                    onDismiss = {
                        showScanResultSheet = false
                        viewModel.clearScanner()
                    }
                )
            }
        }

        // Camera & Pre-OCR Settings Modal Bottom Sheet
        if (showCameraSettingsSheet) {
            CameraSettingsBottomSheet(
                isBangla = isBangla,
                scanMode = scannerState.scanMode,
                onScanModeChanged = { mode ->
                    viewModel.setScanMode(mode)
                },
                isAdaptiveEnhancementEnabled = scannerState.isAdaptiveEnhancementEnabled,
                onAdaptiveEnhancementToggled = { enabled ->
                    viewModel.setAdaptiveEnhancementEnabled(enabled)
                },
                isAutoCaptureEnabled = scannerState.isAutoCaptureEnabled,
                onAutoCaptureToggled = { enabled ->
                    viewModel.setAutoCaptureEnabled(enabled)
                },
                lastAppliedAdaptiveEnhancement = scannerState.lastAppliedAdaptiveEnhancement,
                selectedAspectRatio = scannerState.aspectRatio,
                onAspectRatioSelected = { ratio, label ->
                    when {
                        label.contains("US", ignoreCase = true) -> {
                            activePresetSizeIndex = 0
                            viewModel.updateScannerAspectRatio(89, 51)
                        }
                        label.contains("ISO", ignoreCase = true) || label.contains("Credit", ignoreCase = true) -> {
                            activePresetSizeIndex = 1
                            viewModel.updateScannerAspectRatio(86, 54)
                        }
                        label.contains("EU", ignoreCase = true) -> {
                            activePresetSizeIndex = 2
                            viewModel.updateScannerAspectRatio(90, 50)
                        }
                        label.contains("Japanese", ignoreCase = true) || label.contains("Meishi", ignoreCase = true) -> {
                            activePresetSizeIndex = 3
                            viewModel.updateScannerAspectRatio(91, 55)
                        }
                        else -> {
                            viewModel.updateScannerAspectRatio(
                                if (ratio > 1.6f) 89 else 85,
                                if (ratio > 1.6f) 51 else 54
                            )
                        }
                    }
                },
                onDismiss = {
                    showCameraSettingsSheet = false
                }
            )
        }
    }
}

@Composable
private fun ScanResultEditorContent(
    viewModel: CardViewModel,
    scannerState: com.example.ui.viewmodel.ScannerUiState,
    isBangla: Boolean,
    isAutoSyncEnabled: Boolean = true,
    onRetakeFront: () -> Unit,
    onRetakeBack: () -> Unit,
    onAdjustCrop: (String, Boolean) -> Unit,
    onSave: (BusinessCard) -> Unit,
    onDismiss: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()

    if (scannerState.isProcessingAi) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = CardMateTealPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = scannerState.progressMessage.ifBlank { "Processing Card with Gemini AI..." },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isBangla) "উভয় পাশের বাংলা ও ইংরেজি তথ্য এবং যোগাযোগের মাধ্যম একীভূত করা হচ্ছে..." else "Fusing Front & Back bilingual contact details, services & dimensions...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (scannerState.errorMessage != null && scannerState.scanResult == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isBangla) "স্ক্যান ব্যর্থ হয়েছে" else "Scan Failed",
                color = MaterialTheme.colorScheme.error,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = scannerState.errorMessage ?: "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary)
            ) {
                Text(if (isBangla) "ঠিক আছে" else "Close", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    val res = scannerState.scanResult ?: return

    val initialScannedPrimaryPhones = com.example.util.PhoneNumberUtils.splitPhoneNumbers(res.phone)
    val initialScannedSecondaryPhones = com.example.util.PhoneNumberUtils.splitPhoneNumbers(res.secondaryPhone)

    var name by remember { mutableStateOf(res.fullName) }
    var title by remember { mutableStateOf(res.jobTitle) }
    var company by remember { mutableStateOf(res.company) }
    var phone by remember {
        mutableStateOf(initialScannedPrimaryPhones.firstOrNull() ?: res.phone)
    }
    var additionalPhones by remember {
        mutableStateOf<List<String>>(
            buildList {
                if (initialScannedPrimaryPhones.size > 1) {
                    addAll(initialScannedPrimaryPhones.drop(1))
                }
                addAll(initialScannedSecondaryPhones)
            }
        )
    }
    var email by remember { mutableStateOf(res.email) }
    var website by remember { mutableStateOf(res.website) }
    var address by remember { mutableStateOf(res.address) }
    var category by remember { mutableStateOf(res.category.ifBlank { "Corporate" }) }
    var notes by remember { mutableStateOf(res.notes) }
    var socialLinks by remember { mutableStateOf(res.socialLinks) }
    var selectedTemplate by remember { mutableStateOf(res.detectedTemplate.ifBlank { "modern_slate" }) }
    var customPrimaryBg by remember { mutableStateOf(res.detectedPrimaryBgColor) }
    var customSecondaryBg by remember { mutableStateOf(res.detectedSecondaryBgColor) }
    var customAccent by remember { mutableStateOf(res.detectedAccentColor) }
    var customText by remember { mutableStateOf(res.detectedTextColor) }
    var layoutStyle by remember { mutableStateOf(res.detectedLayoutStyle.ifBlank { "Modern Clean" }) }
    var bgPattern by remember { mutableStateOf(res.detectedBgPattern.ifBlank { "plain" }) }
    var isAutoMatchedThemeActive by remember { mutableStateOf(true) }

    var activePhotoPreviewTab by remember { mutableIntStateOf(0) } // 0 = Front photo, 1 = Back photo
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    var pendingDuplicateMatch by remember { mutableStateOf<DuplicateMatch?>(null) }
    var pendingCardToSave by remember { mutableStateOf<BusinessCard?>(null) }

    // AI Smart Suggested Categories derived from context
    val aiSuggestedCategories = remember(name, title, company, notes, address) {
        val text = "$name $title $company $notes $address".lowercase()
        val suggestions = mutableListOf<String>()
        if (text.contains("gov") || text.contains("ministry") || text.contains("secretary") || text.contains("department") || text.contains("govt") || text.contains("সরকারি") || text.contains("মন্ত্রণালয়")) {
            suggestions.add("Government")
        }
        if (text.contains("engineer") || text.contains("architect") || text.contains("civil") || text.contains("developer") || text.contains("software") || text.contains("tech") || text.contains("ইঞ্জিনিয়ার") || text.contains("প্রকৌশলী")) {
            suggestions.add("Engineering")
        }
        if (text.contains("consult") || text.contains("advis") || text.contains("partner") || text.contains("expert") || text.contains("কন্সালট্যান্ট") || text.contains("পরামর্শক")) {
            suggestions.add("Consultancy")
        }
        if (suggestions.isEmpty()) {
            suggestions.addAll(listOf("Government", "Engineering", "Consultancy"))
        }
        suggestions.distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = CardMateTealPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBangla) "উভয় পাশ স্ক্যান ফলাফল" else "Dual-Side Scanned Card",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        // AI Dual Fusion Status Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardMateTealPrimary.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CardMateGoldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (scannerState.backBitmap != null || scannerState.backImageUri != null) {
                            if (isBangla) "✨ সামনের ও পেছনের পাশ সফলভাবে মার্জ করা হয়েছে" else "✨ Front & Back sides merged by Gemini AI"
                        } else {
                            if (isBangla) "✨ সামনের পাশ সফলভাবে স্ক্যান করা হয়েছে" else "✨ Front side scanned successfully"
                        },
                        color = CardMateTealPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${res.standardSizeName} • ${String.format("%.1f", res.detectedWidthMm)} × ${String.format("%.1f", res.detectedHeightMm)} mm",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Pre-OCR Dynamic Enhancements Diagnostic Pill
        if (scannerState.isAdaptiveEnhancementEnabled) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF132338),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardMateCyanAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = CardMateCyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = scannerState.lastAppliedAdaptiveEnhancement ?: if (isBangla) "ডায়নামিক কনট্রাস্ট ও ব্রাইটনেস অ্যাডজাস্টমেন্ট প্রয়োগ করা হয়েছে" else "Dynamic contrast & low-light brightness adjustment applied",
                        color = Color(0xFFBAE6FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Captured Images Dual Preview Box with Pinch to Zoom, Double Tap, Full Screen & OCR text check
        if (scannerState.frontBitmap != null || scannerState.backBitmap != null || scannerState.frontImageUri != null || scannerState.backImageUri != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Retake & Adjust Crop Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (activePhotoPreviewTab == 0) CardMateTealPrimary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { activePhotoPreviewTab = 0 }
                            ) {
                                Text(
                                    text = if (isBangla) "📷 সামনের ছবি (Front)" else "📷 Front Photo",
                                    color = if (activePhotoPreviewTab == 0) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            if (scannerState.backBitmap != null || scannerState.backImageUri != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activePhotoPreviewTab == 1) CardMateGoldAccent else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { activePhotoPreviewTab = 1 }
                                ) {
                                    Text(
                                        text = if (isBangla) "📷 পেছনের ছবি (Back)" else "📷 Back Photo",
                                        color = if (activePhotoPreviewTab == 1) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val activeOriginal = if (activePhotoPreviewTab == 0) {
                                scannerState.frontOriginalUri ?: scannerState.frontImageUri
                            } else {
                                scannerState.backOriginalUri ?: scannerState.backImageUri
                            }

                            if (activeOriginal != null) {
                                OutlinedButton(
                                    onClick = {
                                        onAdjustCrop(activeOriginal, activePhotoPreviewTab == 0)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(12.dp), tint = CardMateCyanAccent)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isBangla) "ক্রপ ঠিক করুন" else "Adjust Crop",
                                        fontSize = 10.5.sp,
                                        color = CardMateCyanAccent
                                    )
                                }
                            }

                            // Retake button for active tab
                            TextButton(
                                onClick = {
                                    if (activePhotoPreviewTab == 0) onRetakeFront() else onRetakeBack()
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isBangla) "পুনরায়" else "Retake",
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    // Smart Perspective Verification Badges Row
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val activeQuality = if (activePhotoPreviewTab == 0) scannerState.frontQualityScore else scannerState.backQualityScore
                        val scoreValue = activeQuality?.overallScore ?: 88

                        // Badge 1: 4 Corners Auto-Detected
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x2234D399)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isBangla) "৪-কোণা ডিটেক্টেড" else "4 Corners Auto-Crop",
                                    color = Color(0xFF34D399),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Badge 2: Perspective Straightened
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CardMateCyanAccent.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Transform, contentDescription = null, tint = CardMateCyanAccent, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isBangla) "পারস্পেক্টিভ সোজা করা হয়েছে" else "Perspective Corrected",
                                    color = CardMateCyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Badge 3: Image Quality Score
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (scoreValue >= 70) Color(0x2238BDF8) else Color(0x22FB923C)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (scoreValue >= 70) Color(0xFF38BDF8) else Color(0xFFFB923C),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isBangla) "কোয়ালিটি: $scoreValue%" else "Quality: $scoreValue%",
                                    color = if (scoreValue >= 70) Color(0xFF38BDF8) else Color(0xFFFB923C),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Badge 4: Dynamic OCR Preprocessing (Low-Light & Poor-Contrast Enhancement)
                        if (activeQuality != null && (activeQuality.isLowLight || activeQuality.isPoorContrast || activeQuality.isOverexposed)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CardMateGoldAccent.copy(alpha = 0.20f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = CardMateGoldAccent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isBangla) "অটো আলো-কনট্রাস্ট বুস্ট" else "Auto Light/Contrast Boost",
                                        color = CardMateGoldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val activeUri = if (activePhotoPreviewTab == 0) {
                        scannerState.frontImageUri ?: scannerState.backImageUri
                    } else {
                        scannerState.backImageUri ?: scannerState.frontImageUri
                    }

                    val activeBitmap = if (activePhotoPreviewTab == 0) {
                        scannerState.frontBitmap ?: scannerState.backBitmap
                    } else {
                        scannerState.backBitmap ?: scannerState.frontBitmap
                    }

                    ZoomableCardImageView(
                        imagePath = activeUri,
                        bitmap = activeBitmap,
                        title = "${name.ifBlank { "Card" }} - ${if (activePhotoPreviewTab == 0) "Front Side" else "Back Side"}",
                        ocrRawText = res.rawOcrText,
                        isBangla = isBangla,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- PHYSICAL CARD THEME AUTO-MATCHING & LIVE PREVIEW ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isAutoMatchedThemeActive) CardMateTealPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = if (isAutoMatchedThemeActive) CardMateTealPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "আসল কার্ডের হুবহু ডিজিটাল থিম" else "Exact Physical Card Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isAutoMatchedThemeActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CardMateTealPrimary.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = if (isBangla) "ডিফল্ট হুবহু ম্যাচড" else "Auto-Matched",
                                color = CardMateTealPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (res.detectedThemeDescription.isNotBlank()) {
                        res.detectedThemeDescription
                    } else if (isBangla) {
                        "স্ক্যান করা কার্ডের ব্যাকগ্রাউন্ড, ব্র্যান্ড অ্যাকসেন্ট ও টেক্সট কালার হুবহু ডিফল্ট থিম হিসেবে সেট করা হয়েছে।"
                    } else {
                        "The background, brand accent, and high-contrast text were automatically matched directly from the physical card."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Color Swatches Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary BG Swatch
                    val bgHex = String.format("#%06X", (customPrimaryBg ?: 0xFFFFFFFFL) and 0xFFFFFFL)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(customPrimaryBg ?: 0xFFFFFFFFL))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "ব্যাকগ্রাউন্ড" else "Background",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = bgHex,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Accent Swatch
                    val accentHex = String.format("#%06X", (customAccent ?: 0xFF0284C7L) and 0xFFFFFFL)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(customAccent ?: 0xFF0284C7L))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "ব্র্যান্ড অ্যাকসেন্ট" else "Accent",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = accentHex,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Text Swatch
                    val textHex = String.format("#%06X", (customText ?: 0xFF0F172AL) and 0xFFFFFFL)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(customText ?: 0xFF0F172AL))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "টেক্সট" else "Text",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = textHex,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Digital Card Preview with extracted colors
                val previewCard = remember(
                    name, title, company, phone, email,
                    selectedTemplate, customPrimaryBg, customSecondaryBg,
                    customAccent, customText, layoutStyle, bgPattern,
                    scannerState.frontImageUri
                ) {
                    BusinessCard(
                        fullName = name.ifBlank { "Full Name" },
                        jobTitle = title,
                        company = company,
                        phone = phone,
                        email = email,
                        cardFrontImageUri = scannerState.frontImageUri,
                        cardLayoutTemplate = selectedTemplate,
                        customPrimaryBgColor = customPrimaryBg,
                        customSecondaryBgColor = customSecondaryBg,
                        customAccentColor = customAccent,
                        customTextColor = customText,
                        layoutStyle = layoutStyle,
                        bgPattern = bgPattern
                    )
                }

                DigitalBusinessCardView(
                    card = previewCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Theme Picker Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option 1: Auto-Matched from physical card (Active by default!)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAutoMatchedThemeActive) CardMateTealPrimary else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAutoMatchedThemeActive) CardMateTealPrimary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isAutoMatchedThemeActive = true
                                selectedTemplate = res.detectedTemplate.ifBlank { "modern_slate" }
                                customPrimaryBg = res.detectedPrimaryBgColor
                                customSecondaryBg = res.detectedSecondaryBgColor
                                customAccent = res.detectedAccentColor
                                customText = res.detectedTextColor
                                layoutStyle = res.detectedLayoutStyle.ifBlank { "Modern Clean" }
                                bgPattern = res.detectedBgPattern.ifBlank { "plain" }
                            }
                    ) {
                        Text(
                            text = if (isBangla) "✨ আসল কার্ড (ডিফল্ট)" else "✨ Physical Card (Default)",
                            color = if (isAutoMatchedThemeActive) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Other Presets
                    CardTemplate.entries.forEach { tpl ->
                        val isSelected = !isAutoMatchedThemeActive && selectedTemplate == tpl.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    isAutoMatchedThemeActive = false
                                    selectedTemplate = tpl.id
                                    customPrimaryBg = tpl.primaryBgColor
                                    customSecondaryBg = tpl.secondaryBgColor
                                    customAccent = tpl.accentColor
                                    customText = tpl.textColor
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(tpl.primaryBgColor))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isBangla) tpl.displayNameBn else tpl.displayNameEn,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Form Fields
        EditableField(label = if (isBangla) "পূর্ণ নাম" else "Full Name", value = name, onValueChange = { name = it })
        EditableField(label = if (isBangla) "পদবী / ডেজিগনেশন" else "Job Title", value = title, onValueChange = { title = it })
        EditableField(label = if (isBangla) "কোম্পানির নাম" else "Company", value = company, onValueChange = { company = it })
        EditableField(
            label = if (isBangla) "প্রধান ফোন নম্বর" else "Primary Phone",
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

        // Multiple fields for each alternate phone number
        additionalPhones.forEachIndexed { index, secPh ->
            DeletableEditableField(
                label = if (isBangla) "বিকল্প ফোন নম্বর ${index + 2}" else "Secondary Phone ${index + 2}",
                value = secPh,
                onValueChange = { newVal ->
                    additionalPhones = com.example.util.PhoneNumberUtils.handlePhoneListEdit(
                        currentList = additionalPhones,
                        index = index,
                        newVal = newVal
                    )
                },
                onDelete = {
                    additionalPhones = additionalPhones.toMutableList().also { it.removeAt(index) }
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { additionalPhones = additionalPhones + "" },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = CardMateTealPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isBangla) "+ বিকল্প ফোন যোগ করুন" else "+ Add Alternate Phone",
                    color = CardMateTealPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        EditableField(label = if (isBangla) "ইমেইল এড্রেস" else "Email", value = email, onValueChange = { email = it })
        EditableField(label = if (isBangla) "ওয়েবসাইট" else "Website", value = website, onValueChange = { website = it })
        EditableField(label = if (isBangla) "ঠিকানা (অফিস / শাখা)" else "Address", value = address, onValueChange = { address = it })
        EditableField(label = if (isBangla) "সেবাসমূহ / পেছনের বিবরণ" else "Services / Back Notes", value = notes, onValueChange = { notes = it })
        EditableField(label = if (isBangla) "সোশ্যাল লিংকসমূহ" else "Social Links", value = socialLinks, onValueChange = { socialLinks = it })

        Spacer(modifier = Modifier.height(12.dp))

        // AI Suggested Categories Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CardMateGoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "AI সাজেস্টেড ক্যাটাগরি" else "Suggested categories",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { showAddCategoryDialog = true }) {
                        Text(
                            text = if (isBangla) "+ কাস্টম" else "+ Custom",
                            color = CardMateTealPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // AI suggestions row (e.g. Government, Engineering, Consultancy)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aiSuggestedCategories.forEach { suggestedCat ->
                        val isSelected = category.equals(suggestedCat, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CardMateTealPrimary else CardMateTealPrimary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { category = suggestedCat }
                        ) {
                            Text(
                                text = if (isSelected) "✓ $suggestedCat" else suggestedCat,
                                color = if (isSelected) Color(0xFF042F2E) else CardMateTealPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isBangla) "অন্যান্য ক্যাটাগরি:" else "All categories:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                val allAvailableCategories = (CardCategory.entries.map { it.englishName } + customCategories).distinct()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allAvailableCategories.forEach { catName ->
                        val isSelected = category.equals(catName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { category = catName }
                        ) {
                            Text(
                                text = catName,
                                color = if (isSelected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Auto-Sync Status Indicator
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isAutoSyncEnabled) CardMateTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = if (isAutoSyncEnabled) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAutoSyncEnabled) {
                        if (isBangla) "⚡ অটো-সিঙ্ক অন: সেভ করলে ফোনের কন্টাক্টে সরাসরি যোগ হবে"
                        else "⚡ Auto-Sync ON: Will automatically save to Phone Contacts"
                    } else {
                        if (isBangla) "ℹ️ কন্টাক্ট অটো-সিঙ্ক বন্ধ (সেটিংস থেকে অন করা যাবে)"
                        else "ℹ️ Contacts Auto-Sync is OFF (Can enable in Settings)"
                    },
                    color = if (isAutoSyncEnabled) CardMateTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Save Button
        Button(
            onClick = {
                val newCard = BusinessCard(
                    fullName = name,
                    jobTitle = title,
                    company = company,
                    phone = phone.trim(),
                    secondaryPhone = additionalPhones.firstOrNull()?.trim() ?: "",
                    email = email,
                    website = website,
                    address = address,
                    category = category,
                    notes = notes,
                    socialLinks = buildList {
                        if (socialLinks.isNotBlank()) add(socialLinks.trim())
                        additionalPhones.drop(1).forEachIndexed { idx, p ->
                            if (p.isNotBlank()) add("${if (isBangla) "বিকল্প ফোন" else "Phone"} ${idx + 3}: ${p.trim()}")
                        }
                    }.joinToString("\n"),
                    cardFrontImageUri = scannerState.frontImageUri,
                    cardBackImageUri = scannerState.backImageUri,
                    cardLayoutTemplate = selectedTemplate,
                    customPrimaryBgColor = customPrimaryBg,
                    customSecondaryBgColor = customSecondaryBg,
                    customAccentColor = customAccent,
                    customTextColor = customText,
                    layoutStyle = layoutStyle,
                    bgPattern = bgPattern,
                    cardWidthMm = res.detectedWidthMm,
                    cardHeightMm = res.detectedHeightMm,
                    cardStandardName = res.standardSizeName,
                    isBackedUpToCloud = true,
                    rawOcrText = res.rawOcrText
                )

                // Check for potential duplicate contacts
                val duplicate = DuplicateDetectionHelper.findBestMatch(newCard, allCards, minScore = 70)
                if (duplicate != null) {
                    pendingDuplicateMatch = duplicate
                    pendingCardToSave = newCard
                } else {
                    onSave(newCard)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CardMateTealPrimary,
                contentColor = Color(0xFF042F2E)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_scanned_card_button")
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isBangla) "উভয় পাশের তথ্য সেভ করুন" else "Save Business Card",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
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
                onDismiss()
            },
            onKeepSeparate = {
                val cardToSave = pendingCardToSave ?: return@DuplicateMergeDialog
                pendingDuplicateMatch = null
                pendingCardToSave = null
                onSave(cardToSave)
            },
            onDismiss = {
                pendingDuplicateMatch = null
                pendingCardToSave = null
            }
        )
    }
}

@Composable
private fun DeletableEditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
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
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
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
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun createSimulatedCardFrontBitmap(
    name: String,
    title: String,
    company: String,
    phone: String,
    email: String,
    widthMm: Float,
    heightMm: Float
): Bitmap {
    val pxW = 889
    val pxH = 508
    val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#0F172A") }
    canvas.drawRect(0f, 0f, pxW.toFloat(), pxH.toFloat(), bgPaint)

    val borderPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#2DD4BF")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRoundRect(10f, 10f, (pxW - 10).toFloat(), (pxH - 10).toFloat(), 24f, 24f, borderPaint)

    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 36f
        isAntiAlias = true
        isFakeBoldText = true
    }
    canvas.drawText(company, 50f, 90f, textPaint)

    textPaint.textSize = 44f
    canvas.drawText(name, 50f, 210f, textPaint)

    textPaint.textSize = 28f
    textPaint.color = android.graphics.Color.parseColor("#2DD4BF")
    canvas.drawText(title, 50f, 265f, textPaint)

    textPaint.textSize = 26f
    textPaint.color = android.graphics.Color.parseColor("#E2E8F0")
    canvas.drawText("Phone: $phone", 50f, 370f, textPaint)
    canvas.drawText("Email: $email", 50f, 420f, textPaint)

    return bitmap
}

private fun createSimulatedCardBackBitmap(
    company: String,
    services: String,
    address: String,
    website: String,
    secondaryPhone: String,
    widthMm: Float,
    heightMm: Float
): Bitmap {
    val pxW = 889
    val pxH = 508
    val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#042F2E") }
    canvas.drawRect(0f, 0f, pxW.toFloat(), pxH.toFloat(), bgPaint)

    val borderPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FBBF24")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRoundRect(10f, 10f, (pxW - 10).toFloat(), (pxH - 10).toFloat(), 24f, 24f, borderPaint)

    val textPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FBBF24")
        textSize = 34f
        isAntiAlias = true
        isFakeBoldText = true
    }
    canvas.drawText("OUR SERVICES & SOLUTIONS", 50f, 85f, textPaint)

    textPaint.textSize = 24f
    textPaint.color = android.graphics.Color.WHITE
    canvas.drawText(services, 50f, 150f, textPaint)

    textPaint.color = android.graphics.Color.parseColor("#2DD4BF")
    canvas.drawText("Website: $website", 50f, 240f, textPaint)
    canvas.drawText("Direct Line: $secondaryPhone", 50f, 290f, textPaint)

    textPaint.color = android.graphics.Color.parseColor("#CBD5E1")
    canvas.drawText("Office Address:", 50f, 360f, textPaint)
    canvas.drawText(address, 50f, 410f, textPaint)

    return bitmap
}
