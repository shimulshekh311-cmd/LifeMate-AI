package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.core.i18n.I18n
import com.example.core.security.BiometricAuthManager
import com.example.core.security.BiometricAuthResult
import com.example.core.security.BiometricCapabilityStatus
import kotlinx.coroutines.delay
import com.example.data.local.entities.ExamStageEntity
import com.example.data.local.entities.JobApplicationEntity
import com.example.data.repository.StageSummary
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JobApplicationsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val categorizedJobs by viewModel.categorizedJobs.collectAsState()
    val jobStats by viewModel.jobStats.collectAsState()
    val allStages by viewModel.allExamStages.collectAsState()

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    // Categories according to exact precedence:
    // 0: Cancelled, 1: Failed, 2: Completed, 3: Ongoing, 4: Upcoming
    val categoryTitles = listOf(
        I18n.t("cat_cancelled") + " (${categorizedJobs.cancelled.size})",
        I18n.t("cat_failed") + " (${categorizedJobs.failed.size})",
        I18n.t("cat_completed") + " (${categorizedJobs.completed.size})",
        I18n.t("cat_ongoing") + " (${categorizedJobs.ongoing.size})",
        I18n.t("cat_upcoming") + " (${categorizedJobs.upcoming.size})"
    )

    var showAddDialog by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<JobApplicationEntity?>(null) }
    var editingStage by remember { mutableStateOf<ExamStageEntity?>(null) }
    var viewingAppDetails by remember { mutableStateOf<JobApplicationEntity?>(null) }
    var appToDelete by remember { mutableStateOf<JobApplicationEntity?>(null) }

    val currentList = when (selectedCategoryIndex) {
        0 -> categorizedJobs.cancelled
        1 -> categorizedJobs.failed
        2 -> categorizedJobs.completed
        3 -> categorizedJobs.ongoing
        else -> categorizedJobs.upcoming
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryViolet,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = I18n.t("add_job_application"))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dashboard Header: 8 Summary Cards
            item {
                Text(
                    text = "চাকরির পরীক্ষার সার্বিক ড্যাশবোর্ড",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Row 1: The 4 Exam Stages Summaries
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StageDashboardSummaryCard(
                        title = I18n.t("stage_mcq"),
                        summary = jobStats.mcqSummary,
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StageDashboardSummaryCard(
                        title = I18n.t("stage_written"),
                        summary = jobStats.writtenSummary,
                        accentColor = PrimaryViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StageDashboardSummaryCard(
                        title = I18n.t("stage_typing"),
                        summary = jobStats.typingSummary,
                        accentColor = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                    StageDashboardSummaryCard(
                        title = I18n.t("stage_viva"),
                        summary = jobStats.vivaSummary,
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Totals Cards (Total Apps, Total Exams = Apps - Cancelled, Failed, Cancelled)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallMetricCard(
                        title = I18n.t("stat_total_apps"),
                        value = "${jobStats.totalApplications}",
                        color = PrimaryViolet,
                        modifier = Modifier.weight(1f)
                    )
                    SmallMetricCard(
                        title = I18n.t("stat_total_exams"),
                        value = "${jobStats.totalExams}",
                        color = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    SmallMetricCard(
                        title = I18n.t("stat_failed_total"),
                        value = "${jobStats.totalFailedApplications}",
                        color = RoseError,
                        modifier = Modifier.weight(1f)
                    )
                    SmallMetricCard(
                        title = I18n.t("stat_cancelled_total"),
                        value = "${jobStats.totalCancelledApplications}",
                        color = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Category Filter Chips
            item {
                Text(
                    text = "আবেদনের ধাপভিত্তিক শ্রেণীবিন্যাস",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryViolet
                ) {
                    categoryTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            if (currentList.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.WorkOutline,
                        message = "এই বিভাগে কোনো চাকরির আবেদন পাওয়া যায়নি।",
                        actionButton = {
                            Button(onClick = { showAddDialog = true }) {
                                Text(I18n.t("add_job_application"))
                            }
                        }
                    )
                }
            } else {
                items(currentList) { app ->
                    val appStages = allStages.filter { it.applicationId == app.id }
                    JobApplicationCard(
                        app = app,
                        stages = appStages,
                        viewModel = viewModel,
                        onStageClick = { stage -> editingStage = stage },
                        onEdit = { editingApp = app },
                        onViewDetails = { viewingAppDetails = app },
                        onDelete = { appToDelete = app }
                    )
                }
            }
        }
    }

    if (appToDelete != null) {
        ConfirmDeleteDialog(
            title = I18n.t("delete"),
            message = "'${appToDelete?.organizationName} (${appToDelete?.positionName})' আবেদনটি মুছে ফেলতে চান?",
            onConfirm = {
                viewModel.deleteJobApplication(appToDelete!!.id)
                appToDelete = null
            },
            onDismiss = { appToDelete = null }
        )
    }

    if (showAddDialog) {
        CreateJobApplicationDialog(
            onDismiss = { showAddDialog = false },
            onSave = { org, pos, uid, pass, appPdf, admitPdf, cancelled, unsuccessful ->
                viewModel.addJobApplication(org, pos, uid, pass, appPdf, admitPdf, cancelled, unsuccessful)
                showAddDialog = false
            }
        )
    }

    if (editingApp != null) {
        EditJobApplicationDialog(
            app = editingApp!!,
            onDismiss = { editingApp = null },
            onSave = { updated, newPass ->
                viewModel.updateJobApplication(updated, newPass)
                editingApp = null
            }
        )
    }

    if (editingStage != null) {
        EditStageDialog(
            stage = editingStage!!,
            onDismiss = { editingStage = null },
            onSave = { updated ->
                viewModel.updateExamStage(updated)
                editingStage = null
            }
        )
    }

    if (viewingAppDetails != null) {
        val app = viewingAppDetails!!
        val appStages = allStages.filter { it.applicationId == app.id }
        JobDetailsDialog(
            app = app,
            stages = appStages,
            viewModel = viewModel,
            onDismiss = { viewingAppDetails = null }
        )
    }
}

@Composable
fun SmallMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun StageDashboardSummaryCard(
    title: String,
    summary: StageSummary,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "উত্তীর্ণ: ${summary.passedCount}",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldSuccess
            )
            Text(
                text = "ব্যর্থ: ${summary.failedCount}",
                style = MaterialTheme.typography.labelSmall,
                color = RoseError
            )
            Text(
                text = "বাতিল: ${summary.cancelledCount}",
                style = MaterialTheme.typography.labelSmall,
                color = AmberWarning
            )
        }
    }
}

@Composable
fun JobApplicationCard(
    app: JobApplicationEntity,
    stages: List<ExamStageEntity>,
    viewModel: MainViewModel,
    onStageClick: (ExamStageEntity) -> Unit,
    onEdit: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.organizationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.positionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberCyan
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app.isCancelled) {
                        StatusBadge(text = I18n.t("cat_cancelled"), color = AmberWarning)
                    } else if (app.isUnsuccessful) {
                        StatusBadge(text = I18n.t("cat_failed"), color = RoseError)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryViolet)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "তারিখ: ${app.creationDate} (${app.weekday})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stages.forEach { stage ->
                    val badgeColor = when {
                        stage.outcome.equals("PASSED", ignoreCase = true) -> EmeraldSuccess
                        stage.outcome.equals("FAILED", ignoreCase = true) -> RoseError
                        stage.isCompleted -> CyberCyan
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onStageClick(stage) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stage.stageType}: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                            Text(
                                text = if (stage.isCompleted) stage.outcome else I18n.t("stage_not_completed"),
                                fontSize = 11.sp,
                                color = badgeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CreateJobApplicationDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, Boolean, Boolean) -> Unit
) {
    var orgName by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var rawPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var appPdf by remember { mutableStateOf("") }
    var admitPdf by remember { mutableStateOf("") }
    var isCancelled by remember { mutableStateOf(false) }
    var isUnsuccessful by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("add_job_application"), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = orgName,
                        onValueChange = { orgName = it },
                        label = { Text(I18n.t("org_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = position,
                        onValueChange = { position = it },
                        label = { Text(I18n.t("position_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text(I18n.t("app_user_id")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = rawPassword,
                        onValueChange = { rawPassword = it },
                        label = { Text(I18n.t("app_password")) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        supportingText = { Text(I18n.t("password_vault_info"), fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = appPdf,
                        onValueChange = { appPdf = it },
                        label = { Text(I18n.t("app_pdf")) },
                        placeholder = { Text("/documents/application.pdf") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = admitPdf,
                        onValueChange = { admitPdf = it },
                        label = { Text(I18n.t("admit_pdf")) },
                        placeholder = { Text("/documents/admit_card.pdf") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isCancelled, onCheckedChange = { isCancelled = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(I18n.t("is_cancelled"))
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isUnsuccessful, onCheckedChange = { isUnsuccessful = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(I18n.t("is_unsuccessful"))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (orgName.isNotBlank() && position.isNotBlank()) {
                        onSave(orgName, position, userId, rawPassword, appPdf, admitPdf, isCancelled, isUnsuccessful)
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
fun EditJobApplicationDialog(
    app: JobApplicationEntity,
    onDismiss: () -> Unit,
    onSave: (JobApplicationEntity, String?) -> Unit
) {
    var orgName by remember { mutableStateOf(app.organizationName) }
    var position by remember { mutableStateOf(app.positionName) }
    var userId by remember { mutableStateOf(app.userId) }
    var newPassword by remember { mutableStateOf("") }
    var appPdf by remember { mutableStateOf(app.applicationPdfPath) }
    var admitPdf by remember { mutableStateOf(app.admitCardPdfPath) }
    var isCancelled by remember { mutableStateOf(app.isCancelled) }
    var isUnsuccessful by remember { mutableStateOf(app.isUnsuccessful) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("চাকরির আবেদন সম্পাদনা", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = orgName,
                        onValueChange = { orgName = it },
                        label = { Text(I18n.t("org_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = position,
                        onValueChange = { position = it },
                        label = { Text(I18n.t("position_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text(I18n.t("app_user_id")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("নতুন পাসওয়ার্ড (পরিবর্তন না করলে খালি রাখুন)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = appPdf,
                        onValueChange = { appPdf = it },
                        label = { Text(I18n.t("app_pdf")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = admitPdf,
                        onValueChange = { admitPdf = it },
                        label = { Text(I18n.t("admit_pdf")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isCancelled, onCheckedChange = { isCancelled = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(I18n.t("is_cancelled"))
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isUnsuccessful, onCheckedChange = { isUnsuccessful = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(I18n.t("is_unsuccessful"))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (orgName.isNotBlank() && position.isNotBlank()) {
                        val updated = app.copy(
                            organizationName = orgName,
                            positionName = position,
                            userId = userId,
                            applicationPdfPath = appPdf,
                            admitCardPdfPath = admitPdf,
                            isCancelled = isCancelled,
                            isUnsuccessful = isUnsuccessful
                        )
                        val passToSave = if (newPassword.isNotBlank()) newPassword else null
                        onSave(updated, passToSave)
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
fun EditStageDialog(
    stage: ExamStageEntity,
    onDismiss: () -> Unit,
    onSave: (ExamStageEntity) -> Unit
) {
    var isCompleted by remember { mutableStateOf(stage.isCompleted) }
    var performance by remember { mutableStateOf(stage.performance) }
    var outcome by remember { mutableStateOf(stage.outcome) }
    var examDate by remember {
        mutableStateOf(
            if (stage.examDate.isNotEmpty()) stage.examDate
            else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
    }
    var notes by remember { mutableStateOf(stage.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stage.stageType} ${I18n.t("update_stage")}", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(I18n.t("stage_status"))
                        FilterChip(
                            selected = isCompleted,
                            onClick = { isCompleted = !isCompleted },
                            label = { Text(if (isCompleted) I18n.t("stage_completed") else I18n.t("stage_not_completed")) }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = examDate,
                        onValueChange = { examDate = it },
                        label = { Text(I18n.t("exam_date")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(I18n.t("performance"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(selected = performance == "GOOD", onClick = { performance = "GOOD" }, label = { Text(I18n.t("perf_good")) })
                        FilterChip(selected = performance == "MEDIUM", onClick = { performance = "MEDIUM" }, label = { Text(I18n.t("perf_medium")) })
                        FilterChip(selected = performance == "BAD", onClick = { performance = "BAD" }, label = { Text(I18n.t("perf_bad")) })
                    }
                }

                item {
                    Text(I18n.t("outcome"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(selected = outcome == "PENDING", onClick = { outcome = "PENDING" }, label = { Text(I18n.t("out_pending")) })
                        FilterChip(selected = outcome == "PASSED", onClick = { outcome = "PASSED" }, label = { Text(I18n.t("out_passed")) })
                        FilterChip(selected = outcome == "FAILED", onClick = { outcome = "FAILED" }, label = { Text(I18n.t("out_failed")) })
                        FilterChip(selected = outcome == "NOT_ANNOUNCED", onClick = { outcome = "NOT_ANNOUNCED" }, label = { Text(I18n.t("out_not_announced")) })
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("নোট") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        stage.copy(
                            isCompleted = isCompleted,
                            performance = performance,
                            outcome = outcome,
                            examDate = examDate,
                            notes = notes
                        )
                    )
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
fun JobDetailsDialog(
    app: JobApplicationEntity,
    stages: List<ExamStageEntity>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var revealedPassword by remember { mutableStateOf<String?>(null) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var copyNotice by remember { mutableStateOf(false) }

    // Auto-mask after 30 seconds of inactivity to protect against shoulder-surfing
    LaunchedEffect(revealedPassword) {
        if (revealedPassword != null) {
            delay(30_000L)
            revealedPassword = null
        }
    }

    // Clean up memory when dialog closes or leaves composition
    DisposableEffect(Unit) {
        onDispose {
            revealedPassword = null
            authErrorMessage = null
            copyNotice = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.organizationName, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = "পদ: ${app.positionName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberCyan
                    )
                    Text("আবেদনের তারিখ: ${app.creationDate} (${app.weekday})")
                }

                if (app.userId.isNotEmpty()) {
                    item {
                        Text("ইউজার আইডি: ${app.userId}", fontWeight = FontWeight.Medium)
                    }
                }

                if (app.encryptedPassword.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("পাসওয়ার্ড (Biometric Vault):", fontSize = 11.sp, color = CyberCyan)
                                        Text(
                                            text = revealedPassword ?: "••••••••",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.testTag("displayed_password_text")
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (revealedPassword != null) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(revealedPassword!!))
                                                    copyNotice = true
                                                },
                                                modifier = Modifier.testTag("copy_password_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = I18n.t("copy_password"),
                                                    modifier = Modifier.size(18.dp),
                                                    tint = CyberCyan
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = {
                                                if (revealedPassword != null) {
                                                    // Explicit hide
                                                    revealedPassword = null
                                                    authErrorMessage = null
                                                    copyNotice = false
                                                } else {
                                                    authErrorMessage = null
                                                    val activity = context as? FragmentActivity
                                                    if (activity != null) {
                                                        BiometricAuthManager.authenticate(
                                                            activity = activity,
                                                            title = I18n.t("biometric_prompt_title"),
                                                            subtitle = I18n.t("biometric_prompt_subtitle")
                                                        ) { result ->
                                                            when (result) {
                                                                is BiometricAuthResult.Success -> {
                                                                    revealedPassword = viewModel.decryptPassword(app.encryptedPassword)
                                                                    authErrorMessage = null
                                                                }
                                                                is BiometricAuthResult.Cancelled -> {
                                                                    revealedPassword = null
                                                                    authErrorMessage = I18n.t("biometric_auth_cancelled")
                                                                }
                                                                is BiometricAuthResult.Error -> {
                                                                    revealedPassword = null
                                                                    authErrorMessage = result.message
                                                                }
                                                                is BiometricAuthResult.Unavailable -> {
                                                                    revealedPassword = null
                                                                    authErrorMessage = result.message
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        // Direct capability check when activity context is not a FragmentActivity
                                                        val status = BiometricAuthManager.checkCapability(context)
                                                        if (status == BiometricCapabilityStatus.READY) {
                                                            revealedPassword = viewModel.decryptPassword(app.encryptedPassword)
                                                        } else {
                                                            authErrorMessage = BiometricAuthManager.getCapabilityMessage(status)
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("toggle_password_reveal_btn")
                                        ) {
                                            Text(if (revealedPassword == null) I18n.t("reveal_password") else I18n.t("hide_password"))
                                        }
                                    }
                                }

                                if (copyNotice) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = I18n.t("password_copied"),
                                        fontSize = 11.sp,
                                        color = EmeraldSuccess
                                    )
                                }

                                if (authErrorMessage != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = RoseError.copy(alpha = 0.15f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = RoseError,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = authErrorMessage!!,
                                                color = RoseError,
                                                fontSize = 12.sp,
                                                modifier = Modifier.testTag("auth_error_message")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (app.applicationPdfPath.isNotEmpty()) {
                    item {
                        Text("📁 ${I18n.t("app_pdf")}: ${app.applicationPdfPath}")
                    }
                }

                if (app.admitCardPdfPath.isNotEmpty()) {
                    item {
                        Text("🎫 ${I18n.t("admit_pdf")}: ${app.admitCardPdfPath}")
                    }
                }

                item {
                    HorizontalDivider()
                    Text("পরীক্ষার ধাপসমূহ (৪টি ধাপ):", fontWeight = FontWeight.Bold)
                }

                items(stages) { stage ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${stage.stageType}: ${if (stage.isCompleted) stage.outcome else I18n.t("stage_not_completed")}",
                                fontWeight = FontWeight.Bold,
                                color = if (stage.outcome.equals("PASSED", ignoreCase = true)) EmeraldSuccess else CyberCyan
                            )
                            if (stage.examDate.isNotEmpty()) {
                                Text("তারিখ: ${stage.examDate}", fontSize = 11.sp)
                            }
                            if (stage.performance != "NONE") {
                                Text("পারফরম্যান্স: ${stage.performance}", fontSize = 11.sp)
                            }
                            if (stage.notes.isNotEmpty()) {
                                Text("নোট: ${stage.notes}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("ঠিক আছে") }
        }
    )
}
