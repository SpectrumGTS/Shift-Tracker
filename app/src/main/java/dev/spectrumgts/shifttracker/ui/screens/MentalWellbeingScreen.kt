package dev.spectrumgts.shifttracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.OvertimeCalculator
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WellbeingLevel {
    OPTIMAL,
    MODERATE,
    HIGH,
    CRITICAL
}

@Composable
fun MentalWellbeingScreen(
    shifts: List<ShiftLog>,
    appSettings: AppSettings
) {
    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    val availableMonths = remember(shifts, currentMonthKey) {
        val keys = shifts.map { it.date.take(7) }.toMutableSet()
        keys.add(currentMonthKey)
        keys.toList().sortedDescending()
    }

    var selectedMonthKey by remember { mutableStateOf(currentMonthKey) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    val monthDisplayName = remember(selectedMonthKey) {
        try {
            val fmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val d = fmt.parse(selectedMonthKey)
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d ?: Date())
        } catch (e: Exception) {
            selectedMonthKey
        }
    }

    val selectedMonthShifts = remember(shifts, selectedMonthKey) {
        shifts.filter { it.date.startsWith(selectedMonthKey) }
    }

    val totalOvertimeMinutes = remember(selectedMonthShifts, appSettings) {
        selectedMonthShifts.sumOf { shift ->
            OvertimeCalculator.calculate(
                workStartMinutes = shift.workStartMinutes,
                workEndMinutes = shift.workEndMinutes,
                clockInMinutes = shift.clockInMinutes,
                clockOutMinutes = shift.clockOutMinutes,
                bufferBeforeMinutes = shift.bufferBeforeMinutes,
                bufferAfterMinutes = shift.bufferAfterMinutes,
                isWorkDay = shift.isWorkDay,
                cutoffTimeMinutes = appSettings.cutoffTimeMinutes,
                ignoreEarlyClockIns = appSettings.ignoreEarlyClockIns,
                lunchStartMinutes = appSettings.lunchStartMinutes,
                lunchEndMinutes = appSettings.lunchEndMinutes,
                subtractLunchWorkDays = appSettings.subtractLunchWorkDays,
                subtractLunchOffDays = appSettings.subtractLunchOffDays
            ).totalOvertimeMinutes
        }
    }

    val formattedOvertime = remember(totalOvertimeMinutes) {
        OvertimeCalculator.formatDurationMinutes(totalOvertimeMinutes)
    }

    val wellbeingLevel = remember(totalOvertimeMinutes) {
        val totalHours = totalOvertimeMinutes / 60.0
        when {
            totalHours <= 15.0 -> WellbeingLevel.OPTIMAL
            totalHours <= 35.0 -> WellbeingLevel.MODERATE
            totalHours <= 60.0 -> WellbeingLevel.HIGH
            else -> WellbeingLevel.CRITICAL
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mental_wellbeing_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.wellbeing_headline),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.wellbeing_readonly_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.wellbeing_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Month Selector & Rating Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (wellbeingLevel) {
                        WellbeingLevel.OPTIMAL -> MaterialTheme.colorScheme.primaryContainer
                        WellbeingLevel.MODERATE -> MaterialTheme.colorScheme.secondaryContainer
                        WellbeingLevel.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                        WellbeingLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Title row
                    Text(
                        text = stringResource(R.string.wellbeing_monthly_overtime_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (wellbeingLevel) {
                            WellbeingLevel.OPTIMAL -> MaterialTheme.colorScheme.onPrimaryContainer
                            WellbeingLevel.MODERATE -> MaterialTheme.colorScheme.onSecondaryContainer
                            WellbeingLevel.HIGH -> MaterialTheme.colorScheme.onTertiaryContainer
                            WellbeingLevel.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
                        }.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month dropdown selector (Row 2 - Full/Wide button)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { monthDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wellbeing_month_selector")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = monthDisplayName,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        DropdownMenu(
                            expanded = monthDropdownExpanded,
                            onDismissRequest = { monthDropdownExpanded = false }
                        ) {
                            availableMonths.forEach { mKey ->
                                val displayName = try {
                                    val fmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                                    val d = fmt.parse(mKey)
                                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d ?: Date())
                                } catch (e: Exception) {
                                    mKey
                                }
                                DropdownMenuItem(
                                    text = { Text(displayName) },
                                    onClick = {
                                        selectedMonthKey = mKey
                                        monthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big overtime amount + Status Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = formattedOvertime,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (wellbeingLevel) {
                                    WellbeingLevel.OPTIMAL -> MaterialTheme.colorScheme.onPrimaryContainer
                                    WellbeingLevel.MODERATE -> MaterialTheme.colorScheme.onSecondaryContainer
                                    WellbeingLevel.HIGH -> MaterialTheme.colorScheme.onTertiaryContainer
                                    WellbeingLevel.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                        }

                        // Rating badge
                        val ratingText = when (wellbeingLevel) {
                            WellbeingLevel.OPTIMAL -> stringResource(R.string.wellbeing_rating_optimal)
                            WellbeingLevel.MODERATE -> stringResource(R.string.wellbeing_rating_moderate)
                            WellbeingLevel.HIGH -> stringResource(R.string.wellbeing_rating_high)
                            WellbeingLevel.CRITICAL -> stringResource(R.string.wellbeing_rating_critical)
                        }

                        val ratingIcon: ImageVector = when (wellbeingLevel) {
                            WellbeingLevel.OPTIMAL -> Icons.Default.CheckCircle
                            WellbeingLevel.MODERATE -> Icons.Default.Info
                            WellbeingLevel.HIGH -> Icons.Default.Warning
                            WellbeingLevel.CRITICAL -> Icons.Default.Error
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when (wellbeingLevel) {
                                WellbeingLevel.OPTIMAL -> MaterialTheme.colorScheme.primary
                                WellbeingLevel.MODERATE -> MaterialTheme.colorScheme.secondary
                                WellbeingLevel.HIGH -> MaterialTheme.colorScheme.tertiary
                                WellbeingLevel.CRITICAL -> MaterialTheme.colorScheme.error
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = ratingIcon,
                                    contentDescription = null,
                                    tint = when (wellbeingLevel) {
                                        WellbeingLevel.OPTIMAL -> MaterialTheme.colorScheme.onPrimary
                                        WellbeingLevel.MODERATE -> MaterialTheme.colorScheme.onSecondary
                                        WellbeingLevel.HIGH -> MaterialTheme.colorScheme.onTertiary
                                        WellbeingLevel.CRITICAL -> MaterialTheme.colorScheme.onError
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ratingText,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when (wellbeingLevel) {
                                        WellbeingLevel.OPTIMAL -> MaterialTheme.colorScheme.onPrimary
                                        WellbeingLevel.MODERATE -> MaterialTheme.colorScheme.onSecondary
                                        WellbeingLevel.HIGH -> MaterialTheme.colorScheme.onTertiary
                                        WellbeingLevel.CRITICAL -> MaterialTheme.colorScheme.onError
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description text
                    val ratingDesc = when (wellbeingLevel) {
                        WellbeingLevel.OPTIMAL -> stringResource(R.string.wellbeing_desc_optimal, formattedOvertime)
                        WellbeingLevel.MODERATE -> stringResource(R.string.wellbeing_desc_moderate, formattedOvertime)
                        WellbeingLevel.HIGH -> stringResource(R.string.wellbeing_desc_high, formattedOvertime)
                        WellbeingLevel.CRITICAL -> stringResource(R.string.wellbeing_desc_critical, formattedOvertime)
                    }

                    Text(
                        text = ratingDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (wellbeingLevel) {
                            WellbeingLevel.OPTIMAL -> MaterialTheme.colorScheme.onPrimaryContainer
                            WellbeingLevel.MODERATE -> MaterialTheme.colorScheme.onSecondaryContainer
                            WellbeingLevel.HIGH -> MaterialTheme.colorScheme.onTertiaryContainer
                            WellbeingLevel.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }

        // Section Title for Educational Resources
        item {
            Text(
                text = stringResource(R.string.wellbeing_section_resources),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Material Card 1: Burnout Signs
        item {
            WellbeingMaterialCard(
                icon = Icons.Default.Psychology,
                title = stringResource(R.string.wellbeing_mat_burnout_title),
                description = stringResource(R.string.wellbeing_mat_burnout_desc),
                items = listOf(
                    stringResource(R.string.wellbeing_mat_burnout_item1),
                    stringResource(R.string.wellbeing_mat_burnout_item2),
                    stringResource(R.string.wellbeing_mat_burnout_item3),
                    stringResource(R.string.wellbeing_mat_burnout_item4)
                )
            )
        }

        // Material Card 2: Recovery Strategies
        item {
            WellbeingMaterialCard(
                icon = Icons.Default.Lightbulb,
                title = stringResource(R.string.wellbeing_mat_strategies_title),
                description = stringResource(R.string.wellbeing_mat_strategies_desc),
                items = listOf(
                    stringResource(R.string.wellbeing_mat_strategies_item1),
                    stringResource(R.string.wellbeing_mat_strategies_item2),
                    stringResource(R.string.wellbeing_mat_strategies_item3),
                    stringResource(R.string.wellbeing_mat_strategies_item4)
                )
            )
        }

        // Material Card 3: Box Breathing
        item {
            WellbeingMaterialCard(
                icon = Icons.Default.Air,
                title = stringResource(R.string.wellbeing_mat_breathing_title),
                description = stringResource(R.string.wellbeing_mat_breathing_desc),
                items = listOf(
                    stringResource(R.string.wellbeing_mat_breathing_step1),
                    stringResource(R.string.wellbeing_mat_breathing_step2),
                    stringResource(R.string.wellbeing_mat_breathing_step3),
                    stringResource(R.string.wellbeing_mat_breathing_step4)
                )
            )
        }

        // Material Card 4: Seeking Support
        item {
            WellbeingMaterialCard(
                icon = Icons.AutoMirrored.Filled.ContactSupport,
                title = stringResource(R.string.wellbeing_mat_support_title),
                description = stringResource(R.string.wellbeing_mat_support_desc),
                items = listOf(
                    stringResource(R.string.wellbeing_mat_support_item1),
                    stringResource(R.string.wellbeing_mat_support_item2),
                    stringResource(R.string.wellbeing_mat_support_item3)
                )
            )
        }
    }
}

@Composable
private fun WellbeingMaterialCard(
    icon: ImageVector,
    title: String,
    description: String,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            items.forEach { itemText ->
                Text(
                    text = itemText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}
