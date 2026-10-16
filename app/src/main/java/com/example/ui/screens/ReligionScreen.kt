package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.I18n
import com.example.data.local.entities.ReligiousActivityEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GlassCard
import com.example.ui.components.TimePickerField
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ReligionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activities by viewModel.religiousActivities.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ReligiousActivityEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ReligiousActivityEntity?>(null) }

    val completedCount = activities.count { it.isCompletedToday }
    val totalCount = activities.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldSuccess,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = I18n.t("add_observance"))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Progress Card
            item {
                GlassCard(
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = I18n.t("religion_module_title"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$completedCount/$totalCount",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = EmeraldSuccess,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = I18n.t("religion_toggle_info"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // Checklist Header
            item {
                Text(
                    text = I18n.t("daily_checklist"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activities.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.FavoriteBorder,
                        message = "কোনো আমল বা প্রার্থনা তালিকাভুক্ত নেই।",
                        actionButton = {
                            Button(onClick = { showAddDialog = true }) {
                                Text(I18n.t("add_observance"))
                            }
                        }
                    )
                }
            } else {
                items(activities) { activity ->
                    ReligiousItemCard(
                        activity = activity,
                        onToggle = { viewModel.toggleReligiousActivity(activity) },
                        onEdit = { itemToEdit = activity },
                        onDelete = { itemToDelete = activity }
                    )
                }
            }
        }
    }

    if (itemToDelete != null) {
        ConfirmDeleteDialog(
            title = I18n.t("delete"),
            message = "'${itemToDelete?.title}' মুছে ফেলতে চান?",
            onConfirm = {
                viewModel.deleteReligiousActivity(itemToDelete!!.id)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    if (showAddDialog) {
        AddReligiousActivityDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, time, notes, notif ->
                viewModel.addReligiousActivity(title, time, notes)
                showAddDialog = false
            }
        )
    }

    if (itemToEdit != null) {
        EditReligiousActivityDialog(
            activity = itemToEdit!!,
            onDismiss = { itemToEdit = null },
            onSave = { updated ->
                viewModel.updateReligiousActivity(updated)
                itemToEdit = null
            }
        )
    }
}

@Composable
fun ReligiousItemCard(
    activity: ReligiousActivityEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = activity.isCompletedToday,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (activity.isCompletedToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (activity.scheduledTime.isNotEmpty()) {
                    Text(
                        text = "⏰ ${activity.scheduledTime}",
                        fontSize = 11.sp,
                        color = EmeraldSuccess
                    )
                }
                if (activity.notes.isNotEmpty()) {
                    Text(
                        text = activity.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryViolet, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddReligiousActivityDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("05:00 AM") }
    var notes by remember { mutableStateOf("") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var titleError by remember { mutableStateOf(false) }

    val prayerPresets = listOf(
        Pair("ফজর", "05:00 AM"),
        Pair("যোহর", "01:15 PM"),
        Pair("আসর", "04:30 PM"),
        Pair("মাগরিব", "06:00 PM"),
        Pair("এশা", "08:00 PM"),
        Pair("তাহাজ্জুদ", "04:00 AM"),
        Pair("কুরআন তিলাওয়াত", "06:30 AM"),
        Pair("জিকির ও দু\'আ", "07:00 AM")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("add_observance"), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = if (I18n.currentLanguage == "bn") "দ্রুত নির্বাচন (ওয়াক্ত/আমল):" else "Quick Presets:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        prayerPresets.take(4).forEach { (presetName, presetTime) ->
                            SuggestionChip(
                                onClick = {
                                    title = presetName
                                    time = presetTime
                                    titleError = false
                                },
                                label = { Text(presetName, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        prayerPresets.drop(4).forEach { (presetName, presetTime) ->
                            SuggestionChip(
                                onClick = {
                                    title = presetName
                                    time = presetTime
                                    titleError = false
                                },
                                label = { Text(presetName, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text(I18n.t("observance_title") + " *") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text(if (I18n.currentLanguage == "bn") "নাম প্রদান করা বাধ্যতামূলক" else "Title is required", color = RoseError) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    TimePickerField(
                        label = if (I18n.currentLanguage == "bn") "আমল/প্রার্থনার সময় *" else "Observance Time *",
                        selectedTime = time,
                        onTimeSelected = { time = it }
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (I18n.currentLanguage == "bn") "ওয়াক্ত/আমলের রিমাইন্ডার অ্যালার্ম" else "Reminder Alarm",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(I18n.t("observance_notes")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(title.trim(), time.trim(), notes.trim(), notificationsEnabled)
                    }
                }
            ) {
                Text(I18n.t("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(I18n.t("cancel")) }
        }
    )
}

@Composable
fun EditReligiousActivityDialog(
    activity: ReligiousActivityEntity,
    onDismiss: () -> Unit,
    onSave: (ReligiousActivityEntity) -> Unit
) {
    var title by remember { mutableStateOf(activity.title) }
    var time by remember { mutableStateOf(activity.scheduledTime) }
    var notes by remember { mutableStateOf(activity.notes) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (I18n.currentLanguage == "bn") "আমল/প্রার্থনা সম্পাদনা" else "Edit Observance",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text(I18n.t("observance_title")) },
                        isError = titleError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    TimePickerField(
                        label = if (I18n.currentLanguage == "bn") "আমল/প্রার্থনার সময় *" else "Observance Time *",
                        selectedTime = time,
                        onTimeSelected = { time = it }
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (I18n.currentLanguage == "bn") "রিমাইন্ডার নোটিফিকেশন" else "Reminder Notification",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = activity.notificationsEnabled,
                            onCheckedChange = { onSave(activity.copy(notificationsEnabled = it)) }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(I18n.t("observance_notes")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(activity.copy(title = title.trim(), scheduledTime = time.trim(), notes = notes.trim()))
                    }
                }
            ) {
                Text(I18n.t("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(I18n.t("cancel")) }
        }
    )
}
