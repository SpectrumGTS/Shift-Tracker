package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Divider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ScreenDestination

@Composable
fun AppNavigationDrawerSheet(
    currentDestination: ScreenDestination,
    onDestinationSelected: (ScreenDestination) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(310.dp)
            .testTag("navigation_drawer_sheet"),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
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
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "App Icon",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Shift Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Work-Life Balance",
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
                label = "Dashboard & Log",
                icon = Icons.Default.Dashboard,
                isSelected = currentDestination == ScreenDestination.DASHBOARD,
                testTag = "drawer_item_dashboard",
                onClick = {
                    onDestinationSelected(ScreenDestination.DASHBOARD)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Shift History",
                icon = Icons.Default.History,
                isSelected = currentDestination == ScreenDestination.HISTORY,
                testTag = "drawer_item_history",
                onClick = {
                    onDestinationSelected(ScreenDestination.HISTORY)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Yearly Insights",
                icon = Icons.Default.TrendingUp,
                isSelected = currentDestination == ScreenDestination.INSIGHTS,
                testTag = "drawer_item_insights",
                onClick = {
                    onDestinationSelected(ScreenDestination.INSIGHTS)
                    onCloseDrawer()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Text(
                text = "SETTINGS & PREFERENCES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
            )

            DrawerMenuItem(
                label = "Default Schedule",
                icon = Icons.Default.CalendarMonth,
                isSelected = currentDestination == ScreenDestination.DEFAULT_SCHEDULES,
                testTag = "drawer_item_default_schedule",
                onClick = {
                    onDestinationSelected(ScreenDestination.DEFAULT_SCHEDULES)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Buffer & Grace Time",
                icon = Icons.Default.Tune,
                isSelected = currentDestination == ScreenDestination.SETTINGS,
                testTag = "drawer_item_settings",
                onClick = {
                    onDestinationSelected(ScreenDestination.SETTINGS)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Backup & Restore (CSV)",
                icon = Icons.Default.Storage,
                isSelected = currentDestination == ScreenDestination.BACKUP_RESTORE,
                testTag = "drawer_item_backup_restore",
                onClick = {
                    onDestinationSelected(ScreenDestination.BACKUP_RESTORE)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "About This App",
                icon = Icons.Default.Info,
                isSelected = currentDestination == ScreenDestination.ABOUT,
                testTag = "drawer_item_about",
                onClick = {
                    onDestinationSelected(ScreenDestination.ABOUT)
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Room Offline Persistence • M3 Native",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
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
        onClick = onClick,
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
