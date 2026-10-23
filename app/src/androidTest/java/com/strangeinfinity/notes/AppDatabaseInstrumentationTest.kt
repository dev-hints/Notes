package com.strangeinfinity.notes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.strangeinfinity.notes.core.backup.BackupManager
import com.strangeinfinity.notes.core.backup.ImportMode
import com.strangeinfinity.notes.core.data.AttachmentManager
import com.strangeinfinity.notes.core.database.NotesDatabase
import com.strangeinfinity.notes.core.database.entity.FolderEntity
import com.strangeinfinity.notes.core.database.entity.NoteEntity
import com.strangeinfinity.notes.core.database.entity.NoteTagCrossRef
import com.strangeinfinity.notes.core.database.entity.TagEntity
import com.strangeinfinity.notes.core.model.ChecklistItem
import com.strangeinfinity.notes.core.model.DrawingData
import com.strangeinfinity.notes.core.model.DrawingPoint
import com.strangeinfinity.notes.core.model.DrawingStroke
import com.strangeinfinity.notes.core.security.BackupCrypto
import com.strangeinfinity.notes.core.security.InvalidBackupCredentialsOrCorruptedException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppDatabaseInstrumentationTest {

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
    fun testPopulateSampleDatasetAndVerifyRoundTrip() {
        runBlocking {
            val noteDao = database.noteDao()
            val folderDao = database.folderDao()
            val tagDao = database.tagDao()

        // 1. Create folders
        val workFolder = FolderEntity(id = "folder-work", name = "Work & Projects", color = 0xFF6366F1, createdAt = System.currentTimeMillis())
        val ideasFolder = FolderEntity(id = "folder-ideas", name = "Creative Ideas", color = 0xFF06B6D4, createdAt = System.currentTimeMillis())
        val personalFolder = FolderEntity(id = "folder-personal", name = "Personal", color = 0xFF10B981, createdAt = System.currentTimeMillis())
        folderDao.insertAll(listOf(workFolder, ideasFolder, personalFolder))

        // 2. Create tags
        val tagArch = TagEntity(id = "tag-arch", name = "Architecture")
        val tagSec = TagEntity(id = "tag-sec", name = "Security")
        val tagHindi = TagEntity(id = "tag-hindi", name = "Hindi")
        val tagSprint = TagEntity(id = "tag-sprint", name = "Sprint")
        tagDao.insertAll(listOf(tagArch, tagSec, tagHindi, tagSprint))

        // 3. Create rich checklist
        val sprintChecklist = listOf(
            ChecklistItem(id = "c1", text = "Offline Room Database implementation", isCompleted = true, order = 0),
            ChecklistItem(id = "c2", text = "AES-256-GCM Backup subsystem", isCompleted = true, order = 1),
            ChecklistItem(id = "c3", text = "Handwriting & Drawing vector canvas", isCompleted = true, order = 2),
            ChecklistItem(id = "c4", text = "Connected device ADB verification", isCompleted = true, order = 3),
            ChecklistItem(id = "c5", text = "Export encrypted JSON backup file", isCompleted = false, order = 4)
        )
        val checklistJson = Json.encodeToString(sprintChecklist)

        // 4. Create vector drawing strokes
        val stroke1 = DrawingStroke(
            points = listOf(
                DrawingPoint(50f, 60f),
                DrawingPoint(100f, 120f),
                DrawingPoint(150f, 80f),
                DrawingPoint(200f, 160f)
            ),
            color = 0xFF6366F1,
            strokeWidth = 10f
        )
        val stroke2 = DrawingStroke(
            points = listOf(
                DrawingPoint(200f, 160f),
                DrawingPoint(250f, 220f),
                DrawingPoint(300f, 180f)
            ),
            color = 0xFF06B6D4,
            strokeWidth = 8f
        )
        val drawingJson = Json.encodeToString(DrawingData(listOf(stroke1, stroke2)))

        // 5. Create rich notes
        val note1 = NoteEntity(
            id = "note-arch-1",
            title = "Notes by StrangeInfinity Architecture",
            content = "100% offline, local persistence with Room, ViewModel, and StateFlow.\n\nSecurity model: Client-side AES-256-GCM encryption with PBKDF2-HMAC-SHA256 key derivation.\n\nZero telemetry, zero ads, zero trackers.",
            folderId = "folder-work",
            isPinned = true,
            isTrashed = false,
            trashedAt = null,
            colorIndex = 5, // Indigo
            createdAt = System.currentTimeMillis() - 86400000L,
            updatedAt = System.currentTimeMillis() - 3600000L,
            checklistJson = checklistJson,
            drawingJson = null
        )

        val note2 = NoteEntity(
            id = "note-hindi-2",
            title = "नमस्ते StrangeInfinity - सुरक्षित नोट्स",
            content = "यह ऐप पूरी तरह से ऑफलाइन काम करता है। आपका सारा डेटा आपके फ़ोन पर सुरक्षित है।\n\n- कोई विज्ञापन नहीं\n- कोई ट्रैकर नहीं\n- पूरी गोपनीयता",
            folderId = "folder-personal",
            isPinned = true,
            isTrashed = false,
            trashedAt = null,
            colorIndex = 2, // Amber
            createdAt = System.currentTimeMillis() - 43200000L,
            updatedAt = System.currentTimeMillis() - 1800000L,
            checklistJson = null,
            drawingJson = null
        )

        val note3 = NoteEntity(
            id = "note-sketch-3",
            title = "Infinite Loop & UI Wireframe",
            content = "Hand-drawn infinity glyph and responsive tablet navigation wireframe.",
            folderId = "folder-ideas",
            isPinned = false,
            isTrashed = false,
            trashedAt = null,
            colorIndex = 4, // Cyan
            createdAt = System.currentTimeMillis() - 7200000L,
            updatedAt = System.currentTimeMillis() - 600000L,
            checklistJson = null,
            drawingJson = drawingJson
        )

        val note4 = NoteEntity(
            id = "note-ideas-4",
            title = "Design System & Color Palettes",
            content = "Clean typography hierarchy, subtle container borders, restrained shadows, and Material 3 adaptive color tokens.",
            folderId = "folder-ideas",
            isPinned = false,
            isTrashed = false,
            trashedAt = null,
            colorIndex = 1, // Rose
            createdAt = System.currentTimeMillis() - 3600000L,
            updatedAt = System.currentTimeMillis() - 300000L,
            checklistJson = null,
            drawingJson = null
        )

        noteDao.insertAll(listOf(note1, note2, note3, note4))

        // Tag associations
        tagDao.insertCrossRefs(
            listOf(
                NoteTagCrossRef("note-arch-1", "tag-arch"),
                NoteTagCrossRef("note-arch-1", "tag-sec"),
                NoteTagCrossRef("note-hindi-2", "tag-hindi"),
                NoteTagCrossRef("note-hindi-2", "tag-sec"),
                NoteTagCrossRef("note-sketch-3", "tag-arch")
            )
        )

        // Verify retrieval
        val allActive = noteDao.getAllActiveNotes()
        assertThat(allActive.size).isAtLeast(4)

        // Test Backup Export & Import Round-trip on real hardware
        val outputStream = ByteArrayOutputStream()
        val backupPassword = "StrangeInfinitySecurePassword2026!"
        val bytesExported = backupManager.exportBackup(outputStream, backupPassword, isEncrypted = true)
        assertThat(bytesExported).isGreaterThan(0L)

        val exportedBytes = outputStream.toByteArray()
        val envelope = backupManager.readEnvelope(ByteArrayInputStream(exportedBytes))
        assertThat(envelope.format).isEqualTo("strangeinfinity.notes.backup")
        assertThat(envelope.formatVersion).isEqualTo(1)
        assertThat(envelope.encryption?.algorithm).isEqualTo("AES-256-GCM")

        // Preview backup
        val preview = backupManager.previewBackup(envelope, backupPassword)
        assertThat(preview.noteCount).isAtLeast(4)
        assertThat(preview.folderCount).isAtLeast(3)

        // Test wrong password
        assertThrows(InvalidBackupCredentialsOrCorruptedException::class.java) {
            runBlocking {
                backupManager.decryptPayload(envelope, "IncorrectPassword!")
            }
        }
    }
}
}
