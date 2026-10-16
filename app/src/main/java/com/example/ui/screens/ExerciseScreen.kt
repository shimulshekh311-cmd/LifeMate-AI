package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.entities.ExerciseItemEntity
import com.example.data.local.entities.ExercisePlanEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExerciseScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val plans by viewModel.exercisePlans.collectAsState()
    val history by viewModel.exerciseHistory.collectAsState()
    val sessionState by viewModel.guidedSession.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<ExercisePlanEntity?>(null) }
    var selectedPlanForSession by remember { mutableStateOf<ExercisePlanEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (!sessionState.isActive) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = EmeraldSuccess,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = I18n.t("create_exercise_plan"))
                }
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
            // Active Guided Workout Session (if active)
            if (sessionState.isActive) {
                item {
                    GuidedSessionActiveCard(
                        viewModel = viewModel
                    )
                }
            }

            // Exercise Plans Section
            item {
                Text(
                    text = I18n.t("exercise_plans"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (plans.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.FitnessCenter,
                        message = I18n.t("no_plan_active"),
                        actionButton = {
                            Button(onClick = { showCreateDialog = true }) {
                                Text(I18n.t("create_exercise_plan"))
                            }
                        }
                    )
                }
            } else {
                items(plans) { plan ->
                    val planItems by viewModel.repository.getExerciseItems(plan.id).collectAsState(initial = emptyList())
                    ExercisePlanCard(
                        plan = plan,
                        items = planItems,
                        onStartSession = {
                            viewModel.startGuidedSession(plan, planItems)
                        },
                        onDelete = { planToDelete = plan }
                    )
                }
            }

            // Exercise History Section
            if (history.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = I18n.t("exercise_history"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(history.take(5)) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.planTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.completedTimestamp))
                                Text(
                                    text = "$dateStr • ${item.completedRounds} রাউন্ড সম্পন্ন",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${item.totalSeconds} সেকেন্ড",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (planToDelete != null) {
        ConfirmDeleteDialog(
            title = I18n.t("delete"),
            message = "'${planToDelete?.title}' ব্যায়াম পরিকল্পনাটি মুছে ফেলতে চান?",
            onConfirm = {
                viewModel.deleteExercisePlan(planToDelete!!.id)
                planToDelete = null
            },
            onDismiss = { planToDelete = null }
        )
    }

    if (showCreateDialog) {
        CreateExercisePlanDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, desc, restSec, rounds, startDate, endDate, startTime, notifEnabled, items ->
                viewModel.createExercisePlan(title, desc, restSec, rounds, startDate, endDate, startTime, notifEnabled, items)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun ExercisePlanCard(
    plan: ExercisePlanEntity,
    items: List<ExerciseItemEntity>,
    onStartSession: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (plan.description.isNotEmpty()) {
                        Text(
                            text = plan.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = PrimaryViolet.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "রাউন্ড: ${plan.rounds}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryViolet,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = CyberCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "বিরতি: ${plan.restDurationSeconds}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${items.size} টি ব্যায়াম",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldSuccess,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEachIndexed { idx, itm ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${idx + 1}. ${itm.name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (itm.durationSeconds > 0) "(${itm.durationSeconds}s)" else "(${itm.reps} reps)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onStartSession,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("গাইডেড সেশন শুরু করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GuidedSessionActiveCard(
    viewModel: MainViewModel
) {
    val session by viewModel.guidedSession.collectAsState()
    val currentItem = session.items.getOrNull(session.currentItemIndex)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                2.dp,
                if (session.isResting) CyberCyan else EmeraldSuccess,
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PrimaryViolet.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "রাউন্ড ${session.currentRound} / ${session.plan?.rounds ?: 1}",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryViolet,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (session.isResting) CyberCyan.copy(alpha = 0.2f) else EmeraldSuccess.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (session.isResting) "😴 বিশ্রামের সময়" else "💪 ব্যায়াম চলছে",
                        fontWeight = FontWeight.Bold,
                        color = if (session.isResting) CyberCyan else EmeraldSuccess,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Timer Countdown Display
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.isResting) CyberCyan.copy(alpha = 0.15f) else EmeraldSuccess.copy(alpha = 0.15f)
                    )
                    .border(
                        4.dp,
                        if (session.isResting) CyberCyan else EmeraldSuccess,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${session.remainingSeconds}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "সেকেন্ড",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (session.isResting) "পরবর্তী: ${session.items.getOrNull(session.currentItemIndex + 1)?.name ?: "পরবর্তী রাউন্ড"}"
                else currentItem?.name ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (currentItem != null && currentItem.reps > 0 && !session.isResting) {
                Text(
                    text = "লক্ষ্য: ${currentItem.reps} Reps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Control Buttons: Pause/Resume, Skip, Finish
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { viewModel.pauseResumeGuidedSession() }) {
                    Icon(
                        imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (session.isPaused) I18n.t("resume") else I18n.t("pause"))
                }

                Button(
                    onClick = { viewModel.skipGuidedExercise() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(I18n.t("skip"))
                }

                OutlinedButton(
                    onClick = { viewModel.finishGuidedSession(interrupted = true) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(I18n.t("finish"))
                }
            }
        }
    }
}

@Composable
fun CreateExercisePlanDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int, String, String, String, Boolean, List<ExerciseItemEntity>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var restSecStr by remember { mutableStateOf("15") }
    var roundsStr by remember { mutableStateOf("2") }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var startDate by remember { mutableStateOf(todayStr) }
    var hasEndDate by remember { mutableStateOf(false) }
    var endDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("07:00 AM") }
    var notificationsEnabled by remember { mutableStateOf(true) }

    var itemName by remember { mutableStateOf("") }
    var itemDurationStr by remember { mutableStateOf("30") }
    var itemRepsStr by remember { mutableStateOf("0") }
    var titleError by remember { mutableStateOf(false) }

    val itemsList = remember { mutableStateListOf<ExerciseItemEntity>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("create_exercise_plan"), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text(I18n.t("plan_title") + " *") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text(if (I18n.currentLanguage == "bn") "পরিকল্পনার নাম বাধ্যতামূলক" else "Plan title is required", color = RoseError) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    DatePickerField(
                        label = if (I18n.currentLanguage == "bn") "শুরুর তারিখ (Start Date) *" else "Start Date *",
                        selectedDate = startDate,
                        onDateSelected = { startDate = it }
                    )
                }
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
                            text = if (I18n.currentLanguage == "bn") "নির্দিষ্ট মেয়াদ / শেষের তারিখ রয়েছে" else "Has an end date",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (hasEndDate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        DatePickerField(
                            label = if (I18n.currentLanguage == "bn") "শেষের তারিখ (End Date)" else "End Date",
                            selectedDate = endDate,
                            onDateSelected = { endDate = it },
                            minDate = startDate
                        )
                    }
                }
                item {
                    TimePickerField(
                        label = if (I18n.currentLanguage == "bn") "ব্যায়াম শুরুর সময় *" else "Start Time *",
                        selectedTime = startTime,
                        onTimeSelected = { startTime = it }
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
                                text = if (I18n.currentLanguage == "bn") "ব্যায়ামের রিমাইন্ডার অ্যালার্ম" else "Workout Reminder Alarm",
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
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(if (I18n.currentLanguage == "bn") "বিবরণ (ঐচ্ছিক)" else "Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(
                        text = if (I18n.currentLanguage == "bn") "রাউন্ড ও বিশ্রামের সময়:" else "Rounds & Rest:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("1", "2", "3", "4").forEach { r ->
                            FilterChip(
                                selected = roundsStr == r,
                                onClick = { roundsStr = r },
                                label = { Text(if (I18n.currentLanguage == "bn") "$r রাউন্ড" else "$r Rds", fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("15", "30", "45", "60").forEach { sec ->
                            FilterChip(
                                selected = restSecStr == sec,
                                onClick = { restSecStr = sec },
                                label = { Text(if (I18n.currentLanguage == "bn") "$sec সে. বিশ্রাম" else "$sec s rest", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (I18n.currentLanguage == "bn") "ব্যায়ামসমূহ যোগ করুন:" else "Add Exercises:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Quick Suggestions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("পুশ-আপ", "স্কোয়াট", "প্ল্যাঙ্ক", "লাঞ্জ").forEach { suggestion ->
                            SuggestionChip(
                                onClick = { itemName = suggestion },
                                label = { Text(suggestion, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text(I18n.t("exercise_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = itemDurationStr,
                            onValueChange = { itemDurationStr = it },
                            label = { Text(if (I18n.currentLanguage == "bn") "সময় (সেকেন্ড)" else "Seconds") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = itemRepsStr,
                            onValueChange = { itemRepsStr = it },
                            label = { Text("Reps") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            if (itemName.isNotBlank()) {
                                itemsList.add(
                                    ExerciseItemEntity(
                                        planId = 0,
                                        name = itemName.trim(),
                                        durationSeconds = itemDurationStr.toIntOrNull() ?: 30,
                                        reps = itemRepsStr.toIntOrNull() ?: 0,
                                        orderIndex = itemsList.size
                                    )
                                )
                                itemName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(I18n.t("add_exercise"))
                    }
                }

                if (itemsList.isNotEmpty()) {
                    items(itemsList) { itm ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${itm.name} (${if (itm.durationSeconds > 0) "${itm.durationSeconds}s" else "${itm.reps} reps"})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                IconButton(onClick = { itemsList.remove(itm) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = RoseError, modifier = Modifier.size(16.dp))
                                }
                            }
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
                        val rest = restSecStr.toIntOrNull() ?: 15
                        val rounds = roundsStr.toIntOrNull() ?: 1
                        val finalItems = if (itemsList.isEmpty()) {
                            listOf(
                                ExerciseItemEntity(planId = 0, name = "স্ট্রেচিং", durationSeconds = 30),
                                ExerciseItemEntity(planId = 0, name = "পুশ-আপ", reps = 10),
                                ExerciseItemEntity(planId = 0, name = "স্কোয়াট", reps = 15)
                            )
                        } else itemsList.toList()
                        onSave(
                            title.trim(),
                            desc.trim(),
                            rest,
                            rounds,
                            startDate.trim(),
                            if (hasEndDate) endDate.trim() else "",
                            startTime.trim(),
                            notificationsEnabled,
                            finalItems
                        )
                    }
                }
            ) {
                Text(I18n.t("save_plan"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(I18n.t("cancel")) }
        }
    )
}
