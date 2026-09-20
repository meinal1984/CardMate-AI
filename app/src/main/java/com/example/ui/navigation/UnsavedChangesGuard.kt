package com.example.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.theme.StatusWarning

/**
 * Reusable Composable hook and dialog for preventing accidental loss of unsaved changes.
 * Automatically registers with BackNavigationService so Android edge swipe, system back,
 * and header back buttons will show this discard confirmation before leaving the screen.
 */
@Composable
fun UnsavedChangesGuard(
    hasUnsavedChanges: () -> Boolean,
    isBangla: Boolean = false,
    onSaveAndExit: (() -> Unit)? = null,
    onDiscardConfirmed: () -> Unit = {}
) {
    val navService = BackNavigationService.instance

    DisposableEffect(Unit) {
        val unregister = navService.registerUnsavedChangesGuard(
            hasChanges = hasUnsavedChanges,
            onDiscard = onDiscardConfirmed
        )
        onDispose {
            unregister()
        }
    }

    if (navService.showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { navService.dismissUnsavedChangesDialog() },
            modifier = Modifier.testTag("unsaved_changes_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isBangla) "অসংরক্ষিত পরিবর্তন বাতিল করবেন?" else "Discard unsaved changes?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "আপনার ফর্মে কিছু পরিবর্তন করা হয়েছে যা সংরক্ষণ করা হয়নি। আপনি কি পরিবর্তনগুলো বাতিল করে ফিরে যেতে চান?"
                    else
                        "You have unsaved changes in this form. Leaving will discard all unsaved edits.",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSaveAndExit != null) {
                        Button(
                            onClick = {
                                navService.dismissUnsavedChangesDialog()
                                onSaveAndExit()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardMateTealPrimary),
                            modifier = Modifier.testTag("save_and_exit_btn")
                        ) {
                            Text(
                                text = if (isBangla) "সংরক্ষণ করুন" else "Save & Exit",
                                color = Color(0xFF042F2E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            navService.confirmDiscardUnsavedChanges()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("discard_changes_btn")
                    ) {
                        Text(
                            text = if (isBangla) "বাতিল করুন" else "Discard",
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { navService.dismissUnsavedChangesDialog() },
                    modifier = Modifier.testTag("keep_editing_btn")
                ) {
                    Text(
                        text = if (isBangla) "এডিট করতে থাকুন" else "Keep Editing",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }
}
