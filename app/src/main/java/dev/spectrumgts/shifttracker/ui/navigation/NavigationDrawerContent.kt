package dev.spectrumgts.shifttracker.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.ui.components.triggerTouchSound
import dev.spectrumgts.shifttracker.ui.theme.LocalSystemCornerRadius
import dev.spectrumgts.shifttracker.ui.viewmodel.ScreenDestination

@Composable
fun AppNavigationDrawerSheet(
    currentDestination: ScreenDestination,
    onDestinationSelected: (ScreenDestination) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val systemRadius = LocalSystemCornerRadius.current
    ModalDrawerSheet(
        modifier = Modifier
            .width(310.dp)
            .testTag("navigation_drawer_sheet"),
        drawerShape = RoundedCornerShape(topEnd = systemRadius, bottomEnd = systemRadius),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
    ) {
        AppNavigationDrawerContent(
            currentDestination = currentDestination,
            onDestinationSelected = onDestinationSelected,
            onCloseDrawer = onCloseDrawer
        )
    }
}

@Composable
fun AppNavigationDrawerContent(
    currentDestination: ScreenDestination,
    onDestinationSelected: (ScreenDestination) -> Unit,
    onCloseDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp)
    ) {
        // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_work_time),
                                    contentDescription = stringResource(R.string.content_desc_app_icon),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.menu_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Main Navigation Items
            DrawerMenuItem(
                label = stringResource(R.string.menu_dashboard),
                icon = Icons.Default.Dashboard,
                isSelected = currentDestination == ScreenDestination.DASHBOARD,
                testTag = "drawer_item_dashboard",
                onClick = {
                    onDestinationSelected(ScreenDestination.DASHBOARD)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = stringResource(R.string.menu_history),
                icon = Icons.Default.History,
                isSelected = currentDestination == ScreenDestination.HISTORY,
                testTag = "drawer_item_history",
                onClick = {
                    onDestinationSelected(ScreenDestination.HISTORY)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = stringResource(R.string.menu_insights),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                isSelected = currentDestination == ScreenDestination.INSIGHTS,
                testTag = "drawer_item_insights",
                onClick = {
                    onDestinationSelected(ScreenDestination.INSIGHTS)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = stringResource(R.string.menu_mental_wellbeing),
                icon = Icons.Default.SelfImprovement,
                isSelected = currentDestination == ScreenDestination.MENTAL_WELLBEING,
                testTag = "drawer_item_mental_wellbeing",
                onClick = {
                    onDestinationSelected(ScreenDestination.MENTAL_WELLBEING)
                    onCloseDrawer()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Text(
                text = stringResource(R.string.menu_section_settings),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
            )

            DrawerMenuItem(
                label = stringResource(R.string.menu_default_schedule),
                icon = Icons.Default.CalendarMonth,
                isSelected = currentDestination == ScreenDestination.DEFAULT_SCHEDULES,
                testTag = "drawer_item_default_schedule",
                onClick = {
                    onDestinationSelected(ScreenDestination.DEFAULT_SCHEDULES)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = stringResource(R.string.menu_buffer_grace_time),
                icon = Icons.Default.Tune,
                isSelected = currentDestination == ScreenDestination.SETTINGS,
                testTag = "drawer_item_settings",
                onClick = {
                    onDestinationSelected(ScreenDestination.SETTINGS)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = stringResource(R.string.menu_backup_restore),
                icon = Icons.Default.Storage,
                isSelected = currentDestination == ScreenDestination.BACKUP_RESTORE,
                testTag = "drawer_item_backup_restore",
                onClick = {
                    onDestinationSelected(ScreenDestination.BACKUP_RESTORE)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = stringResource(R.string.menu_about),
                icon = Icons.Default.Info,
                isSelected = currentDestination == ScreenDestination.ABOUT,
                testTag = "drawer_item_about",
                onClick = {
                    onDestinationSelected(ScreenDestination.ABOUT)
                    onCloseDrawer()
                }
            )

            // Footer info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.menu_footer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

@Composable
private fun DrawerMenuItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val view = LocalView.current
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        selected = isSelected,
        onClick = {
            triggerTouchSound(view)
            onClick()
        },
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag(testTag),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
