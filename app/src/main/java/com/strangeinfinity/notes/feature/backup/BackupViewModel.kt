package com.strangeinfinity.notes.feature.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strangeinfinity.notes.core.backup.BackupEnvelope
import com.strangeinfinity.notes.core.backup.BackupManager
import com.strangeinfinity.notes.core.backup.BackupPayload
import com.strangeinfinity.notes.core.backup.BackupPreview
import com.strangeinfinity.notes.core.backup.ImportMode
import com.strangeinfinity.notes.core.backup.RestoreResult
import com.strangeinfinity.notes.core.security.InvalidBackupCredentialsOrCorruptedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BackupStatus {
    object Idle : BackupStatus()
    data class Loading(val message: String) : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

data class BackupUiState(
    val exportPassword: String = "",
    val exportPasswordConfirm: String = "",
    val isExportPasswordVisible: Boolean = false,
    val showPlainExportWarning: Boolean = false,
    val importPassword: String = "",
    val isImportPasswordVisible: Boolean = false,
    val isPasswordPromptOpen: Boolean = false,
    val pendingEnvelope: BackupEnvelope? = null,
    val decryptedPayload: BackupPayload? = null,
    val preview: BackupPreview? = null,
    val showRestoreConfirmDialog: Boolean = false,
    val selectedImportMode: ImportMode = ImportMode.MERGE,
    val status: BackupStatus = BackupStatus.Idle
)

class BackupViewModel(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onExportPasswordChange(pw: String) {
        _uiState.value = _uiState.value.copy(exportPassword = pw)
    }

    fun onExportPasswordConfirmChange(pw: String) {
        _uiState.value = _uiState.value.copy(exportPasswordConfirm = pw)
    }

    fun toggleExportPasswordVisibility() {
        _uiState.value = _uiState.value.copy(isExportPasswordVisible = !_uiState.value.isExportPasswordVisible)
    }

    fun onImportPasswordChange(pw: String) {
        _uiState.value = _uiState.value.copy(importPassword = pw)
    }

    fun toggleImportPasswordVisibility() {
        _uiState.value = _uiState.value.copy(isImportPasswordVisible = !_uiState.value.isImportPasswordVisible)
    }

    fun showPlainExportWarning(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPlainExportWarning = show)
    }

    fun performExport(context: Context, destinationUri: Uri, isEncrypted: Boolean) {
        val state = _uiState.value
        if (isEncrypted) {
            if (state.exportPassword.isBlank()) {
                _uiState.value = state.copy(status = BackupStatus.Error("Please enter a backup password"))
                return
            }
            if (state.exportPassword != state.exportPasswordConfirm) {
                _uiState.value = state.copy(status = BackupStatus.Error("Passwords do not match"))
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = BackupStatus.Loading("Encrypting and exporting backup..."))
            try {
                val outputStream = context.contentResolver.openOutputStream(destinationUri)
                    ?: throw IllegalStateException("Could not open output destination")

                val bytesWritten = backupManager.exportBackup(
                    outputStream = outputStream,
                    password = if (isEncrypted) state.exportPassword else null,
                    isEncrypted = isEncrypted
                )

                _uiState.value = _uiState.value.copy(
                    status = BackupStatus.Success("Backup exported successfully (${bytesWritten / 1024} KB)"),
                    exportPassword = "",
                    exportPasswordConfirm = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = BackupStatus.Error("Export failed: ${e.message ?: "Unknown error"}")
                )
            }
        }
    }

    fun onFileSelectedForImport(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = BackupStatus.Loading("Reading backup file..."))
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                    ?: throw IllegalStateException("Could not open source file")

                val envelope = backupManager.readEnvelope(inputStream)
                if (envelope.unencrypted) {
                    val preview = backupManager.previewBackup(envelope, null)
                    val payload = backupManager.decryptPayload(envelope, null)
                    _uiState.value = _uiState.value.copy(
                        pendingEnvelope = envelope,
                        decryptedPayload = payload,
                        preview = preview,
                        showRestoreConfirmDialog = true,
                        status = BackupStatus.Idle
                    )
                } else {
                    // Encrypted: prompt for password
                    _uiState.value = _uiState.value.copy(
                        pendingEnvelope = envelope,
                        isPasswordPromptOpen = true,
                        importPassword = "",
                        status = BackupStatus.Idle
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = BackupStatus.Error("Failed to parse backup: ${e.message ?: "Invalid file"}")
                )
            }
        }
    }

    fun submitImportPassword() {
        val envelope = _uiState.value.pendingEnvelope ?: return
        val password = _uiState.value.importPassword
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(status = BackupStatus.Error("Password cannot be empty"))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = BackupStatus.Loading("Decrypting and verifying payload..."))
            try {
                val preview = backupManager.previewBackup(envelope, password)
                val payload = backupManager.decryptPayload(envelope, password)

                _uiState.value = _uiState.value.copy(
                    decryptedPayload = payload,
                    preview = preview,
                    isPasswordPromptOpen = false,
                    showRestoreConfirmDialog = true,
                    status = BackupStatus.Idle
                )
            } catch (e: InvalidBackupCredentialsOrCorruptedException) {
                _uiState.value = _uiState.value.copy(
                    status = BackupStatus.Error("Incorrect password or backup corrupted.")
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = BackupStatus.Error("Decryption error: ${e.message ?: "Authentication failed"}")
                )
            }
        }
    }

    fun cancelPasswordPrompt() {
        _uiState.value = _uiState.value.copy(
            isPasswordPromptOpen = false,
            pendingEnvelope = null,
            importPassword = ""
        )
    }

    fun setImportMode(mode: ImportMode) {
        _uiState.value = _uiState.value.copy(selectedImportMode = mode)
    }

    fun dismissRestoreConfirm() {
        _uiState.value = _uiState.value.copy(
            showRestoreConfirmDialog = false,
            pendingEnvelope = null,
            decryptedPayload = null,
            preview = null
        )
    }

    fun executeRestore() {
        val payload = _uiState.value.decryptedPayload ?: return
        val mode = _uiState.value.selectedImportMode

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = BackupStatus.Loading("Restoring notes and attachments..."),
                showRestoreConfirmDialog = false
            )
            try {
                val result = backupManager.restorePayload(payload, mode)
                _uiState.value = _uiState.value.copy(
                    status = BackupStatus.Success(
                        "Restored ${result.notesRestored} notes, ${result.foldersRestored} folders, and ${result.attachmentsRestored} attachments."
                    ),
                    pendingEnvelope = null,
                    decryptedPayload = null,
                    preview = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = BackupStatus.Error("Restore failed: ${e.message ?: "Database error"}")
                )
            }
        }
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(status = BackupStatus.Idle)
    }

    fun setPreviewForTesting(preview: BackupPreview, payload: BackupPayload) {
        _uiState.value = _uiState.value.copy(
            preview = preview,
            decryptedPayload = payload,
            showRestoreConfirmDialog = true
        )
    }

    companion object {
        fun provideFactory(backupManager: BackupManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BackupViewModel(backupManager) as T
                }
            }
    }
}
