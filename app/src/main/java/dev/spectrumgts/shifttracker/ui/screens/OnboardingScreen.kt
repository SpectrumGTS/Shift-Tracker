package dev.spectrumgts.shifttracker.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.ui.components.BufferAfterCard
import dev.spectrumgts.shifttracker.ui.components.BufferBeforeCard
import dev.spectrumgts.shifttracker.ui.components.CutoffTimeCard
import dev.spectrumgts.shifttracker.ui.components.DefaultScheduleSettingsCard
import dev.spectrumgts.shifttracker.ui.components.LunchBreakCard
import dev.spectrumgts.shifttracker.ui.components.M3TimePickerDialog

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    appSettings: AppSettings,
    defaultSchedules: List<DayDefaultSchedule>,
    onSaveSettings: (
        bufferBefore: Int,
        bufferAfter: Int,
        cutoffTime: Int,
        ignoreEarlyClockIns: Boolean,
        lunchStartMinutes: Int,
        lunchEndMinutes: Int,
        subtractLunchWorkDays: Boolean,
        subtractLunchOffDays: Boolean
    ) -> Unit,
    onSaveSchedule: (dayOfWeek: Int, isWorkDay: Boolean, workStart: Int, workEnd: Int) -> Unit,
    onApplyToAllWorkingDays: (workStart: Int, workEnd: Int) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) } // Step 0 = Welcome, 1 = Buffer, 2 = Schedule, 3 = Wellbeing
    val totalSetupSteps = 3

    // State for Buffer & Lunch settings (Step 1)
    var bufferBefore by remember(appSettings) { mutableFloatStateOf(appSettings.bufferBeforeMinutes.toFloat()) }
    var bufferAfter by remember(appSettings) { mutableFloatStateOf(appSettings.bufferAfterMinutes.toFloat()) }
    var cutoffTime by remember(appSettings) { mutableIntStateOf(appSettings.cutoffTimeMinutes) }
    var ignoreEarlyClockIns by remember(appSettings) { mutableStateOf(appSettings.ignoreEarlyClockIns) }
    var lunchStart by remember(appSettings) { mutableIntStateOf(appSettings.lunchStartMinutes) }
    var lunchEnd by remember(appSettings) { mutableIntStateOf(appSettings.lunchEndMinutes) }
    var subtractLunchWorkDays by remember(appSettings) { mutableStateOf(appSettings.subtractLunchWorkDays) }
    var subtractLunchOffDays by remember(appSettings) { mutableStateOf(appSettings.subtractLunchOffDays) }

    var showCutoffTimePicker by remember { mutableStateOf(false) }
    var showLunchStartTimePicker by remember { mutableStateOf(false) }
    var showLunchEndTimePicker by remember { mutableStateOf(false) }

    // Backup restore file launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onImportBackup(it)
            Toast.makeText(context, context.getString(R.string.onboarding_restore_success), Toast.LENGTH_LONG).show()
            onFinishOnboarding()
        }
    }

    // Synchronize settings changes
    LaunchedEffect(bufferBefore, bufferAfter, cutoffTime, ignoreEarlyClockIns, lunchStart, lunchEnd, subtractLunchWorkDays, subtractLunchOffDays) {
        onSaveSettings(
            bufferBefore.toInt(),
            bufferAfter.toInt(),
            cutoffTime,
            ignoreEarlyClockIns,
            lunchStart,
            lunchEnd,
            subtractLunchWorkDays,
            subtractLunchOffDays
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("onboarding_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentStep > 0) 80.dp else 0.dp) // Leave room for bottom bar on steps 1..3
        ) {
            if (currentStep > 0) {
                // Header Pixel OOBE Style for setup steps
                OobeHeader(
                    currentStep = currentStep,
                    totalSteps = totalSetupSteps
                )
            }

            // Step Content Switcher
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() with
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() with
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> OnboardingWelcomeStep(
                        onGetStarted = { currentStep = 1 },
                        onRestoreBackup = { filePickerLauncher.launch("*/*") },
                        onSkipSetup = onFinishOnboarding
                    )
                    1 -> OnboardingStep1Buffer(
                        bufferBefore = bufferBefore,
                        onBufferBeforeChange = { bufferBefore = it },
                        bufferAfter = bufferAfter,
                        onBufferAfterChange = { bufferAfter = it },
                        cutoffTime = cutoffTime,
                        onCutoffTimeChange = { cutoffTime = it },
                        ignoreEarlyClockIns = ignoreEarlyClockIns,
                        onIgnoreEarlyChange = { ignoreEarlyClockIns = it },
                        lunchStart = lunchStart,
                        lunchEnd = lunchEnd,
                        onShowLunchStartPicker = { showLunchStartTimePicker = true },
                        onShowLunchEndPicker = { showLunchEndTimePicker = true },
                        subtractLunchWorkDays = subtractLunchWorkDays,
                        onSubtractWorkDaysChange = { subtractLunchWorkDays = it },
                        subtractLunchOffDays = subtractLunchOffDays,
                        onSubtractOffDaysChange = { subtractLunchOffDays = it },
                        onShowCutoffPicker = { showCutoffTimePicker = true }
                    )
                    2 -> OnboardingStep2Schedule(
                        schedules = defaultSchedules,
                        onSaveSchedule = onSaveSchedule,
                        onApplyToAllWorkingDays = onApplyToAllWorkingDays
                    )
                    3 -> OnboardingStep3Wellbeing()
                }
            }
        }

        // Bottom Navigation Bar for setup steps 1..3
        if (currentStep > 0) {
            OobeBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                currentStep = currentStep,
                totalSteps = totalSetupSteps,
                onBack = { if (currentStep > 0) currentStep-- },
                onNext = {
                    if (currentStep < totalSetupSteps) {
                        currentStep++
                    } else {
                        onFinishOnboarding()
                    }
                },
                onSkip = onFinishOnboarding
            )
        }
    }

    // Time Pickers for Step 1
    if (showLunchStartTimePicker) {
        M3TimePickerDialog(
            title = stringResource(R.string.select_lunch_start_title),
            initialMinutesFromMidnight = lunchStart,
            onDismissRequest = { showLunchStartTimePicker = false },
            onTimeSelected = { selectedMins ->
                lunchStart = selectedMins
                showLunchStartTimePicker = false
            }
        )
    }

    if (showLunchEndTimePicker) {
        M3TimePickerDialog(
            title = stringResource(R.string.select_lunch_end_title),
            initialMinutesFromMidnight = lunchEnd,
            onDismissRequest = { showLunchEndTimePicker = false },
            onTimeSelected = { selectedMins ->
                lunchEnd = selectedMins
                showLunchEndTimePicker = false
            }
        )
    }

    if (showCutoffTimePicker) {
        M3TimePickerDialog(
            title = stringResource(R.string.select_cutoff_title),
            initialMinutesFromMidnight = cutoffTime,
            onDismissRequest = { showCutoffTimePicker = false },
            onTimeSelected = { selectedMins ->
                cutoffTime = selectedMins
                showCutoffTimePicker = false
            }
        )
    }
}

