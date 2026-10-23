package com.strangeinfinity.notes.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeinfinity.notes.core.backup.ImportMode
import com.strangeinfinity.notes.core.designsystem.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // SAF Launchers
    val exportEncryptedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.performExport(context, uri, isEncrypted = true)
        }
    }

    val exportPlainLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.performExport(context, uri, isEncrypted = false)
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onFileSelectedForImport(context, uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = { Text("Backup & Restore") },
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
            // Status Banner
            when (val status = uiState.status) {
                is BackupStatus.Loading -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(status.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is BackupStatus.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(status.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is BackupStatus.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                status.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                is BackupStatus.Idle -> {}
            }

            // SECTION 1: EXPORT BACKUP
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Export Notes Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Export all notes, folders, tags, handwritten sketches, and photos into a password-protected encrypted JSON file (AES-256-GCM).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password input
                    OutlinedTextField(
                        value = uiState.exportPassword,
                        onValueChange = viewModel::onExportPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Backup Password") },
                        singleLine = true,
                        visualTransformation = if (uiState.isExportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = viewModel::toggleExportPasswordVisibility) {
                                Icon(
                                    imageVector = if (uiState.isExportPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirm Password input
                    OutlinedTextField(
                        value = uiState.exportPasswordConfirm,
                        onValueChange = viewModel::onExportPasswordConfirmChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = if (uiState.isExportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Export Encrypted Button
                    Button(
                        onClick = {
                            val defaultName = "Notes_StrangeInfinity_backup_${System.currentTimeMillis()}.json"
                            exportEncryptedLauncher.launch(defaultName)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Encrypted Backup (Recommended)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Plain Export Button
                    TextButton(
                        onClick = { viewModel.showPlainExportWarning(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Export Unencrypted Plain JSON...",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // SECTION 2: IMPORT BACKUP
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            viewModel.setPreviewForTesting(
                                preview = com.strangeinfinity.notes.core.backup.BackupPreview(
                                    exportedAt = "2026-10-23T12:00:00Z",
                                    noteCount = 4,
                                    folderCount = 3,
                                    tagCount = 4,
                                    attachmentCount = 1,
                                    isEncrypted = true
                                ),
                                payload = com.strangeinfinity.notes.core.backup.BackupPayload(
                                    schemaVersion = 1,
                                    app = "Notes by StrangeInfinity",
                                    exportedAt = "2026-10-23T12:00:00Z"
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Restore Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Select a StrangeInfinity JSON backup file to restore your notes. Your current notes will remain intact unless you explicitly choose to replace them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Backup File to Restore")
                    }
                }
            }
        }
    }

    // Dialog: Password Prompt for Encrypted Import
    if (uiState.isPasswordPromptOpen) {
        AlertDialog(
            onDismissRequest = viewModel::cancelPasswordPrompt,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Encrypted Backup")
                }
            },
            text = {
                Column {
                    Text(
                        "This backup is protected with AES-256-GCM encryption. Enter the password used during export to decrypt and restore it.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.importPassword,
                        onValueChange = viewModel::onImportPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (uiState.isImportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = viewModel::toggleImportPasswordVisibility) {
                                Icon(
                                    imageVector = if (uiState.isImportPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::submitImportPassword) {
                    Text("Decrypt & Preview")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPasswordPrompt) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Restore Preview & Mode Selection (Merge vs Replace)
    if (uiState.showRestoreConfirmDialog && uiState.preview != null) {
        val preview = uiState.preview!!
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreConfirm,
            title = { Text("Restore Backup Preview") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Exported at: ${preview.exportedAt}", style = MaterialTheme.typography.bodySmall)
                    Text("Notes found: ${preview.noteCount}", fontWeight = FontWeight.SemiBold)
                    Text("Folders found: ${preview.folderCount}")
                    Text("Tags found: ${preview.tagCount}")
                    Text("Attachments found: ${preview.attachmentCount}")

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Choose Restore Mode:", fontWeight = FontWeight.Bold)

                    // Option 1: Merge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setImportMode(ImportMode.MERGE) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedImportMode == ImportMode.MERGE,
                            onClick = { viewModel.setImportMode(ImportMode.MERGE) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Merge", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Keep existing notes; add new notes and update matching records safely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Replace
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setImportMode(ImportMode.REPLACE) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedImportMode == ImportMode.REPLACE,
                            onClick = { viewModel.setImportMode(ImportMode.REPLACE) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Replace (Overwrite)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            Text(
                                "Warning: Wipes current notes and completely replaces them with this backup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::executeRestore,
                    colors = if (uiState.selectedImportMode == ImportMode.REPLACE) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (uiState.selectedImportMode == ImportMode.REPLACE) "Replace All Notes" else "Merge Notes")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRestoreConfirm) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Warning for Plain JSON export
    if (uiState.showPlainExportWarning) {
        ConfirmDialog(
            title = "Export Plain JSON?",
            message = "Unencrypted backups store your notes, checklists, and sketches in readable plain text without password protection. Anyone who accesses this file will be able to read it. Are you sure?",
            confirmText = "Export Plain JSON",
            isDestructive = true,
            onConfirm = {
                viewModel.showPlainExportWarning(false)
                val defaultName = "Notes_StrangeInfinity_plain_${System.currentTimeMillis()}.json"
                exportPlainLauncher.launch(defaultName)
            },
            onDismiss = { viewModel.showPlainExportWarning(false) }
        )
    }
}
