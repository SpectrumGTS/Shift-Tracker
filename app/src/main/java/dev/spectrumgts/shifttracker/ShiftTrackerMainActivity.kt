package dev.spectrumgts.shifttracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spectrumgts.shifttracker.ui.ShiftTrackerOnboardingActivity
import dev.spectrumgts.shifttracker.ui.navigation.AppNavigationDrawerSheet
import dev.spectrumgts.shifttracker.ui.screens.AboutScreen
import dev.spectrumgts.shifttracker.ui.screens.AddEditShiftDialog
import dev.spectrumgts.shifttracker.ui.screens.BackupRestoreScreen
import dev.spectrumgts.shifttracker.ui.screens.DashboardScreen
import dev.spectrumgts.shifttracker.ui.screens.DefaultScheduleScreen
import dev.spectrumgts.shifttracker.ui.screens.InsightsScreen
import dev.spectrumgts.shifttracker.ui.screens.MentalWellbeingScreen
import dev.spectrumgts.shifttracker.ui.screens.SettingsScreen
import dev.spectrumgts.shifttracker.ui.screens.ShiftHistoryScreen
import dev.spectrumgts.shifttracker.ui.theme.MyApplicationTheme
import dev.spectrumgts.shifttracker.ui.viewmodel.OvertimeViewModel
import dev.spectrumgts.shifttracker.ui.viewmodel.ScreenDestination
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class ShiftTrackerMainActivity : ComponentActivity() {

    private val viewModel: OvertimeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("has_completed_onboarding", false)) {
            startActivity(Intent(this, ShiftTrackerOnboardingActivity::class.java))
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                OvertimeTrackerApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvertimeTrackerApp(viewModel: OvertimeViewModel) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val shifts by viewModel.shifts.collectAsStateWithLifecycle()
    val defaultSchedules by viewModel.defaultSchedules.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val shiftInputState by viewModel.shiftInputState.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppNavigationDrawerSheet(
                    currentDestination = currentScreen,
                    onDestinationSelected = { destination ->
                        viewModel.navigateTo(destination)
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentScreen) {
                                    ScreenDestination.DASHBOARD -> stringResource(R.string.app_name)
                                    ScreenDestination.DEFAULT_SCHEDULES -> stringResource(R.string.menu_default_schedule)
                                    ScreenDestination.SETTINGS -> stringResource(R.string.menu_buffer_grace_time)
                                    ScreenDestination.HISTORY -> stringResource(R.string.menu_history)
                                    ScreenDestination.INSIGHTS -> stringResource(R.string.menu_insights)
                                    ScreenDestination.MENTAL_WELLBEING -> stringResource(R.string.title_mental_wellbeing)
                                    ScreenDestination.BACKUP_RESTORE -> stringResource(R.string.menu_backup_restore)
                                    ScreenDestination.ABOUT -> stringResource(R.string.menu_about)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier.testTag("hamburger_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open slide-in navigation menu"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        ScreenDestination.DASHBOARD -> {
                            DashboardScreen(
                                shifts = shifts,
                                defaultSchedules = defaultSchedules,
                                appSettings = appSettings,
                                onLogNewShift = { viewModel.openNewShiftDialog() },
                                onEditShift = { shift -> viewModel.openEditShiftDialog(shift) },
                                onDeleteShift = { shift -> viewModel.deleteShift(shift) },
                                onNavigateToHistory = { viewModel.navigateTo(ScreenDestination.HISTORY) }
                            )
                        }
                        ScreenDestination.DEFAULT_SCHEDULES -> {
                            DefaultScheduleScreen(
                                schedules = defaultSchedules,
                                onSaveSchedule = { dayOfWeek, isWorkDay, workStart, workEnd ->
                                    viewModel.saveDefaultSchedule(dayOfWeek, isWorkDay, workStart, workEnd)
                                },
                                onApplyToAllWorkingDays = { workStart, workEnd ->
                                    viewModel.applyDefaultScheduleToAllWorkingDays(workStart, workEnd)
                                }
                            )
                        }
                        ScreenDestination.SETTINGS -> {
                            SettingsScreen(
                                appSettings = appSettings,
                                defaultSchedules = defaultSchedules,
                                onSaveSettings = { bufferBefore, bufferAfter, cutoffTime, ignoreEarly, lunchStart, lunchEnd, subWork, subOff ->
                                    viewModel.saveAppSettings(bufferBefore, bufferAfter, cutoffTime, ignoreEarly, lunchStart, lunchEnd, subWork, subOff)
                                }
                            )
                        }
                        ScreenDestination.HISTORY -> {
                            ShiftHistoryScreen(
                                shifts = shifts,
                                onLogNewShift = { viewModel.openNewShiftDialog() },
                                onEditShift = { shift -> viewModel.openEditShiftDialog(shift) },
                                onDeleteShift = { shift -> viewModel.deleteShift(shift) },
                                appSettings = appSettings
                            )
                        }
                        ScreenDestination.INSIGHTS -> {
                            InsightsScreen(
                                shifts = shifts,
                                appSettings = appSettings
                            )
                        }
                        ScreenDestination.MENTAL_WELLBEING -> {
                            MentalWellbeingScreen(
                                shifts = shifts,
                                appSettings = appSettings
                            )
                        }
                        ScreenDestination.BACKUP_RESTORE -> {
                            BackupRestoreScreen(
                                shifts = shifts,
                                defaultSchedules = defaultSchedules,
                                onExportUnifiedBackup = { uri, includeShifts, includeSettings ->
                                    viewModel.exportUnifiedBackupToUri(uri, context, includeShifts, includeSettings)
                                },
                                onRedoOnboarding = {
                                    context.startActivity(Intent(context, ShiftTrackerOnboardingActivity::class.java))
                                }
                            )
                        }
                        ScreenDestination.ABOUT -> {
                            AboutScreen()
                        }
                    }

                    // Active Shift Input / Edit Dialog
                    shiftInputState?.let { input ->
                        AddEditShiftDialog(
                            inputState = input,
                            onDateChanged = { newDate -> viewModel.updateShiftDate(newDate) },
                            onTimesUpdated = { workStart, workEnd, clockIn, clockOut, bufferBefore, bufferAfter, isWorkDay, notes ->
                                viewModel.updateShiftTimes(
                                    workStart,
                                    workEnd,
                                    clockIn,
                                    clockOut,
                                    bufferBefore,
                                    bufferAfter,
                                    isWorkDay,
                                    notes
                                )
                            },
                            onDismiss = { viewModel.dismissShiftDialog() },
                            onSave = { viewModel.saveCurrentShift() },
                            appSettings = appSettings
                        )
                    }
                }
            }
        }
    }
