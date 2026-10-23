package com.strangeinfinity.notes

import android.app.Application
import com.strangeinfinity.notes.core.backup.BackupManager
import com.strangeinfinity.notes.core.data.AttachmentManager
import com.strangeinfinity.notes.core.data.NotesRepository
import com.strangeinfinity.notes.core.data.PreferencesRepository
import com.strangeinfinity.notes.core.database.NotesDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotesApplication : Application() {
    val database: NotesDatabase by lazy { NotesDatabase.getInstance(this) }
    val attachmentManager: AttachmentManager by lazy { AttachmentManager(this) }
    val notesRepository: NotesRepository by lazy { NotesRepository(database, attachmentManager) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }
    val backupManager: BackupManager by lazy { BackupManager(this, database, attachmentManager) }

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // Asynchronously warm up SQLite database on a background IO thread
        appScope.launch {
            try {
                database.openHelper.readableDatabase
            } catch (e: Exception) {
                // Warmup fallback
            }
        }
    }
}
