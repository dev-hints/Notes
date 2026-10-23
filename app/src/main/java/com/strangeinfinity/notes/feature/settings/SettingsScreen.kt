package com.strangeinfinity.notes.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strangeinfinity.notes.core.designsystem.StrangeInfinityBrandLogo
import com.strangeinfinity.notes.core.model.LayoutMode
import com.strangeinfinity.notes.core.model.SortOrder
import com.strangeinfinity.notes.core.model.ThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // APPEARANCE & PREFERENCES
            Text(
                "Appearance & Layout",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    // Theme Setting
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "App Theme",
                        subtitle = when (uiState.themeConfig) {
                            ThemeConfig.SYSTEM -> "System default"
                            ThemeConfig.LIGHT -> "Light theme"
                            ThemeConfig.DARK -> "Dark theme"
                        },
                        onClick = { showThemeDialog = true }
                    )

                    // Default Layout
                    SettingsRow(
                        icon = if (uiState.layoutMode == LayoutMode.GRID) Icons.Default.GridView else Icons.Default.ViewAgenda,
                        title = "Default Home Layout",
                        subtitle = if (uiState.layoutMode == LayoutMode.GRID) "Staggered Grid" else "List View",
                        onClick = {
                            val newMode = if (uiState.layoutMode == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID
                            viewModel.setLayoutMode(newMode)
                        }
                    )

                    // Default Sort Order
                    SettingsRow(
                        icon = Icons.Default.Sort,
                        title = "Default Note Sorting",
                        subtitle = when (uiState.sortOrder) {
                            SortOrder.MODIFIED_DESC -> "Last modified (Newest first)"
                            SortOrder.MODIFIED_ASC -> "Last modified (Oldest first)"
                            SortOrder.CREATED_DESC -> "Date created (Newest first)"
                            SortOrder.CREATED_ASC -> "Date created (Oldest first)"
                            SortOrder.TITLE_ASC -> "Title (A-Z)"
                            SortOrder.TITLE_DESC -> "Title (Z-A)"
                        },
                        onClick = { showSortDialog = true }
                    )
                }
            }

            // DATA & STORAGE
            Text(
                "Data & Storage",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    // Backup & Restore
                    SettingsRow(
                        icon = Icons.Default.Backup,
                        title = "Backup & Restore",
                        subtitle = "Password-protected encrypted JSON backups",
                        onClick = onNavigateToBackup
                    )

                    // Trash
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        title = "Trash / Recycle Bin",
                        subtitle = "${uiState.trashedNotesCount} notes in trash",
                        onClick = onNavigateToTrash
                    )

                    // Storage stats
                    val kb = uiState.storageSizeBytes / 1024
                    val mb = kb / 1024f
                    val sizeFormatted = if (mb >= 1.0f) String.format("%.2f MB", mb) else "$kb KB"

                    SettingsRow(
                        icon = Icons.Default.Storage,
                        title = "Local Storage Usage",
                        subtitle = "${uiState.activeNotesCount} notes, $sizeFormatted attachments",
                        onClick = viewModel::refreshStorageSize
                    )
                }
            }

            // PRIVACY & ABOUT
            Text(
                "About & Security",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Information",
                        subtitle = "100% offline, zero cloud, zero tracking",
                        onClick = onNavigateToPrivacy
                    )

                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "About Notes",
                        subtitle = "Notes by StrangeInfinity • v1.0.0",
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            // Brand Footer
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StrangeInfinityBrandLogo(size = 28)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "StrangeInfinity",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select App Theme") },
            text = {
                Column {
                    ThemeConfig.values().forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.themeConfig == theme,
                                onClick = {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                when (theme) {
                                    ThemeConfig.SYSTEM -> "System default"
                                    ThemeConfig.LIGHT -> "Light theme"
                                    ThemeConfig.DARK -> "Dark theme"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sort Dialog
    if (showSortDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Default Sorting") },
            text = {
                Column {
                    SortOrder.values().forEach { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setSortOrder(order)
                                    showSortDialog = false
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.sortOrder == order,
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (order) {
                                    SortOrder.MODIFIED_DESC -> "Last modified (Newest)"
                                    SortOrder.MODIFIED_ASC -> "Last modified (Oldest)"
                                    SortOrder.CREATED_DESC -> "Date created (Newest)"
                                    SortOrder.CREATED_ASC -> "Date created (Oldest)"
                                    SortOrder.TITLE_ASC -> "Title (A-Z)"
                                    SortOrder.TITLE_DESC -> "Title (Z-A)"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSortDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StrangeInfinityBrandLogo(size = 32)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Notes", fontWeight = FontWeight.Bold)
                        Text(
                            "by StrangeInfinity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version 1.0.0 (Production Release)", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "An original, distraction-free note taking experience with rich formatting, responsive handwriting canvas, and AES-256-GCM encrypted backup security.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• 100% Offline & Private\n• Zero Network Permissions\n• Zero Ads, Trackers, or Analytics\n• Sandboxed Local Room Database",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
    }
}
