package dev.spectrumgts.shifttracker.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.ShiftTrackerMainActivity
import dev.spectrumgts.shifttracker.ui.theme.MyApplicationTheme
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogBodyStyle
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogTitleStyle
import dev.spectrumgts.shifttracker.ui.viewmodel.OvertimeViewModel

class BackupImportActivity : ComponentActivity() {

    private val viewModel: OvertimeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                var selectedUri by remember { mutableStateOf<Uri?>(null) }
                var showDialog by remember { mutableStateOf(false) }
                var restoreShiftsChecked by remember { mutableStateOf(true) }
                var restoreSettingsChecked by remember { mutableStateOf(true) }

                val shifts by viewModel.shifts.collectAsStateWithLifecycle()
                val defaultSchedules by viewModel.defaultSchedules.collectAsStateWithLifecycle()

                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        selectedUri = uri
                        showDialog = true
                    } else {
                        finish()
                    }
                }

                LaunchedEffect(Unit) {
                    filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }

                if (showDialog && selectedUri != null) {
                    val uri = selectedUri!!
                    AlertDialog(
                        onDismissRequest = {
                            showDialog = false
                            finish()
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.backup_confirm_overwrite_title),
                                style = WarningDialogTitleStyle
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.backup_restore_dialog_intro),
                                    style = WarningDialogBodyStyle
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
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDialog = false
                                    viewModel.importUnifiedBackupFromUri(
                                        uri = uri,
                                        context = context,
                                        restoreShifts = restoreShiftsChecked,
                                        restoreSettings = restoreSettingsChecked
                                    ) { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            val prefs = context.getSharedPreferences("app_prefs",
                                                MODE_PRIVATE
                                            )
                                            prefs.edit().putBoolean("has_completed_onboarding", true).apply()

                                            val intent = Intent(context, ShiftTrackerMainActivity::class.java).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        }
                                        finish()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                enabled = restoreShiftsChecked || restoreSettingsChecked,
                                modifier = Modifier.testTag("confirm_import_button")
                            ) {
                                Text(stringResource(R.string.backup_overwrite_import_btn))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showDialog = false
                                    finish()
                                },
                                modifier = Modifier.testTag("dismiss_import_button")
                            ) {
                                Text(stringResource(R.string.cancel_btn))
                            }
                        }
                    )
                }
            }
        }
    }
}
