package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PhotoStorageHelper {
    /**
     * Saves an image Uri (from phone gallery or external provider) into the app's internal storage
     * so it persists permanently and can be read by Coil or exported without permission expiry.
     */
    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, "profile_photos")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val destinationFile = File(directory, "avatar_${System.currentTimeMillis()}.jpg")
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val outputStream = FileOutputStream(destinationFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destinationFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a captured camera Bitmap into app's internal storage.
     */
    suspend fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, "profile_photos")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val destinationFile = File(directory, "avatar_cam_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(destinationFile)
            outputStream.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            Uri.fromFile(destinationFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoPickerBottomSheet(
    currentPhotoUri: String?,
    isBangla: Boolean,
    onPhotoSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var previewPhotoUri by remember { mutableStateOf(currentPhotoUri) }
    var customUrlInput by remember { mutableStateOf(currentPhotoUri?.takeIf { it.startsWith("http") } ?: "") }
    var isUrlSectionExpanded by remember { mutableStateOf(false) }

    // Launcher for Gallery Picker (Photo Picker / MediaStore)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = PhotoStorageHelper.saveImageToInternalStorage(context, uri)
                if (savedPath != null) {
                    previewPhotoUri = savedPath
                    onPhotoSelected(savedPath)
                    Toast.makeText(
                        context,
                        if (isBangla) "গ্যালারি থেকে ফটো সফলভাবে যুক্ত হয়েছে!" else "Photo selected from gallery!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                } else {
                    previewPhotoUri = uri.toString()
                    onPhotoSelected(uri.toString())
                    onDismiss()
                }
            }
        }
    }

    // Fallback GetContent for broad compatibility
    val fallbackGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = PhotoStorageHelper.saveImageToInternalStorage(context, uri)
                val finalUri = savedPath ?: uri.toString()
                previewPhotoUri = finalUri
                onPhotoSelected(finalUri)
                Toast.makeText(
                    context,
                    if (isBangla) "ফটো সফলভাবে যুক্ত হয়েছে!" else "Photo attached successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                onDismiss()
            }
        }
    }

    // Launcher for Camera Snapshot
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                val savedPath = PhotoStorageHelper.saveBitmapToInternalStorage(context, bitmap)
                if (savedPath != null) {
                    previewPhotoUri = savedPath
                    onPhotoSelected(savedPath)
                    Toast.makeText(
                        context,
                        if (isBangla) "ক্যামেরা ছবি সফলভাবে সেট করা হয়েছে!" else "Camera photo set successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                }
            }
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                if (isBangla) "ক্যামেরা ব্যবহারের অনুমতি প্রয়োজন" else "Camera permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val sampleAvatars = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=200&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200&auto=format&fit=crop&q=80"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "প্রোফাইল ছবি পরিবর্তন" else "Change Profile Photo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Avatar Live Preview
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(CardMateTealPrimary.copy(alpha = 0.15f))
                    .border(2.5.dp, CardMateTealPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!previewPhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = previewPhotoUri,
                        contentDescription = "Avatar Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = CardMateTealPrimary,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action Buttons (Gallery & Camera)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Phone Gallery Button
                Button(
                    onClick = {
                        try {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } catch (e: Exception) {
                            fallbackGalleryLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardMateTealPrimary,
                        contentColor = Color(0xFF042F2E)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "ফোন গ্যালারি" else "Phone Gallery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // 2. Camera Button
                OutlinedButton(
                    onClick = {
                        val hasCameraPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPerm) {
                            cameraLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CardMateCyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "ক্যামেরা তুলুন" else "Take Camera",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Designer Avatar Presets
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (isBangla) "অথবা তৈরি অবতার পছন্দ করুন:" else "Or choose a preset avatar:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sampleAvatars) { avatarUrl ->
                            val isSelected = previewPhotoUri == avatarUrl
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) CardMateTealPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        previewPhotoUri = avatarUrl
                                        onPhotoSelected(avatarUrl)
                                        onDismiss()
                                    }
                            ) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Web URL or Logo Input Section
            if (isUrlSectionExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        placeholder = { Text("https://example.com/photo.jpg", fontSize = 12.sp) },
                        label = { Text(if (isBangla) "ছবির ওয়েব লিঙ্ক / URL" else "Web Image URL") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CardMateTealPrimary
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customUrlInput.isNotBlank()) {
                                previewPhotoUri = customUrlInput.trim()
                                onPhotoSelected(customUrlInput.trim())
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF042F2E))
                    }
                }
            } else {
                TextButton(
                    onClick = { isUrlSectionExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla) "অনলাইন ইমেজ লিঙ্ক বা কোম্পানি লোগো URL দিন" else "Enter Image URL or Company Logo Link",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Remove Photo Option
            if (!previewPhotoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        previewPhotoUri = null
                        onPhotoSelected(null)
                        Toast.makeText(
                            context,
                            if (isBangla) "ছবি মুছে ফেলা হয়েছে" else "Photo removed",
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla) "ছবি মুছে ফেলুন (নামের আদ্যক্ষর ব্যবহার করুন)" else "Remove Photo (Use Initials)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
