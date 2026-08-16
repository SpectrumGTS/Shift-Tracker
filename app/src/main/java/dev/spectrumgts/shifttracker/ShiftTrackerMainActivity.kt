package dev.spectrumgts.shifttracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import dev.spectrumgts.shifttracker.ui.ShiftTrackerOnboardingActivity
import dev.spectrumgts.shifttracker.ui.navigation.AppNavigationDrawerContent
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
import dev.spectrumgts.shifttracker.ui.viewmodel.ShiftInputState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
class ShiftTrackerMainActivity : ComponentActivity() {

    private val viewModel: OvertimeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("has_completed_onboarding", false)) {
            startActivity(Intent(this, ShiftTrackerOnboardingActivity::class.java))
            finish()
            return
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            MyApplicationTheme {
                OvertimeTrackerApp(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvertimeTrackerApp(
    viewModel: OvertimeViewModel,
    windowSizeClass: WindowSizeClass
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val shifts by viewModel.shifts.collectAsStateWithLifecycle()
    val defaultSchedules by viewModel.defaultSchedules.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val shiftInputState by viewModel.shiftInputState.collectAsStateWithLifecycle()

    val showPermanentDrawer = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded ||
            (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium && windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact)

    if (showPermanentDrawer) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.width(310.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface,
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
                ) {
                    AppNavigationDrawerContent(
                        currentDestination = currentScreen,
                        onDestinationSelected = { destination ->
                            viewModel.navigateTo(destination)
                        },
                        onCloseDrawer = { /* No-op for permanent drawer */ }
                    )
                }
            }
        ) {
            AppContent(
                currentScreen = currentScreen,
                shifts = shifts,
                defaultSchedules = defaultSchedules,
                appSettings = appSettings,
                shiftInputState = shiftInputState,
                viewModel = viewModel,
                drawerState = drawerState,
                coroutineScope = coroutineScope,
                showHamburgerMenu = false,
                context = context,
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical)
            )
        }
    } else {
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
            AppContent(
                currentScreen = currentScreen,
                shifts = shifts,
                defaultSchedules = defaultSchedules,
                appSettings = appSettings,
                shiftInputState = shiftInputState,
                viewModel = viewModel,
                drawerState = drawerState,
                coroutineScope = coroutineScope,
                showHamburgerMenu = true,
                context = context,
                contentWindowInsets = WindowInsets.safeDrawing
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(
    currentScreen: ScreenDestination,
    shifts: List<ShiftLog>,
    defaultSchedules: List<DayDefaultSchedule>,
    appSettings: AppSettings,
    shiftInputState: ShiftInputState?,
    viewModel: OvertimeViewModel,
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    showHamburgerMenu: Boolean,
    context: Context,
    contentWindowInsets: WindowInsets
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = contentWindowInsets,
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
                    if (showHamburgerMenu) {
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
                                contentDescription = stringResource(R.string.content_desc_nav_menu)
                            )
                        }
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
                        appSettings = appSettings,
                        schedules = defaultSchedules,
                        onSaveSchedule = { dayOfWeek, isWorkDay, workStart, workEnd ->
                            viewModel.saveDefaultSchedule(dayOfWeek, isWorkDay, workStart, workEnd)
                        },
                        onSaveFirstDayOfWeek = { firstDay ->
                            viewModel.saveAppSettings(
                                bufferBefore = appSettings.bufferBeforeMinutes,
                                bufferAfter = appSettings.bufferAfterMinutes,
                                cutoffTime = appSettings.cutoffTimeMinutes,
                                ignoreEarlyClockIns = appSettings.ignoreEarlyClockIns,
                                lunchStart = appSettings.lunchStartMinutes,
                                lunchEnd = appSettings.lunchEndMinutes,
                                subtractLunchWorkDays = appSettings.subtractLunchWorkDays,
                                subtractLunchOffDays = appSettings.subtractLunchOffDays,
                                firstDayOfWeek = firstDay
                            )
                        },
                        onApplyToAllWorkingDays = { workStart, workEnd, forcePreset, firstDay ->
                            viewModel.applyDefaultScheduleToAllWorkingDays(workStart, workEnd, forcePreset, firstDay)
                        }
                    )
                }
                ScreenDestination.SETTINGS -> {
                    SettingsScreen(
                        appSettings = appSettings,
                        defaultSchedules = defaultSchedules,
                        onSaveSettings = { bufferBefore, bufferAfter, cutoffTime, ignoreEarly, lunchStart, lunchEnd, subWork, subOff ->
                            viewModel.saveAppSettings(
                                bufferBefore = bufferBefore,
                                bufferAfter = bufferAfter,
                                cutoffTime = cutoffTime,
                                ignoreEarlyClockIns = ignoreEarly,
                                lunchStart = lunchStart,
                                lunchEnd = lunchEnd,
                                subtractLunchWorkDays = subWork,
                                subtractLunchOffDays = subOff,
                                firstDayOfWeek = appSettings.firstDayOfWeek
                            )
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
