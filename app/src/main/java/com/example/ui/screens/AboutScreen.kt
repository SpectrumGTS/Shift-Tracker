package com.example.ui.screens

import com.example.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("about_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "About This App",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Learn about the architecture, framework, and licensing powering Shift Tracker, designed to support work-life balance and mental wellbeing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tech Stack Section Header
            item {
                Text(
                    text = "Technology Stack",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Material 3 UI Card
            item {
                AboutTechCard(
                    title = "Native Material 3 UI",
                    description = "This app is built using modern Jetpack Compose and strictly adheres to Material Design 3 guidelines. It features elegant component styling, support for edge-to-edge rendering, accessible touch targets, and a cohesive theme utilizing M3 tonal elevations and generous negative space.",
                    icon = Icons.Default.Palette
                )
            }

            // Kotlin Language Card
            item {
                AboutTechCard(
                    title = "Kotlin Language",
                    description = "The entire codebase is written in Kotlin. It leverages Kotlin's advanced features, including absolute type safety, structured concurrency with Coroutines and Flow for seamless reactive state updates, and standard modern coding practices.",
                    icon = Icons.Default.Code
                )
            }

            // Room Database Card
            item {
                AboutTechCard(
                    title = "Offline Room Database",
                    description = "Your data stays private and secure. The application integrates an offline SQLite database via Android Jetpack Room. This ensures reliable offline data persistence, atomic database transactions, and smooth local state management without relying on external cloud APIs.",
                    icon = Icons.Default.Storage
                )
            }

            // License Header
            item {
                Text(
                    text = "Licensing & Legal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // License Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "GNU GPL v3 License Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "This application is released under the GNU General Public License v3 (GPL v3). Below is a simplified summary of the license terms:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                        LicenseBulletPoint(
                            title = "Permissions",
                            detail = "You are permitted to run, copy, distribute, modify, and convey the program commercially or privately, provided that you preserve the author attributes, license notices, and make the source code available."
                        )

                        LicenseBulletPoint(
                            title = "Copyleft Rule",
                            detail = "Modified versions of the source code must also be licensed under the GPL v3. Anyone who distributes modified versions of the software must release their source code under the same terms."
                        )

                        LicenseBulletPoint(
                            title = "No Warranty",
                            detail = "This software is provided 'as is' without any warranty of any kind. The authors and copyright holders are not liable for any damages or issues resulting from its use."
                        )
                    }
                }
            }

            // Version & Build Info Header
            item {
                Text(
                    text = "Version & Build Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Version & Build Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BuildInfoRow(label = "Application ID", value = BuildConfig.APPLICATION_ID)
                        BuildInfoRow(label = "Version", value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        BuildInfoRow(label = "Build Type", value = BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercase() })
                        BuildInfoRow(label = "Target SDK", value = "API 36")
                        BuildInfoRow(label = "Min SDK", value = "API 24")
                        BuildInfoRow(label = "Build Date", value = "August 10, 2026")
                    }
                }
            }
        }
    }
}

@Composable
fun BuildInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AboutTechCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LicenseBulletPoint(title: String, detail: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
