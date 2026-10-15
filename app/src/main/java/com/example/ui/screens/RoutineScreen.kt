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
import com.example.data.local.entities.TaskHabitEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun RoutineScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Tasks, 2: Habits, 3: Completed
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<TaskHabitEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<TaskHabitEntity?>(null) }

    val filteredList = remember(tasks, selectedTab) {
        when (selectedTab) {
            1 -> tasks.filter { it.type == "TASK" && !it.isCompleted }
            2 -> tasks.filter { it.type == "HABIT" && !it.isCompleted }
            3 -> tasks.filter { it.isCompleted }
            else -> tasks
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryViolet,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = I18n.t("add_item"))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryViolet
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(I18n.t("tab_all")) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(I18n.t("tab_tasks")) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(I18n.t("tab_habits")) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text(I18n.t("tab_completed")) })
            }

            if (filteredList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Checklist,
                    message = "এখানে কোনো কাজ বা অভ্যাস পাওয়া যায়নি।",
                    actionButton = {
                        Button(onClick = { showAddDialog = true }) {
                            Text(I18n.t("add_item"))
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { item ->
                        RoutineItemCard(
                            item = item,
                            onToggle = { viewModel.toggleTaskCompletion(item) },
                            onEdit = { itemToEdit = item },
                            onArchive = { viewModel.archiveTaskHabit(item.id) },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    if (itemToDelete != null) {
        ConfirmDeleteDialog(
            title = I18n.t("delete"),
            message = "'${itemToDelete?.title}' মুছে ফেলতে চান?",
            onConfirm = {
                viewModel.deleteTask(itemToDelete!!.id)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    if (showAddDialog) {
        AddTaskHabitDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, type, time, date, recurrence ->
                viewModel.addTaskHabit(title, desc, type, time, date, recurrence)
                showAddDialog = false
            }
        )
    }

    if (itemToEdit != null) {
        EditTaskHabitDialog(
            item = itemToEdit!!,
            onDismiss = { itemToEdit = null },
            onSave = { updated ->
                viewModel.updateTaskHabit(updated)
                itemToEdit = null
            }
        )
    }
}

@Composable
fun RoutineItemCard(
    item: TaskHabitEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.scheduledTime.isNotEmpty()) {
                        Surface(
                            color = CyberCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⏰ ${item.scheduledTime}",
                                fontSize = 11.sp,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (item.type == "HABIT") {
                        Surface(
                            color = PrimaryViolet.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔥 ${item.streakCount} দিন টানা",
                                fontSize = 11.sp,
                                color = PrimaryViolet,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryViolet)
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Default.Archive, contentDescription = "Archive", tint = AmberWarning)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
            }
        }
    }
}

@Composable
fun AddTaskHabitDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("TASK") }

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val tomorrowDate = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    var selectedDate by remember { mutableStateOf(todayDate) }
    var time by remember { mutableStateOf("08:00 AM") }
    var recurrence by remember { mutableStateOf("NONE") }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (type == "TASK")
                    (if (I18n.currentLanguage == "bn") "নতুন কাজ (Task) তৈরি" else "Create New Task")
                else
                    (if (I18n.currentLanguage == "bn") "নতুন অভ্যাস (Habit) শুরু" else "Start New Habit"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type Switcher
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = type == "TASK",
                            onClick = {
                                type = "TASK"
                                recurrence = "NONE"
                            },
                            label = { Text(I18n.t("type_task")) },
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = type == "HABIT",
                            onClick = {
                                type = "HABIT"
                                recurrence = "DAILY"
                            },
                            label = { Text(I18n.t("type_habit")) },
                            leadingIcon = {
                                Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = {
                            Text(
                                if (type == "TASK") (if (I18n.currentLanguage == "bn") "টাস্কের নাম *" else "Task Name *")
                                else (if (I18n.currentLanguage == "bn") "অভ্যাসের নাম *" else "Habit Name *")
                            )
                        },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text(if (I18n.currentLanguage == "bn") "নাম প্রদান করা বাধ্যতামূলক" else "Title is required", color = RoseError) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Date Presets & Field
                item {
                    Text(
                        text = if (I18n.currentLanguage == "bn") "তারিখ:" else "Scheduled Date:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedDate == todayDate,
                            onClick = { selectedDate = todayDate },
                            label = { Text(if (I18n.currentLanguage == "bn") "আজকে" else "Today") }
                        )
                        FilterChip(
                            selected = selectedDate == tomorrowDate,
                            onClick = { selectedDate = tomorrowDate },
                            label = { Text(if (I18n.currentLanguage == "bn") "কালকে" else "Tomorrow") }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Time Presets & Field
                item {
                    Text(
                        text = if (I18n.currentLanguage == "bn") "সময় ও রিমাইন্ডার:" else "Time & Reminder:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("08:00 AM", "01:00 PM", "06:00 PM", "10:00 PM").forEach { quickTime ->
                            FilterChip(
                                selected = time == quickTime,
                                onClick = { time = quickTime },
                                label = { Text(quickTime, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text(I18n.t("due_time")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Recurrence Selector
                item {
                    Text(
                        text = if (I18n.currentLanguage == "bn") "পুনরাবৃত্তি:" else "Recurrence:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = recurrence == "NONE",
                            onClick = { recurrence = "NONE" },
                            label = { Text(if (I18n.currentLanguage == "bn") "একবার" else "Once") }
                        )
                        FilterChip(
                            selected = recurrence == "DAILY",
                            onClick = { recurrence = "DAILY" },
                            label = { Text(if (I18n.currentLanguage == "bn") "প্রতিদিন" else "Daily") }
                        )
                        FilterChip(
                            selected = recurrence == "WEEKLY",
                            onClick = { recurrence = "WEEKLY" },
                            label = { Text(if (I18n.currentLanguage == "bn") "সাপ্তাহিক" else "Weekly") }
                        )
                    }
                }

                // Description
                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(I18n.t("item_desc")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Notification Alarm Cue
                item {
                    Surface(
                        color = PrimaryViolet.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = PrimaryViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (I18n.currentLanguage == "bn")
                                    "নির্ধারিত সময়ে স্বয়ংক্রিয় নোটিফিকেশন অ্যালার্ম পাঠানো হবে।"
                                else
                                    "A reminder notification alarm will trigger at the scheduled time.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(title.trim(), desc.trim(), type, time.trim(), selectedDate.trim(), recurrence)
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
fun EditTaskHabitDialog(
    item: TaskHabitEntity,
    onDismiss: () -> Unit,
    onSave: (TaskHabitEntity) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var desc by remember { mutableStateOf(item.description) }
    var time by remember { mutableStateOf(item.scheduledTime) }
    var date by remember { mutableStateOf(item.scheduledDate) }
    var recurrence by remember { mutableStateOf(item.recurrence) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (I18n.currentLanguage == "bn") "কাজ/অভ্যাস সম্পাদনা" else "Edit Task / Habit",
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
                        label = { Text(I18n.t("item_title")) },
                        isError = titleError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("তারিখ (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text(I18n.t("due_time")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = recurrence == "NONE",
                            onClick = { recurrence = "NONE" },
                            label = { Text(if (I18n.currentLanguage == "bn") "একবার" else "Once") }
                        )
                        FilterChip(
                            selected = recurrence == "DAILY",
                            onClick = { recurrence = "DAILY" },
                            label = { Text(if (I18n.currentLanguage == "bn") "প্রতিদিন" else "Daily") }
                        )
                        FilterChip(
                            selected = recurrence == "WEEKLY",
                            onClick = { recurrence = "WEEKLY" },
                            label = { Text(if (I18n.currentLanguage == "bn") "সাপ্তাহিক" else "Weekly") }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(I18n.t("item_desc")) },
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
                        onSave(
                            item.copy(
                                title = title.trim(),
                                description = desc.trim(),
                                scheduledTime = time.trim(),
                                scheduledDate = date.trim(),
                                recurrence = recurrence
                            )
                        )
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
