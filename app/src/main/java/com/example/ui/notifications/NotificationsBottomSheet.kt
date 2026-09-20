package com.example.ui.notifications

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.example.ui.theme.CardMateCyanAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.viewmodel.CardViewModel
import com.example.ui.viewmodel.NavigationTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    viewModel: CardViewModel,
    onDismiss: () -> Unit,
    onNavigateToTab: (NavigationTab) -> Unit,
    onOpenCard: (Long) -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "UNREAD" -> notifications.filter { !it.isRead }
            "BACKUP_SYNC" -> notifications.filter { it.type == NotificationType.BACKUP_COMPLETED || it.type == NotificationType.CONTACT_SYNCED }
            "CARDS" -> notifications.filter { it.type == NotificationType.CARD_SCANNED || it.type == NotificationType.NFC_READY }
            "REMINDERS" -> notifications.filter { it.type == NotificationType.REMINDER_FOLLOW_UP || it.type == NotificationType.TIPS_TRICKS }
            else -> notifications
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .testTag("notifications_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardMateTealPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = CardMateTealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isBangla) "নোটিফিকেশন সেন্টার" else "Notification Center",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBangla) {
                                if (unreadCount > 0) "$unreadCount টি অপঠিত নোটিফিকেশন" else "সব নোটিফিকেশন পঠিত"
                            } else {
                                if (unreadCount > 0) "$unreadCount unread alerts" else "All caught up"
                            },
                            fontSize = 11.sp,
                            color = if (unreadCount > 0) CardMateCyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { viewModel.markAllNotificationsAsRead() },
                            modifier = Modifier.testTag("mark_all_read_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CardMateTealPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "সব পঠিত" else "Mark all read",
                                fontSize = 11.sp,
                                color = CardMateTealPrimary
                            )
                        }
                    }

                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllNotifications() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear all",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Quick Filter Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    Triple("ALL", if (isBangla) "সকল (${notifications.size})" else "All (${notifications.size})", Icons.Default.Notifications),
                    Triple("UNREAD", if (isBangla) "অপঠিত ($unreadCount)" else "Unread ($unreadCount)", Icons.Default.NotificationsActive),
                    Triple("BACKUP_SYNC", if (isBangla) "ব্যাকআপ ও সিঙ্ক" else "Backup & Sync", Icons.Default.Sync),
                    Triple("CARDS", if (isBangla) "কার্ড ও স্ক্যান" else "Cards & Scan", Icons.Default.CreditCard),
                    Triple("REMINDERS", if (isBangla) "রিমাইন্ডার ও টিপস" else "Reminders", Icons.Default.Info)
                )

                items(filters) { (id, label, icon) ->
                    val isSelected = selectedFilter == id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CardMateTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = id }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Notifications List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBangla) "কোনো নোটিফিকেশন নেই" else "No notifications found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.triggerSampleNotification() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAlert,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "নমুনা নোটিফিকেশন টেস্ট করুন" else "Trigger Sample Notification",
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        NotificationItemCard(
                            item = item,
                            isBangla = isBangla,
                            onItemClick = {
                                viewModel.markAsRead(item.id)
                                when {
                                    item.targetCardId != null -> {
                                        onDismiss()
                                        onOpenCard(item.targetCardId)
                                    }
                                    item.targetTabName != null -> {
                                        onDismiss()
                                        val tab = runCatching { NavigationTab.valueOf(item.targetTabName) }.getOrNull()
                                        if (tab != null) onNavigateToTab(tab)
                                    }
                                }
                            },
                            onDelete = { viewModel.deleteNotification(item.id) }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { viewModel.triggerSampleNotification() }
                            ) {
                                Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "+ টেস্ট নোটিফিকেশন পাঠান" else "+ Send Test Notification",
                                    fontSize = 11.5.sp,
                                    color = CardMateTealPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    item: AppNotification,
    isBangla: Boolean,
    onItemClick: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = getNotificationIcon(item.type)
    val iconBg = getNotificationColor(item.type)
    val timeFormatted = formatTimestamp(item.timestamp, isBangla)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onItemClick() }
            .then(
                if (!item.isRead) {
                    Modifier.border(
                        width = 1.dp,
                        color = CardMateTealPrimary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) item.titleBn else item.titleEn,
                        fontSize = 13.5.sp,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CardMateTealPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isBangla) item.messageBn else item.messageEn,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                if (item.targetTabName != null || item.targetCardId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val actionLabel = if (isBangla) {
                            when (item.type) {
                                NotificationType.CARD_SCANNED -> "কার্ডটি দেখুন →"
                                NotificationType.BACKUP_COMPLETED -> "ব্যাকআপ স্ট্যাটাস →"
                                NotificationType.CONTACT_SYNCED -> "কন্টাক্ট লিস্ট দেখুন →"
                                NotificationType.NFC_READY -> "এনএফসি কার্ড দেখুন →"
                                NotificationType.REMINDER_FOLLOW_UP -> "ফলো-আপ শুরু করুন →"
                                else -> "বিস্তারিত দেখুন →"
                            }
                        } else {
                            when (item.type) {
                                NotificationType.CARD_SCANNED -> "View Card →"
                                NotificationType.BACKUP_COMPLETED -> "View Backup →"
                                NotificationType.CONTACT_SYNCED -> "Open Contacts →"
                                NotificationType.NFC_READY -> "Open NFC Card →"
                                NotificationType.REMINDER_FOLLOW_UP -> "Take Action →"
                                else -> "Open Details →"
                            }
                        }

                        Text(
                            text = actionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CardMateTealPrimary
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(28.dp)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete notification",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.CARD_SCANNED -> Icons.Default.CreditCard
        NotificationType.BACKUP_COMPLETED -> Icons.Default.CloudDone
        NotificationType.CONTACT_SYNCED -> Icons.Default.Sync
        NotificationType.NFC_READY -> Icons.Default.Nfc
        NotificationType.REMINDER_FOLLOW_UP -> Icons.Default.AddAlert
        NotificationType.TIPS_TRICKS -> Icons.Default.Info
        NotificationType.SYSTEM_UPDATE -> Icons.Default.Notifications
    }
}

private fun getNotificationColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.CARD_SCANNED -> CardMateTealPrimary
        NotificationType.BACKUP_COMPLETED -> Color(0xFF10B981)
        NotificationType.CONTACT_SYNCED -> Color(0xFF06B6D4)
        NotificationType.NFC_READY -> Color(0xFF8B5CF6)
        NotificationType.REMINDER_FOLLOW_UP -> Color(0xFFF59E0B)
        NotificationType.TIPS_TRICKS -> Color(0xFF3B82F6)
        NotificationType.SYSTEM_UPDATE -> CardMateTealPrimary
    }
}

private fun formatTimestamp(timestamp: Long, isBangla: Boolean): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return if (isBangla) {
        when {
            minutes < 1 -> "এখনই"
            minutes < 60 -> "$minutes মি. আগে"
            hours < 24 -> "$hours ঘণ্টা আগে"
            days == 1L -> "গতকাল"
            else -> {
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn", "BD"))
                sdf.format(Date(timestamp))
            }
        }
    } else {
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            else -> {
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
                sdf.format(Date(timestamp))
            }
        }
    }
}
