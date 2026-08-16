package dev.spectrumgts.shifttracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spectrumgts.shifttracker.ShiftTrackerMainActivity
import dev.spectrumgts.shifttracker.ui.screens.OnboardingScreen
import dev.spectrumgts.shifttracker.ui.theme.MyApplicationTheme
import dev.spectrumgts.shifttracker.ui.viewmodel.OvertimeViewModel

class ShiftTrackerOnboardingActivity : ComponentActivity() {

    private val viewModel: OvertimeViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            MyApplicationTheme {
                val context = LocalContext.current
                val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
                val defaultSchedules by viewModel.defaultSchedules.collectAsStateWithLifecycle()

                OnboardingScreen(
                    appSettings = appSettings,
                    defaultSchedules = defaultSchedules,
                    windowSizeClass = windowSizeClass,
                    onSaveSettings = { bufferBefore, bufferAfter, cutoffTime, ignoreEarly, lunchStart, lunchEnd, subWork, subOff, firstDay ->
                        viewModel.saveAppSettings(bufferBefore, bufferAfter, cutoffTime, ignoreEarly, lunchStart, lunchEnd, subWork, subOff, firstDay)
                    },
                    onSaveSchedule = { dayOfWeek, isWorkDay, workStart, workEnd ->
                        viewModel.saveDefaultSchedule(dayOfWeek, isWorkDay, workStart, workEnd)
                    },
                    onApplyToAllWorkingDays = { workStart, workEnd, forcePreset, firstDay ->
                        viewModel.applyDefaultScheduleToAllWorkingDays(workStart, workEnd, forcePreset, firstDay)
                    },
                    onFinishOnboarding = {
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        prefs.edit().putBoolean("has_completed_onboarding", true).apply()

                        startActivity(Intent(this, ShiftTrackerMainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
