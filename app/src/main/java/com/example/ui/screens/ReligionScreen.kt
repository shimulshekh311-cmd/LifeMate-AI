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
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ReligionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activities by viewModel.religiousActivities.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
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
            onSave = { title, time, notes ->
                viewModel.addReligiousActivity(title, time, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ReligiousItemCard(
    activity: ReligiousActivityEntity,
    onToggle: () -> Unit,
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
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
            }
        }
    }
}

@Composable
fun AddReligiousActivityDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("add_observance"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(I18n.t("observance_title")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("সময় (যেমন: 05:00 AM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(I18n.t("observance_notes")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onSave(title, time, notes)
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
