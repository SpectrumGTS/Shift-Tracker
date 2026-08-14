package dev.spectrumgts.shifttracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import dev.spectrumgts.shifttracker.ui.BackupImportActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogBodyStyle
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogTitleStyle

@Composable
fun BackupRestoreScreen(
    shifts: List<ShiftLog>,
    onExportUnifiedBackup: (Uri, Boolean, Boolean) -> Unit,
    onRedoOnboarding: () -> Unit = {}
) {
    val context = LocalContext.current
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var exportShiftsChecked by remember { mutableStateOf(true) }
    var exportSettingsChecked by remember { mutableStateOf(true) }
    var showRedoOnboardingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val exportUnifiedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                onExportUnifiedBackup(it, exportShiftsChecked, exportSettingsChecked)
                snackbarMessage = context.getString(R.string.backup_export_success_unified)
            } catch (e: Exception) {
                snackbarMessage = e.localizedMessage ?: context.getString(R.string.backup_export_failed, "")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("backup_restore_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.backup_db_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.backup_db_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Unified Backup File Card (Single File with Export/Import & Checkboxes)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.backup_unified_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.backup_unified_subtitle, shifts.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            text = stringResource(R.string.backup_export_dialog_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = exportShiftsChecked,
                                onCheckedChange = { exportShiftsChecked = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.backup_export_option_logs, shifts.size))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = exportSettingsChecked,
                                onCheckedChange = { exportSettingsChecked = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.backup_export_option_schedules))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { exportUnifiedLauncher.launch("shift_tracker_backup.json") },
                                enabled = exportShiftsChecked || exportSettingsChecked,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_unified_json_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup_export_json_btn))
                            }

                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(context, BackupImportActivity::class.java))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_unified_json_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup_import_json_btn))
                            }
                        }
                    }
                }
            }

            // Redo Onboarding Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.backup_redo_onboarding_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.backup_redo_onboarding_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showRedoOnboardingDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("redo_onboarding_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.backup_redo_onboarding_btn))
                        }
                    }
                }
            }
        }
    }

    if (showRedoOnboardingDialog) {
        AlertDialog(
            onDismissRequest = { showRedoOnboardingDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.backup_redo_onboarding_dialog_title),
                    style = WarningDialogTitleStyle,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.backup_redo_onboarding_dialog_msg),
                    style = WarningDialogBodyStyle
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRedoOnboardingDialog = false
                        onRedoOnboarding()
                    },
                    modifier = Modifier.testTag("redo_onboarding_confirm_btn")
                ) {
                    Text(
                        text = stringResource(R.string.backup_redo_onboarding_dialog_confirm),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRedoOnboardingDialog = false },
                    modifier = Modifier.testTag("redo_onboarding_cancel_btn")
                ) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }
}
