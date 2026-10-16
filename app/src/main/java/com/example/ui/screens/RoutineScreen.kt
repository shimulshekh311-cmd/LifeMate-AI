package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.I18n
import com.example.data.local.entities.TaskHabitEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TimePickerField
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

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    val filteredList = remember(tasks, selectedTab, todayDateStr) {
        when (selectedTab) {
            1 -> tasks.filter { it.type == "TASK" && (!it.isCompleted || (it.recurrence != "NONE" && it.lastCompletedDate != todayDateStr)) }
            2 -> tasks.filter { it.type == "HABIT" && (!it.isCompleted || it.lastCompletedDate != todayDateStr) }
            3 -> tasks.filter { it.isCompleted || (it.recurrence != "NONE" && it.lastCompletedDate == todayDateStr) }
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
                    message = if (I18n.currentLanguage == "bn") "এখানে কোনো কাজ বা অভ্যাস পাওয়া যায়নি।" else "No items found in this section.",
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
                    items(filteredList, key = { it.id }) { item ->
                        RoutineItemCard(
                            item = item,
                            todayDate = todayDateStr,
                            onToggle = { viewModel.toggleTaskCompletion(item) },
                            onIncrementHabit = { viewModel.incrementHabitProgress(item) },
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
            message = if (I18n.currentLanguage == "bn") "'${itemToDelete?.title}' মুছে ফেলতে চান?" else "Delete '${itemToDelete?.title}'?",
            onConfirm = {
                viewModel.deleteTask(itemToDelete!!.id)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    if (showAddDialog) {
        TaskHabitFormDialog(
            initialItem = null,
            onDismiss = { showAddDialog = false },
            onSave = { updated ->
                viewModel.addTaskHabit(
                    title = updated.title,
                    desc = updated.description,
                    type = updated.type,
                    time = updated.scheduledTime,
                    date = updated.scheduledDate,
                    recurrence = updated.recurrence,
                    durationMinutes = updated.durationMinutes,
                    selectedDays = updated.selectedDays,
                    excludedDays = updated.excludedDays,
                    excludedDates = updated.excludedDates,
                    notificationsEnabled = updated.notificationsEnabled,
                    isAlarm = updated.isAlarm,
                    importance = updated.importance,
                    targetCount = updated.targetCount,
                    endDate = updated.endDate,
                    isOngoing = updated.isOngoing
                )
                showAddDialog = false
            }
        )
    }

    if (itemToEdit != null) {
        TaskHabitFormDialog(
            initialItem = itemToEdit,
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
    todayDate: String,
    onToggle: () -> Unit,
    onIncrementHabit: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    // Check if task occurrence was completed today
    val isCompletedToday = if (item.recurrence != "NONE") {
        item.lastCompletedDate == todayDate && item.isCompleted
    } else {
        item.isCompleted
    }

    // Remaining time calculation while active
    val remainingTimeText = remember(item.scheduledTime, item.durationMinutes) {
        if (item.durationMinutes > 0 && item.scheduledTime.isNotBlank()) {
            try {
                val now = Calendar.getInstance()
                val parsed = SimpleDateFormat("hh:mm a", Locale.US).parse(item.scheduledTime)
                if (parsed != null) {
                    val taskStart = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.YEAR, now.get(Calendar.YEAR))
                        set(Calendar.MONTH, now.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
                    }
                    val startMillis = taskStart.timeInMillis
                    val endMillis = startMillis + (item.durationMinutes * 60 * 1000L)
                    val nowMillis = now.timeInMillis

                    if (nowMillis in startMillis..endMillis) {
                        val remainingMinutes = ((endMillis - nowMillis) / (60 * 1000L)).toInt()
                        if (I18n.currentLanguage == "bn") "⏱️ বাকি সময়: $remainingMinutes মি." else "⏱️ Remaining: ${remainingMinutes}m"
                    } else null
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompletedToday) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (item.importance == "HIGH" && !isCompletedToday)
            androidx.compose.foundation.BorderStroke(1.5.dp, RoseError.copy(alpha = 0.6f))
        else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCompletedToday,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompletedToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        if (item.importance == "HIGH") {
                            Surface(
                                color = RoseError.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (I18n.currentLanguage == "bn") "জরুরি" else "High",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseError,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (item.description.isNotEmpty()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryViolet, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onArchive, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Archive, contentDescription = "Archive", tint = AmberWarning, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError, modifier = Modifier.size(18.dp))
                }
            }

            // Chips Row: Time, Duration, Remaining Time, Recurrence, Excluded Days
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                if (item.durationMinutes > 0) {
                    val durationLabel = if (item.durationMinutes >= 60 && item.durationMinutes % 60 == 0) {
                        "${item.durationMinutes / 60} ${if (I18n.currentLanguage == "bn") "ঘণ্টা" else "hr"}"
                    } else {
                        "${item.durationMinutes} ${if (I18n.currentLanguage == "bn") "মি." else "min"}"
                    }
                    Surface(
                        color = PrimaryViolet.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "⏳ $durationLabel",
                            fontSize = 11.sp,
                            color = PrimaryViolet,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (remainingTimeText != null && !isCompletedToday) {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = remainingTimeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.recurrence != "NONE") {
                    val recurrenceLabel = when (item.recurrence) {
                        "DAILY" -> if (I18n.currentLanguage == "bn") "প্রতিদিন" else "Daily"
                        "WEEKDAYS" -> if (item.selectedDays.isNotBlank()) item.selectedDays else "Weekdays"
                        "CUSTOM_RANGE" -> if (I18n.currentLanguage == "bn") "তারিখ সীমা" else "Date Range"
                        else -> item.recurrence
                    }
                    Surface(
                        color = AmberWarning.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🔁 $recurrenceLabel",
                            fontSize = 11.sp,
                            color = AmberWarning,
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
                            text = "🔥 ${item.streakCount} ${if (I18n.currentLanguage == "bn") "দিন" else "days"}",
                            fontSize = 11.sp,
                            color = PrimaryViolet,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Habit Target Progress Bar and Quick Increment Action
            if (item.type == "HABIT") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progress = if (item.targetCount > 0) {
                        (item.progressCount.toFloat() / item.targetCount).coerceIn(0f, 1f)
                    } else 0f

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (I18n.currentLanguage == "bn")
                                    "দৈনিক লক্ষ্য: ${item.progressCount}/${item.targetCount}"
                                else
                                    "Daily Goal: ${item.progressCount}/${item.targetCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryViolet
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryViolet,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    FilledTonalIconButton(
                        onClick = onIncrementHabit,
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = PrimaryViolet.copy(alpha = 0.15f),
                            contentColor = PrimaryViolet
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add progress", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskHabitFormDialog(
    initialItem: TaskHabitEntity?,
    onDismiss: () -> Unit,
    onSave: (TaskHabitEntity) -> Unit
) {
    val isEdit = initialItem != null
    var type by remember { mutableStateOf(initialItem?.type ?: "TASK") }
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var desc by remember { mutableStateOf(initialItem?.description ?: "") }

    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var startDate by remember { mutableStateOf(initialItem?.scheduledDate?.ifBlank { todayDate } ?: todayDate) }
    var hasEndDate by remember { mutableStateOf(!initialItem?.endDate.isNullOrBlank()) }
    var endDate by remember { mutableStateOf(initialItem?.endDate ?: "") }
    var time by remember { mutableStateOf(initialItem?.scheduledTime?.ifBlank { "08:00 AM" } ?: "08:00 AM") }

    // Duration: value and unit
    val initDurationMin = initialItem?.durationMinutes ?: 0
    val initIsHours = initDurationMin >= 60 && initDurationMin % 60 == 0
    var durationValueStr by remember {
        mutableStateOf(
            if (initDurationMin <= 0) "30"
            else if (initIsHours) (initDurationMin / 60).toString()
            else initDurationMin.toString()
        )
    }
    var durationUnitIsHours by remember { mutableStateOf(initIsHours) }
    var hasDuration by remember { mutableStateOf(initDurationMin > 0) }

    // Recurrence
    var recurrence by remember { mutableStateOf(initialItem?.recurrence ?: if (type == "HABIT") "DAILY" else "NONE") }
    val allWeekdays = listOf("SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI")
    val weekdayLabelsBn = mapOf(
        "SAT" to "শনি", "SUN" to "রবি", "MON" to "সোম", "TUE" to "মঙ্গল",
        "WED" to "বুধ", "THU" to "বৃহঃ", "FRI" to "শুক্র"
    )

    // Selected weekdays for recurring
    val selectedDaysSet = remember {
        mutableStateListOf<String>().apply {
            if (!initialItem?.selectedDays.isNullOrBlank()) {
                addAll(initialItem!!.selectedDays.split(",").map { it.trim().uppercase() })
            }
        }
    }

    // Excluded rest days
    val excludedDaysSet = remember {
        mutableStateListOf<String>().apply {
            if (!initialItem?.excludedDays.isNullOrBlank()) {
                addAll(initialItem!!.excludedDays.split(",").map { it.trim().uppercase() })
            }
        }
    }

    // Specific excluded dates
    val excludedDatesList = remember {
        mutableStateListOf<String>().apply {
            if (!initialItem?.excludedDates.isNullOrBlank()) {
                addAll(initialItem!!.excludedDates.split(",").map { it.trim() })
            }
        }
    }
    var newExcludedDateInput by remember { mutableStateOf("") }

    // Habit Ongoing vs Date Range
    var isOngoingHabit by remember { mutableStateOf(initialItem?.isOngoing ?: true) }
    var targetCountStr by remember { mutableStateOf((initialItem?.targetCount ?: 1).toString()) }

    // Notifications and Alarm
    var notificationsEnabled by remember { mutableStateOf(initialItem?.notificationsEnabled ?: true) }
    var isAlarm by remember { mutableStateOf(initialItem?.isAlarm ?: false) }

    // Importance: Low vs High
    var importance by remember { mutableStateOf(initialItem?.importance ?: "LOW") }

    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) {
                    if (I18n.currentLanguage == "bn") "সম্পাদনা করুন" else "Edit Item"
                } else {
                    if (type == "TASK") (if (I18n.currentLanguage == "bn") "নতুন কাজ (Task) তৈরি" else "Create New Task")
                    else (if (I18n.currentLanguage == "bn") "নতুন অভ্যাস (Habit) শুরু" else "Start New Habit")
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type Switcher (only for new item)
                if (!isEdit) {
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

                // Native Calendar Date Picker for Start Date
                item {
                    DatePickerField(
                        label = if (I18n.currentLanguage == "bn") "শুরুর তারিখ *" else "Start Date *",
                        selectedDate = startDate,
                        onDateSelected = { startDate = it }
                    )
                }

                // Optional End Date / Deadline
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hasEndDate,
                            onCheckedChange = {
                                hasEndDate = it
                                if (it && endDate.isBlank()) endDate = startDate
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (type == "TASK")
                                (if (I18n.currentLanguage == "bn") "নির্দিষ্ট ডেডলাইন / শেষ তারিখ রয়েছে" else "Has a deadline / end date")
                            else
                                (if (I18n.currentLanguage == "bn") "নির্দিষ্ট তারিখ পর্যন্ত চলবে (Date Range)" else "Fixed date range"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (hasEndDate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        DatePickerField(
                            label = if (I18n.currentLanguage == "bn") "শেষের তারিখ / ডেডলাইন" else "End Date / Deadline",
                            selectedDate = endDate,
                            onDateSelected = { endDate = it },
                            minDate = startDate
                        )
                    }
                }

                // Native Clock Time Picker for Start Time
                item {
                    TimePickerField(
                        label = if (I18n.currentLanguage == "bn") "শুরুর সময় *" else "Start Time *",
                        selectedTime = time,
                        onTimeSelected = { time = it }
                    )
                }

                // Optional Duration with Unit (Minutes / Hours)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hasDuration,
                            onCheckedChange = { hasDuration = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (I18n.currentLanguage == "bn") "সময়কাল (Duration) নির্ধারণ করুন" else "Set duration",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (hasDuration) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = durationValueStr,
                                onValueChange = { durationValueStr = it.filter { ch -> ch.isDigit() } },
                                label = { Text(if (I18n.currentLanguage == "bn") "সময়" else "Value") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !durationUnitIsHours,
                                onClick = { durationUnitIsHours = false },
                                label = { Text(if (I18n.currentLanguage == "bn") "মিনিট" else "Min") }
                            )
                            FilterChip(
                                selected = durationUnitIsHours,
                                onClick = { durationUnitIsHours = true },
                                label = { Text(if (I18n.currentLanguage == "bn") "ঘণ্টা" else "Hr") }
                            )
                        }
                    }
                }

                // Recurrence Settings: None, Daily, Selected Weekdays, Custom Range
                item {
                    Text(
                        text = if (I18n.currentLanguage == "bn") "পুনরাবৃত্তি (Recurrence):" else "Recurrence:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val options = listOf(
                            "NONE" to (if (I18n.currentLanguage == "bn") "একবার" else "None"),
                            "DAILY" to (if (I18n.currentLanguage == "bn") "প্রতিদিন" else "Daily"),
                            "WEEKDAYS" to (if (I18n.currentLanguage == "bn") "নির্দিষ্ট দিনসমূহ" else "Weekdays"),
                            "CUSTOM_RANGE" to (if (I18n.currentLanguage == "bn") "তারিখ সীমা" else "Date Range")
                        )
                        items(options) { (key, label) ->
                            FilterChip(
                                selected = recurrence == key,
                                onClick = { recurrence = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Weekday Selector Chips (when recurrence == WEEKDAYS)
                if (recurrence == "WEEKDAYS") {
                    item {
                        Text(
                            text = if (I18n.currentLanguage == "bn") "কোন কোন দিনে চালু থাকবে:" else "Select Weekdays:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(allWeekdays) { dayKey ->
                                val isSelected = selectedDaysSet.contains(dayKey)
                                val label = if (I18n.currentLanguage == "bn") (weekdayLabelsBn[dayKey] ?: dayKey) else dayKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedDaysSet.remove(dayKey)
                                        else selectedDaysSet.add(dayKey)
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Dedicated Schedule / Exception Section (Rest / Excluded Days & Excluded Dates)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = if (I18n.currentLanguage == "bn") "ব্যতিক্রম ও ছুটির দিন (Schedule Exceptions):" else "Schedule Exceptions (Rest Days):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryViolet
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (I18n.currentLanguage == "bn") "যে দিনগুলোতে রিমাইন্ডার ও কাজ বন্ধ থাকবে:" else "Days to skip / suppress reminder:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allWeekdays) { dayKey ->
                            val isExcluded = excludedDaysSet.contains(dayKey)
                            val label = if (I18n.currentLanguage == "bn") (weekdayLabelsBn[dayKey] ?: dayKey) else dayKey
                            FilterChip(
                                selected = isExcluded,
                                onClick = {
                                    if (isExcluded) excludedDaysSet.remove(dayKey)
                                    else excludedDaysSet.add(dayKey)
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoseError.copy(alpha = 0.2f),
                                    selectedLabelColor = RoseError
                                )
                            )
                        }
                    }
                }

                // Specific Excluded Dates Picker & Chip list
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            DatePickerField(
                                label = if (I18n.currentLanguage == "bn") "ছুটির নির্দিষ্ট তারিখ যোগ করুন" else "Add Specific Skip Date",
                                selectedDate = newExcludedDateInput,
                                onDateSelected = {
                                    if (it.isNotBlank() && !excludedDatesList.contains(it)) {
                                        excludedDatesList.add(it)
                                    }
                                    newExcludedDateInput = ""
                                }
                            )
                        }
                    }
                    if (excludedDatesList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(excludedDatesList) { skipDate ->
                                SuggestionChip(
                                    onClick = { excludedDatesList.remove(skipDate) },
                                    label = { Text("✕ $skipDate", fontSize = 11.sp, color = RoseError) }
                                )
                            }
                        }
                    }
                }

                // Habit Specific Target Count
                if (type == "HABIT") {
                    item {
                        OutlinedTextField(
                            value = targetCountStr,
                            onValueChange = { targetCountStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text(if (I18n.currentLanguage == "bn") "দৈনিক সম্পন্ন করার লক্ষ্য (Target Count)" else "Daily Target Count") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Importance Selector (Strictly Low vs High)
                item {
                    Text(
                        text = if (I18n.currentLanguage == "bn") "কাজের গুরুত্ব (Importance):" else "Importance:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = importance == "LOW",
                            onClick = { importance = "LOW" },
                            label = { Text(if (I18n.currentLanguage == "bn") "Low (সাধারণ)" else "Low") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = importance == "HIGH",
                            onClick = { importance = "HIGH" },
                            label = { Text(if (I18n.currentLanguage == "bn") "High (জরুরি)" else "High") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoseError.copy(alpha = 0.2f),
                                selectedLabelColor = RoseError
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Notifications & Alarm Switches
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (I18n.currentLanguage == "bn") "রিমাইন্ডার নোটিফিকেশন" else "Reminder Notification",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (I18n.currentLanguage == "bn") "অ্যালার্ম টোন বাজান" else "Sound Alarm",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = isAlarm,
                            onCheckedChange = { isAlarm = it }
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

                // Explicit Schedule Summary Box before saving
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (I18n.currentLanguage == "bn") "📋 সময়সূচি সারসংক্ষেপ (Schedule Summary):" else "📋 Schedule Summary:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PrimaryViolet
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val durText = if (hasDuration) {
                                val v = durationValueStr.ifBlank { "0" }
                                if (durationUnitIsHours) "$v hr" else "$v min"
                            } else (if (I18n.currentLanguage == "bn") "নির্ধারিত নেই" else "None")

                            val recurText = when (recurrence) {
                                "NONE" -> if (I18n.currentLanguage == "bn") "একবার (নির্দিষ্ট তারিখ)" else "Once"
                                "DAILY" -> if (I18n.currentLanguage == "bn") "প্রতিদিন" else "Daily"
                                "WEEKDAYS" -> if (selectedDaysSet.isNotEmpty()) {
                                    val days = selectedDaysSet.joinToString(", ") { if (I18n.currentLanguage == "bn") (weekdayLabelsBn[it] ?: it) else it }
                                    if (I18n.currentLanguage == "bn") "বার: $days" else "Days: $days"
                                } else (if (I18n.currentLanguage == "bn") "বার নির্বাচন করা হয়নি" else "No weekdays selected")
                                "CUSTOM_RANGE" -> if (I18n.currentLanguage == "bn") "তারিখ সীমা: $startDate থেকে $endDate" else "Date range: $startDate to $endDate"
                                else -> recurrence
                            }

                            val restText = if (excludedDaysSet.isNotEmpty()) {
                                excludedDaysSet.joinToString(", ") { if (I18n.currentLanguage == "bn") (weekdayLabelsBn[it] ?: it) else it }
                            } else (if (I18n.currentLanguage == "bn") "নেই" else "None")

                            val skipDatesText = if (excludedDatesList.isNotEmpty()) {
                                excludedDatesList.joinToString(", ")
                            } else (if (I18n.currentLanguage == "bn") "নেই" else "None")

                            val reminderText = when {
                                notificationsEnabled && isAlarm -> if (I18n.currentLanguage == "bn") "নোটিফিকেশন ও অ্যালার্ম চালু" else "Notification & Alarm ON"
                                notificationsEnabled -> if (I18n.currentLanguage == "bn") "শুধু নোটিফিকেশন চালু" else "Notification Only"
                                else -> if (I18n.currentLanguage == "bn") "বন্ধ" else "Disabled"
                            }

                            Text(
                                text = "• " + (if (I18n.currentLanguage == "bn") "তারিখ ও সময়: $startDate $time" else "Time: $startDate at $time"),
                                fontSize = 11.sp
                            )
                            if (hasEndDate && endDate.isNotBlank()) {
                                Text(
                                    text = "• " + (if (I18n.currentLanguage == "bn") "মেয়াদ শেষ: $endDate" else "Deadline: $endDate"),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "• " + (if (I18n.currentLanguage == "bn") "সময়কাল: $durText" else "Duration: $durText"),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• " + (if (I18n.currentLanguage == "bn") "পুনরাবৃত্তি: $recurText" else "Recurrence: $recurText"),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• " + (if (I18n.currentLanguage == "bn") "ছুটির বার: $restText" else "Rest Days: $restText"),
                                fontSize = 11.sp
                            )
                            if (excludedDatesList.isNotEmpty()) {
                                Text(
                                    text = "• " + (if (I18n.currentLanguage == "bn") "ছুটির তারিখ: $skipDatesText" else "Excluded Dates: $skipDatesText"),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "• " + (if (I18n.currentLanguage == "bn") "রিমাইন্ডার: $reminderText" else "Alert: $reminderText"),
                                fontSize = 11.sp
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
                        val durationMinutes = if (hasDuration) {
                            val parsedVal = durationValueStr.toIntOrNull() ?: 30
                            if (durationUnitIsHours) parsedVal * 60 else parsedVal
                        } else 0

                        val finalSelectedDays = if (recurrence == "WEEKDAYS") selectedDaysSet.joinToString(",") else ""
                        val finalExcludedDays = excludedDaysSet.joinToString(",")
                        val finalExcludedDates = excludedDatesList.joinToString(",")
                        val finalTarget = targetCountStr.toIntOrNull() ?: 1

                        val result = (initialItem ?: TaskHabitEntity(title = "", type = type)).copy(
                            title = title.trim(),
                            description = desc.trim(),
                            type = type,
                            scheduledDate = startDate.trim(),
                            endDate = if (hasEndDate) endDate.trim() else "",
                            scheduledTime = time.trim(),
                            durationMinutes = durationMinutes,
                            recurrence = recurrence,
                            selectedDays = finalSelectedDays,
                            excludedDays = finalExcludedDays,
                            excludedDates = finalExcludedDates,
                            notificationsEnabled = notificationsEnabled,
                            isAlarm = isAlarm,
                            importance = importance,
                            targetCount = finalTarget,
                            isOngoing = isOngoingHabit
                        )
                        onSave(result)
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
