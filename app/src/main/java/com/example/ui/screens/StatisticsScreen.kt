package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.I18n
import com.example.ui.components.GlassCard
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val history by viewModel.exerciseHistory.collectAsState()
    val chapters by viewModel.studyChapters.collectAsState()
    val jobStats by viewModel.jobStats.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val completedTasks = tasks.count { it.isCompleted }
    val totalTasks = tasks.size
    val taskRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks * 100).toInt() else 0

    val completedChapters = chapters.count { it.isCompleted }
    val totalChapters = chapters.size
    val studyRate = if (totalChapters > 0) (completedChapters.toFloat() / totalChapters * 100).toInt() else 0

    val totalWorkoutSeconds = history.sumOf { it.totalSeconds }
    val totalWorkoutMinutes = totalWorkoutSeconds / 60

    // Decimal-safe calculations
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = I18n.t("analytics_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "আপনার সংরক্ষিত কার্যকলাপের সমন্বিত রিপোর্ট",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Productivity & Routine
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = I18n.t("productivity_overview"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$completedTasks/$totalTasks ($taskRate%)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryViolet
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryViolet,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("মোট নির্ধারিত কাজ: $totalTasks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("সম্পন্ন কাজ: $completedTasks", fontSize = 12.sp, color = EmeraldSuccess)
                }
            }
        }

        // Section 2: Health & Workout
        item {
            GlassCard {
                Text(
                    text = I18n.t("health_overview"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "মোট ব্যায়াম সেশন",
                        value = "${history.size}",
                        icon = Icons.Default.FitnessCenter,
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "ব্যায়ামের সময়কাল",
                        value = "$totalWorkoutMinutes মিনিট",
                        icon = Icons.Default.Timer,
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 3: Study Progress
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = I18n.t("study_overview"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$completedChapters/$totalChapters ($studyRate%)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (totalChapters > 0) completedChapters.toFloat() / totalChapters else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = CyberCyan,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "পড়া বাকি অধ্যায়: ${totalChapters - completedChapters} টি",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 4: Job Application Pipeline
        item {
            GlassCard {
                Text(
                    text = "চাকরির পরীক্ষার সাফল্য",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StageStatPill("MCQ", jobStats.mcqSummary.passedCount, jobStats.mcqSummary.failedCount)
                    StageStatPill("লিখিত", jobStats.writtenSummary.passedCount, jobStats.writtenSummary.failedCount)
                    StageStatPill("টাইপিং", jobStats.typingSummary.passedCount, jobStats.typingSummary.failedCount)
                    StageStatPill("ভাইভা", jobStats.vivaSummary.passedCount, jobStats.vivaSummary.failedCount)
                }
            }
        }

        // Section 5: Money & Savings
        item {
            GlassCard {
                Text(
                    text = I18n.t("money_overview"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "মোট আয়",
                        value = "৳${String.format(Locale.getDefault(), "%,.2f", totalIncome.toDouble())}",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "মোট ব্যয়",
                        value = "৳${String.format(Locale.getDefault(), "%,.2f", totalExpense.toDouble())}",
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        accentColor = RoseError,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StageStatPill(name: String, passed: Int, failed: Int) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.width(72.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CyberCyan)
            Text("✅ $passed", fontSize = 10.sp, color = EmeraldSuccess)
            Text("❌ $failed", fontSize = 10.sp, color = RoseError)
        }
    }
}
