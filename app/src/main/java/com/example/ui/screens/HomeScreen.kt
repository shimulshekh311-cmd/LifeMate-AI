package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.I18n
import com.example.data.local.entities.TaskHabitEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GlassCard
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val plans by viewModel.exercisePlans.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val jobStats by viewModel.jobStats.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> I18n.t("greeting_morning")
        hour < 17 -> I18n.t("greeting_afternoon")
        else -> I18n.t("greeting_evening")
    }

    val todayDateStr = SimpleDateFormat(
        if (I18n.currentLanguage == "bn") "EEEE, d MMMM yyyy" else "EEEE, MMMM d, yyyy",
        Locale.getDefault()
    ).format(Date())

    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(BigDecimal.valueOf(t.amount)) }
            .setScale(2, RoundingMode.HALF_EVEN)
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(BigDecimal.valueOf(t.amount)) }
            .setScale(2, RoundingMode.HALF_EVEN)
    }
    val netBalance = remember(totalIncome, totalExpense) {
        totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_EVEN)
    }

    val pendingTasks = tasks.filter { !it.isCompleted }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Companion Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PrimaryViolet, CyberCyan)),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(PrimaryViolet, CyberCyan))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Companion",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = todayDateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${userPrefs?.assistantName ?: "LifeMate"} আপনার সাথে রয়েছে।",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleAssistant(true) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryViolet)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.AddTask,
                    label = I18n.t("quick_add_task"),
                    color = PrimaryViolet,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(ScreenTab.ROUTINE) }
                )
                QuickActionButton(
                    icon = Icons.Default.FitnessCenter,
                    label = I18n.t("start_workout_now"),
                    color = EmeraldSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(ScreenTab.EXERCISE) }
                )
                QuickActionButton(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = I18n.t("quick_add_transaction"),
                    color = CyberCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(ScreenTab.FINANCE) }
                )
            }
        }

        // Overview Metric Cards
        item {
            Text(
                text = I18n.t("today_overview"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = I18n.t("upcoming_tasks"),
                    value = "${pendingTasks.size}",
                    icon = Icons.Default.Checklist,
                    accentColor = PrimaryViolet,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = I18n.t("balance_summary"),
                    value = "৳${String.format(Locale.getDefault(), "%,.0f", netBalance.toDouble())}",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = if (netBalance >= BigDecimal.ZERO) EmeraldSuccess else RoseError,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = I18n.t("stat_total_exams"),
                    value = "${jobStats.totalExams}",
                    icon = Icons.Default.Work,
                    accentColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = I18n.t("active_plan"),
                    value = "${plans.size} টি প্ল্যান",
                    icon = Icons.Default.FitnessCenter,
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Today's Pending Tasks List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = I18n.t("upcoming_tasks"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.setTab(ScreenTab.ROUTINE) }) {
                    Text("সব দেখুন")
                }
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.DoneAll,
                    message = I18n.t("no_tasks_today")
                )
            }
        } else {
            items(pendingTasks.take(4)) { task ->
                HomeTaskItem(
                    task = task,
                    onToggle = { viewModel.toggleTaskCompletion(task) }
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
        }
    }
}

@Composable
fun HomeTaskItem(
    task: TaskHabitEntity,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (task.scheduledTime.isNotEmpty()) {
                    Text(
                        text = "⏰ ${task.scheduledTime}",
                        fontSize = 11.sp,
                        color = CyberCyan
                    )
                }
            }
        }
    }
}
