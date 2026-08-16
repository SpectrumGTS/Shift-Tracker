package dev.spectrumgts.shifttracker

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.spectrumgts.shifttracker.notifications.NotificationHelper
import dev.spectrumgts.shifttracker.ui.MainApp
import dev.spectrumgts.shifttracker.ui.ShiftTrackerOnboardingActivity
import dev.spectrumgts.shifttracker.ui.theme.MyApplicationTheme
import dev.spectrumgts.shifttracker.ui.viewmodel.ShiftTrackerViewModel

/**
 * Entry point Activity for the Shift Tracker application.
 * Handles system-level integration: Splash Screen, Edge-to-Edge, Onboarding checks, and Intents.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ShiftTrackerMainActivity : ComponentActivity() {

    private val viewModel: ShiftTrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize the splash screen before calling super.onCreate
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge support for modern Android look
        enableEdgeToEdge()

        // Redirect to onboarding if not completed
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("has_completed_onboarding", false)) {
            startActivity(Intent(this, ShiftTrackerOnboardingActivity::class.java))
            finish()
            return
        }

        // Process any incoming intent (e.g. from notification clicks)
        handleIntent(intent)

        setContent {
            // Calculate window size class to support adaptive layouts (Phone, Tablet, Desktop)
            val windowSizeClass = calculateWindowSizeClass(this)
            
            MyApplicationTheme {
                // Delegate UI scaffolding and navigation to MainApp composable
                MainApp(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }

    /**
     * Handles intents received when the Activity is already running in the foreground.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Routes logic based on intent extras (e.g. deep-linking from a reminder notification).
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra("EXTRA_ACTION") == "LOG_SHIFT") {
            // Clear the notification from the tray manually when using action buttons
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
            
            // Trigger the "Add Shift" dialog
            viewModel.openNewShiftDialog()
        }
    }
}