@Composable
private fun OnboardingWelcomeStep(
    onGetStarted: () -> Unit,
    onRestoreBackup: () -> Unit,
    onSkipSetup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Illustration & Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MoreTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Feature Highlights Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WelcomeFeatureRow(
                        icon = Icons.Default.Tune,
                        title = stringResource(R.string.onboarding_step_1_title),
                        desc = stringResource(R.string.onboarding_feature_buffer_desc)
                    )
                    WelcomeFeatureRow(
                        icon = Icons.Default.CalendarMonth,
                        title = stringResource(R.string.onboarding_step_2_title),
                        desc = stringResource(R.string.onboarding_feature_schedule_desc)
                    )
                    WelcomeFeatureRow(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.onboarding_step_3_title),
                        desc = stringResource(R.string.onboarding_feature_wellbeing_desc)
                    )
                }
            }
        }

        // Actions Block
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_welcome_get_started_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.onboarding_btn_get_started),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            OutlinedButton(
                onClick = onRestoreBackup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_welcome_restore_backup_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_btn_restore_backup),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onSkipSetup,
                modifier = Modifier.testTag("onboarding_welcome_skip_btn")
            ) {
                Text(
                    text = stringResource(R.string.onboarding_btn_skip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OobeHeader(
    currentStep: Int,
    totalSteps: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Illustration Badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (currentStep) {
                            1 -> Icons.Default.Tune
                            2 -> Icons.Default.CalendarMonth
                            else -> Icons.Default.SelfImprovement
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (currentStep) {
                    1 -> stringResource(R.string.onboarding_step_1_title)
                    2 -> stringResource(R.string.onboarding_step_2_title)
                    else -> stringResource(R.string.onboarding_step_3_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (currentStep) {
                    1 -> stringResource(R.string.onboarding_step_1_subtitle)
                    2 -> stringResource(R.string.onboarding_step_2_subtitle)
                    else -> stringResource(R.string.onboarding_step_3_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Indicator Bar/Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..totalSteps) {
                    val isActive = i == currentStep
                    val isCompleted = i < currentStep

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isActive) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> MaterialTheme.colorScheme.primary
                                    isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStep1Buffer(
    bufferBefore: Float,
    onBufferBeforeChange: (Float) -> Unit,
    bufferAfter: Float,
    onBufferAfterChange: (Float) -> Unit,
    cutoffTime: Int,
    onCutoffTimeChange: (Int) -> Unit,
    ignoreEarlyClockIns: Boolean,
    onIgnoreEarlyChange: (Boolean) -> Unit,
    lunchStart: Int,
    lunchEnd: Int,
    onShowLunchStartPicker: () -> Unit,
    onShowLunchEndPicker: () -> Unit,
    subtractLunchWorkDays: Boolean,
    onSubtractWorkDaysChange: (Boolean) -> Unit,
    subtractLunchOffDays: Boolean,
    onSubtractOffDaysChange: (Boolean) -> Unit,
    onShowCutoffPicker: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Buffer BEFORE Working Hours Card
        item {
            BufferBeforeCard(
                bufferBefore = bufferBefore,
                onBufferBeforeChange = onBufferBeforeChange,
                ignoreEarlyClockIns = ignoreEarlyClockIns,
                onIgnoreEarlyChange = onIgnoreEarlyChange
            )
        }

        // Buffer AFTER Working Hours Card
        item {
            BufferAfterCard(
                bufferAfter = bufferAfter,
                onBufferAfterChange = onBufferAfterChange
            )
        }

        // Lunch Break Settings Card
        item {
            LunchBreakCard(
                lunchStart = lunchStart,
                lunchEnd = lunchEnd,
                subtractLunchWorkDays = subtractLunchWorkDays,
                onSubtractWorkDaysChange = onSubtractWorkDaysChange,
                subtractLunchOffDays = subtractLunchOffDays,
                onSubtractOffDaysChange = onSubtractOffDaysChange,
                onShowLunchStartPicker = onShowLunchStartPicker,
                onShowLunchEndPicker = onShowLunchEndPicker
            )
        }

        // Overnight Shift Cutoff Time Card
        item {
            CutoffTimeCard(
                cutoffTime = cutoffTime,
                onCutoffTimeChange = onCutoffTimeChange,
                onShowCutoffPicker = onShowCutoffPicker
            )
        }
    }
}

@Composable
private fun OnboardingStep2Schedule(
    schedules: List<DayDefaultSchedule>,
    onSaveSchedule: (dayOfWeek: Int, isWorkDay: Boolean, workStart: Int, workEnd: Int) -> Unit,
    onApplyToAllWorkingDays: (workStart: Int, workEnd: Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)
    ) {
        item {
            DefaultScheduleSettingsCard(
                schedules = schedules,
                onSaveSchedule = onSaveSchedule,
                onApplyToAllWorkingDays = onApplyToAllWorkingDays,
                showTitleHeader = false
            )
        }
    }
}

@Composable
private fun OnboardingStep3Wellbeing() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WellbeingTipCard(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.onboarding_tip1_title),
                description = stringResource(R.string.onboarding_tip1_desc)
            )
        }

        item {
            WellbeingTipCard(
                icon = Icons.Default.SelfImprovement,
                title = stringResource(R.string.onboarding_tip2_title),
                description = stringResource(R.string.onboarding_tip2_desc)
            )
        }

        item {
            WellbeingTipCard(
                icon = Icons.Default.Psychology,
                title = stringResource(R.string.onboarding_tip3_title),
                description = stringResource(R.string.onboarding_tip3_desc)
            )
        }

        item {
            WellbeingTipCard(
                icon = Icons.Default.Air,
                title = stringResource(R.string.onboarding_tip4_title),
                description = stringResource(R.string.onboarding_tip4_desc)
            )
        }
    }
}

@Composable
private fun WellbeingTipCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OobeBottomBar(
    modifier: Modifier = Modifier,
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("onboarding_back_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.onboarding_btn_back))
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("onboarding_next_btn")
            ) {
                Text(
                    text = if (currentStep < totalSteps) {
                        stringResource(R.string.onboarding_btn_next)
                    } else {
                        stringResource(R.string.onboarding_btn_done)
                    },
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (currentStep < totalSteps) Icons.Default.ArrowForward else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
