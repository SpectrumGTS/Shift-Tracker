package dev.spectrumgts.shifttracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.data.model.ShiftLog

@Composable
fun BackupRestoreScreen(
    shifts: List<ShiftLog>,
    defaultSchedules: List<DayDefaultSchedule>,
    onExportShifts: (Uri) -> Unit,
    onImportShifts: (Uri) -> Unit,
    onExportSchedules: (Uri) -> Unit,
    onImportSchedules: (Uri) -> Unit,
    onExportUnifiedBackup: ((Uri) -> Unit)? = null,
    onImportUnifiedBackup: ((Uri, Boolean, Boolean) -> Unit)? = null,
    onRedoOnboarding: () -> Unit = {}
) {
    val context = LocalContext.current
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var pendingImportType by remember { mutableStateOf<String?>(null) } // "shifts", "schedules", or "unified"
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var restoreShiftsChecked by remember { mutableStateOf(true) }
    var restoreSettingsChecked by remember { mutableStateOf(true) }

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
                if (onExportUnifiedBackup != null) {
                    onExportUnifiedBackup(it)
                } else {
                    onExportShifts(it)
                    onExportSchedules(it)
                }
                snackbarMessage = context.getString(R.string.backup_export_success_unified)
            } catch (e: Exception) {
                snackbarMessage = context.getString(R.string.backup_export_failed, e.localizedMessage ?: "")
            }
        }
    }

    val importUnifiedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            pendingImportType = "unified"
            restoreShiftsChecked = true
            restoreSettingsChecked = true
        }
    }

    val exportShiftsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                onExportShifts(it)
                snackbarMessage = context.getString(R.string.backup_export_success_shifts)
            } catch (e: Exception) {
                snackbarMessage = context.getString(R.string.backup_export_failed, e.localizedMessage ?: "")
            }
        }
    }

    val importShiftsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                onImportShifts(it)
                snackbarMessage = context.getString(R.string.backup_import_success_shifts)
            } catch (e: Exception) {
                snackbarMessage = context.getString(R.string.backup_import_failed, e.localizedMessage ?: "")
            }
        }
    }

    val exportSchedulesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                onExportSchedules(it)
                snackbarMessage = context.getString(R.string.backup_export_success_schedules)
            } catch (e: Exception) {
                snackbarMessage = context.getString(R.string.backup_export_failed, e.localizedMessage ?: "")
            }
        }
    }

    val importSchedulesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                onImportSchedules(it)
                snackbarMessage = context.getString(R.string.backup_import_success_schedules)
            } catch (e: Exception) {
                snackbarMessage = context.getString(R.string.backup_import_failed, e.localizedMessage ?: "")
            }
        }
    }

    // Restore Options & Confirmation Dialog
    if (pendingImportType != null) {
        val type = pendingImportType
        val uri = pendingImportUri

        AlertDialog(
            onDismissRequest = {
                pendingImportType = null
                pendingImportUri = null
            },
            title = { Text(text = stringResource(R.string.backup_confirm_overwrite_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (type == "unified" && uri != null) {
                        Text(
                            text = stringResource(R.string.backup_restore_dialog_intro),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = restoreShiftsChecked,
                                onCheckedChange = { restoreShiftsChecked = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.backup_restore_option_logs, shifts.size))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = restoreSettingsChecked,
                                onCheckedChange = { restoreSettingsChecked = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.backup_restore_option_schedules, defaultSchedules.size))
                        }
                    } else if (type == "shifts") {
                        Text(text = stringResource(R.string.backup_confirm_import_shifts_msg))
                    } else {
                        Text(text = stringResource(R.string.backup_confirm_import_schedules_msg))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentType = pendingImportType
                        val targetUri = pendingImportUri
                        pendingImportType = null
                        pendingImportUri = null

                        if (currentType == "unified" && targetUri != null) {
                            try {
                                if (onImportUnifiedBackup != null) {
                                    onImportUnifiedBackup(targetUri, restoreShiftsChecked, restoreSettingsChecked)
                                } else {
                                    if (restoreShiftsChecked) onImportShifts(targetUri)
                                    if (restoreSettingsChecked) onImportSchedules(targetUri)
                                }
                                snackbarMessage = context.getString(R.string.backup_import_success_unified)
                            } catch (e: Exception) {
                                snackbarMessage = context.getString(R.string.backup_import_failed, e.localizedMessage ?: "")
                            }
                        } else if (currentType == "shifts") {
                            importShiftsLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "*/*"))
                        } else if (currentType == "schedules") {
                            importSchedulesLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "*/*"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = type != "unified" || (restoreShiftsChecked || restoreSettingsChecked),
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text(stringResource(R.string.backup_overwrite_import_btn))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingImportType = null
                        pendingImportUri = null
                    },
                    modifier = Modifier.testTag("dismiss_import_button")
                ) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
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

            // Unified Backup File Card (Single File)
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
                                    text = stringResource(R.string.backup_unified_subtitle, shifts.size, defaultSchedules.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { exportUnifiedLauncher.launch("shift_tracker_backup.json") },
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
                                    importUnifiedLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
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

            // Independent Shift Logs Backup Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.backup_shifts_count_title, shifts.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.backup_shifts_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { exportShiftsLauncher.launch("overtime_shifts_backup.json") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_shifts_csv_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup_export_json_btn))
                            }

                            OutlinedButton(
                                onClick = { pendingImportType = "shifts" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_shifts_csv_btn"),
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

            // Independent Default Schedules & Settings Backup Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.backup_schedules_count_title, defaultSchedules.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.backup_schedules_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { exportSchedulesLauncher.launch("overtime_schedules_backup.json") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_schedules_csv_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup_export_json_btn))
                            }

                            OutlinedButton(
                                onClick = { pendingImportType = "schedules" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_schedules_csv_btn"),
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
                                    style = MaterialTheme.typography.titleMedium,
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
                            onClick = onRedoOnboarding,
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
}

