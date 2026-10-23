package com.strangeinfinity.notes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.strangeinfinity.notes.core.backup.BackupManager
import com.strangeinfinity.notes.core.backup.ImportMode
import com.strangeinfinity.notes.core.data.AttachmentManager
import com.strangeinfinity.notes.core.database.NotesDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class BackupWorkflowInstrumentationTest {

    private lateinit var context: Context
    private lateinit var database: NotesDatabase
    private lateinit var attachmentManager: AttachmentManager
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = NotesDatabase.getInstance(context)
        attachmentManager = AttachmentManager(context)
        backupManager = BackupManager(context, database, attachmentManager)
    }

    @Test
    fun testCompleteBackupRoundTripAndFileVerification() {
        runBlocking {
            // 1. Export real encrypted backup to cache file
            val backupFile = File(context.cacheDir, "strangeinfinity_test_backup.json")
            val password = "StrangeInfinity2026Password!"
            val bytesWritten = withContext(Dispatchers.IO) {
                FileOutputStream(backupFile).use { fos ->
                    backupManager.exportBackup(fos, password, isEncrypted = true)
                }
            }
            assertThat(bytesWritten).isGreaterThan(0L)
            assertThat(backupFile.exists()).isTrue()

            // 2. Read envelope
            val envelope = backupFile.inputStream().use { backupManager.readEnvelope(it) }
            assertThat(envelope.format).isEqualTo("strangeinfinity.notes.backup")
            assertThat(envelope.ciphertext).isNotNull()
            assertThat(envelope.encryption?.algorithm).isEqualTo("AES-256-GCM")

            // 3. Verify that ciphertext does NOT expose any plaintext words
            val rawBackupContent = backupFile.readText()
            assertThat(rawBackupContent).doesNotContain("Architecture")
            assertThat(rawBackupContent).doesNotContain("StrangeInfinity Architecture")

            // 4. Decrypt & Preview
            val preview = backupManager.previewBackup(envelope, password)
            assertThat(preview.isEncrypted).isTrue()
            assertThat(preview.noteCount).isAtLeast(4)

            // 5. Restore in MERGE mode
            val payload = backupManager.decryptPayload(envelope, password)
            val restoreResult = backupManager.restorePayload(payload, ImportMode.MERGE)
            assertThat(restoreResult.notesRestored).isAtLeast(4)
            assertThat(restoreResult.mode).isEqualTo(ImportMode.MERGE)

            // 6. Restore in REPLACE mode
            val replaceResult = backupManager.restorePayload(payload, ImportMode.REPLACE)
            assertThat(replaceResult.notesRestored).isAtLeast(4)
            assertThat(replaceResult.mode).isEqualTo(ImportMode.REPLACE)

            // Verify active count in DB after replace
            val count = database.noteDao().countActiveNotes()
            assertThat(count).isAtLeast(4)
        }
    }
}
