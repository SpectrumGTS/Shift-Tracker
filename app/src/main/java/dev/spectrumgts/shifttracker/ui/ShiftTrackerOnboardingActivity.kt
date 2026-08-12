package dev.spectrumgts.shifttracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spectrumgts.shifttracker.ShiftTrackerMainActivity
import dev.spectrumgts.shifttracker.ui.screens.OnboardingScreen
import dev.spectrumgts.shifttracker.ui.theme.MyApplicationTheme
import dev.spectrumgts.shifttracker.ui.viewmodel.OvertimeViewModel

class ShiftTrackerOnboardingActivity : ComponentActivity() {

    private val viewModel: OvertimeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
                val defaultSchedules by viewModel.defaultSchedules.collectAsStateWithLifecycle()

                OnboardingScreen(
                    appSettings = appSettings,
                    defaultSchedules = defaultSchedules,
                    onSaveSettings = { bufferBefore, bufferAfter, cutoffTime, ignoreEarly, lunchStart, lunchEnd, subWork, subOff ->
                        viewModel.saveAppSettings(bufferBefore, bufferAfter, cutoffTime, ignoreEarly, lunchStart, lunchEnd, subWork, subOff)
                    },
                    onSaveSchedule = { dayOfWeek, isWorkDay, workStart, workEnd ->
                        viewModel.saveDefaultSchedule(dayOfWeek, isWorkDay, workStart, workEnd)
                    },
                    onApplyToAllWorkingDays = { workStart, workEnd ->
                        viewModel.applyDefaultScheduleToAllWorkingDays(workStart, workEnd)
                    },
                    onFinishOnboarding = {
                        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("has_completed_onboarding", true).apply()

                        startActivity(Intent(this, ShiftTrackerMainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
