package com.strangeinfinity.notes.core.backup

import com.strangeinfinity.notes.core.model.Folder
import com.strangeinfinity.notes.core.model.Note
import com.strangeinfinity.notes.core.model.Tag
import kotlinx.serialization.Serializable

@Serializable
data class EncryptionHeader(
    val algorithm: String,
    val kdf: String,
    val iterations: Int,
    val salt: String,
    val nonce: String
)

@Serializable
data class BackupEnvelope(
    val format: String = "strangeinfinity.notes.backup",
    val formatVersion: Int = 1,
    val app: String = "Notes by StrangeInfinity",
    val encryption: EncryptionHeader? = null,
    val ciphertext: String? = null,
    val unencrypted: Boolean = false,
    val plainPayload: BackupPayload? = null
)

@Serializable
data class NoteTagRefPayload(
    val noteId: String,
    val tagId: String
)

@Serializable
data class AttachmentBackupData(
    val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val createdAt: Long,
    val dataBase64: String? = null
)

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val app: String = "Notes by StrangeInfinity",
    val exportedAt: String,
    val notes: List<Note> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val noteTagRefs: List<NoteTagRefPayload> = emptyList(),
    val attachments: List<AttachmentBackupData> = emptyList()
)

data class BackupPreview(
    val exportedAt: String,
    val noteCount: Int,
    val folderCount: Int,
    val tagCount: Int,
    val attachmentCount: Int,
    val isEncrypted: Boolean
)

enum class ImportMode {
    MERGE,
    REPLACE
}

data class RestoreResult(
    val notesRestored: Int,
    val foldersRestored: Int,
    val tagsRestored: Int,
    val attachmentsRestored: Int,
    val mode: ImportMode
)
