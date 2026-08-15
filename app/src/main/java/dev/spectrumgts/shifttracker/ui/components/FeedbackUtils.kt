package dev.spectrumgts.shifttracker.ui.components

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.interaction.MutableInteractionSource

/**
 * Triggers only the system-wide touch feedback sound.
 * Used for general buttons, sliders, and chips.
 */
fun triggerTouchSound(view: View) {
    view.playSoundEffect(SoundEffectConstants.CLICK)
}

/**
 * Triggers both sound and haptic feedback.
 * Reserved for Toggles, Checkboxes, and FABs.
 * Uses performHapticFeedback to respect system-wide haptic strength settings.
 */
fun triggerSystemFeedback(view: View) {
    view.playSoundEffect(SoundEffectConstants.CLICK)
    // CLOCK_TICK is a standard, widely supported constant that respects system haptic settings
    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

/**
 * A wrapper for Material 3 Switch that triggers sound and haptic feedback.
 */
@Composable
fun HapticSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val view = LocalView.current
    Switch(
        checked = checked,
        onCheckedChange = {
            triggerSystemFeedback(view)
            onCheckedChange?.invoke(it)
        },
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

/**
 * A wrapper for Material 3 Checkbox that triggers sound and haptic feedback.
 */
@Composable
fun HapticCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val view = LocalView.current
    Checkbox(
        checked = checked,
        onCheckedChange = {
            triggerSystemFeedback(view)
            onCheckedChange?.invoke(it)
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}
