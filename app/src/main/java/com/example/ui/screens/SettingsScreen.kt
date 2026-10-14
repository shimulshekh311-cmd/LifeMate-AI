package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.I18n
import com.example.data.backup.BackupPreview
import com.example.data.backup.RestoreConflictPolicy
import com.example.data.local.entities.UserPreferencesEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.BackupRestoreUiState
import com.example.ui.viewmodel.MainViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPrefs by viewModel.userPreferences.collectAsState()
    val backupState by viewModel.backupRestoreState.collectAsState()

    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    var selectedRestorePolicy by remember { mutableStateOf(RestoreConflictPolicy.MERGE_EXISTING) }

    // Storage Access Framework - Document Creation Picker (Safe native export)
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingBackupJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(pendingBackupJson!!.toByteArray(Charsets.UTF_8))
                }
                val preview = viewModel.repository.backupRestoreManager.inspectBackupJson(pendingBackupJson!!)
                viewModel.onBackupSavedSuccess(preview.totalRecords)
            } catch (e: Exception) {
                viewModel.prepareBackupFile { /* no-op */ }
            } finally {
                pendingBackupJson = null
            }
        } else {
            pendingBackupJson = null
            viewModel.onRestoreCancelled()
        }
    }

    // Storage Access Framework - Document Open Picker (Safe native import)
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line).append('\n')
                            line = reader.readLine()
                        }
                    }
                }
                val content = stringBuilder.toString()
                viewModel.onBackupFileSelected(content)
            } catch (e: Exception) {
                viewModel.onRestoreCancelled()
            }
        } else {
            viewModel.onRestoreCancelled()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Language & Display
        item {
            Text(
                text = I18n.t("assistant_settings"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = I18n.t("language_choice"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (I18n.currentLanguage == "bn") "বাংলা (Bangla)" else "English",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            val newLang = if (I18n.currentLanguage == "bn") "en" else "bn"
                            viewModel.setLanguage(newLang)
                        },
                        modifier = Modifier.testTag("toggle_language_btn")
                    ) {
                        Text(if (I18n.currentLanguage == "bn") "Switch to English" else "বাংলায় পরিবর্তন")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = I18n.t("religion_toggle"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = I18n.t("religion_toggle_info"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userPrefs?.isReligionModuleEnabled ?: true,
                        onCheckedChange = { isEnabled ->
                            userPrefs?.let {
                                viewModel.saveUserPreferences(it.copy(isReligionModuleEnabled = isEnabled))
                            }
                        },
                        modifier = Modifier.testTag("toggle_religion_switch")
                    )
                }
            }
        }

        // 2. Data Management & Backup/Restore
        item {
            Text(
                text = I18n.t("backup_restore_title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(
                borderColor = PrimaryViolet.copy(alpha = 0.4f)
            ) {
                // Backup Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.prepareBackupFile { json ->
                                pendingBackupJson = json
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                createDocLauncher.launch("lifemate_backup_$timestamp.json")
                            }
                        }
                        .padding(vertical = 8.dp)
                        .testTag("create_backup_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Backup",
                            tint = PrimaryViolet
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = I18n.t("backup_now"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = I18n.t("backup_subtitle"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Restore Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        }
                        .padding(vertical = 8.dp)
                        .testTag("restore_backup_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Restore",
                            tint = EmeraldSuccess
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = I18n.t("restore_from_file"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = I18n.t("restore_subtitle"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Privacy & Security Note
        item {
            Text(
                text = I18n.t("privacy_security"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(
                borderColor = CyberCyan.copy(alpha = 0.35f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = I18n.t("cloud_sync_local_only"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = I18n.t("cloud_sync_info"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // --- State Handlers & Dialogs ---
    when (val state = backupState) {
        is BackupRestoreUiState.InProgress -> {
            AlertDialog(
                onDismissRequest = { /* non-cancellable while in progress */ },
                title = { Text(I18n.t("backup_restore_title")) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = PrimaryViolet
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {}
            )
        }

        is BackupRestoreUiState.PreviewReady -> {
            RestorePreviewDialog(
                preview = state.preview,
                selectedPolicy = selectedRestorePolicy,
                onPolicyChange = { selectedRestorePolicy = it },
                onConfirm = {
                    viewModel.executeRestore(state.jsonContent, selectedRestorePolicy)
                },
                onDismiss = {
                    viewModel.resetBackupRestoreState()
                }
            )
        }

        is BackupRestoreUiState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupRestoreState() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = { Text(I18n.t("confirm")) },
                text = {
                    Column {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        if (state.warnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = I18n.t("attachment_warning_title"),
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = I18n.t("attachment_warning_msg"),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetBackupRestoreState() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Text("OK")
                    }
                }
            )
        }

        is BackupRestoreUiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupRestoreState() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = RoseError,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = { Text(I18n.t("settings_title")) },
                text = { Text(state.message, color = MaterialTheme.colorScheme.error) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetBackupRestoreState() },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                    ) {
                        Text(I18n.t("btn_dismiss"))
                    }
                }
            )
        }

        is BackupRestoreUiState.Cancelled -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupRestoreState() },
                title = { Text(I18n.t("backup_restore_title")) },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetBackupRestoreState() }) {
                        Text(I18n.t("btn_dismiss"))
                    }
                }
            )
        }

        BackupRestoreUiState.Idle -> { /* Idle */ }
    }
}

@Composable
fun RestorePreviewDialog(
    preview: BackupPreview,
    selectedPolicy: RestoreConflictPolicy,
    onPolicyChange: (RestoreConflictPolicy) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = I18n.t("restore_preview_header"),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(I18n.t("backup_date_label"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(preview.exportDate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(I18n.t("backup_version_label"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("v${preview.schemaVersion}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(I18n.t("total_records_label"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${preview.totalRecords}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = I18n.t("restore_mode_label"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Policy 1: Merge
                item {
                    Surface(
                        onClick = { onPolicyChange(RestoreConflictPolicy.MERGE_EXISTING) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedPolicy == RestoreConflictPolicy.MERGE_EXISTING)
                            PrimaryViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedPolicy == RestoreConflictPolicy.MERGE_EXISTING) PrimaryViolet else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPolicy == RestoreConflictPolicy.MERGE_EXISTING,
                                onClick = { onPolicyChange(RestoreConflictPolicy.MERGE_EXISTING) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(I18n.t("mode_merge"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(I18n.t("mode_merge_desc"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Policy 2: Replace Local Data
                item {
                    Surface(
                        onClick = { onPolicyChange(RestoreConflictPolicy.REPLACE_LOCAL) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedPolicy == RestoreConflictPolicy.REPLACE_LOCAL)
                            RoseError.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedPolicy == RestoreConflictPolicy.REPLACE_LOCAL) RoseError else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPolicy == RestoreConflictPolicy.REPLACE_LOCAL,
                                onClick = { onPolicyChange(RestoreConflictPolicy.REPLACE_LOCAL) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(I18n.t("mode_replace"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoseError)
                                Text(I18n.t("mode_replace_desc"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedPolicy == RestoreConflictPolicy.REPLACE_LOCAL) RoseError else PrimaryViolet
                ),
                modifier = Modifier.testTag("confirm_restore_btn")
            ) {
                Text(I18n.t("btn_start_restore"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(I18n.t("btn_dismiss"))
            }
        }
    )
}
