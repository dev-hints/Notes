package com.strangeinfinity.notes.core.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.strangeinfinity.notes.core.data.AttachmentManager
import com.strangeinfinity.notes.core.database.NotesDatabase
import com.strangeinfinity.notes.core.database.entity.AttachmentEntity
import com.strangeinfinity.notes.core.database.entity.FolderEntity
import com.strangeinfinity.notes.core.database.entity.NoteEntity
import com.strangeinfinity.notes.core.database.entity.NoteTagCrossRef
import com.strangeinfinity.notes.core.database.entity.TagEntity
import com.strangeinfinity.notes.core.model.Attachment
import com.strangeinfinity.notes.core.security.BackupCrypto
import com.strangeinfinity.notes.core.security.InvalidBackupCredentialsOrCorruptedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BackupManager(
    private val context: Context,
    private val database: NotesDatabase,
    private val attachmentManager: AttachmentManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    suspend fun exportBackup(
        outputStream: OutputStream,
        password: String?,
        isEncrypted: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val noteDao = database.noteDao()
        val folderDao = database.folderDao()
        val tagDao = database.tagDao()
        val attachmentDao = database.attachmentDao()

        // 1. Gather all data
        val notes = noteDao.getAllNotesRaw().map { it.toDomain() }
        val folders = folderDao.getAllFolders().map { it.toDomain() }
        val tags = tagDao.getAllTags().map { it.toDomain() }
        val crossRefs = tagDao.getAllCrossRefs().map { NoteTagRefPayload(it.noteId, it.tagId) }
        val attachments = attachmentDao.getAllAttachments().map { it.toDomain() }

        // Read attachment bytes
        val attachmentPayloads = attachments.map { attachment ->
            val base64 = attachmentManager.getAttachmentBytesBase64(attachment)
            AttachmentBackupData(
                id = attachment.id,
                noteId = attachment.noteId,
                fileName = attachment.fileName,
                mimeType = attachment.mimeType,
                fileSize = attachment.fileSize,
                createdAt = attachment.createdAt,
                dataBase64 = base64
            )
        }

        val payload = BackupPayload(
            schemaVersion = 1,
            app = "Notes by StrangeInfinity",
            exportedAt = getCurrentIsoTimestamp(),
            notes = notes,
            folders = folders,
            tags = tags,
            noteTagRefs = crossRefs,
            attachments = attachmentPayloads
        )

        val payloadJson = json.encodeToString(payload)

        val envelope = if (isEncrypted) {
            require(!password.isNullOrBlank()) { "A password is required for encrypted backups" }
            val encrypted = BackupCrypto.encrypt(payloadJson, password.toCharArray())
            BackupEnvelope(
                format = "strangeinfinity.notes.backup",
                formatVersion = 1,
                app = "Notes by StrangeInfinity",
                encryption = EncryptionHeader(
                    algorithm = encrypted.algorithm,
                    kdf = encrypted.kdf,
                    iterations = encrypted.iterations,
                    salt = encrypted.saltBase64,
                    nonce = encrypted.nonceBase64
                ),
                ciphertext = encrypted.ciphertextBase64,
                unencrypted = false,
                plainPayload = null
            )
        } else {
            BackupEnvelope(
                format = "strangeinfinity.notes.backup",
                formatVersion = 1,
                app = "Notes by StrangeInfinity",
                encryption = null,
                ciphertext = null,
                unencrypted = true,
                plainPayload = payload
            )
        }

        val finalJsonString = json.encodeToString(envelope)
        val bytes = finalJsonString.toByteArray(Charsets.UTF_8)
        outputStream.use { it.write(bytes) }
        bytes.size.toLong()
    }

    suspend fun readEnvelope(inputStream: InputStream): BackupEnvelope = withContext(Dispatchers.IO) {
        val content = inputStream.bufferedReader().use { it.readText() }
        if (content.length > 50 * 1024 * 1024) { // 50MB reasonable safety limit
            throw IllegalArgumentException("Backup file exceeds maximum allowed size (50MB)")
        }
        val envelope = json.decodeFromString<BackupEnvelope>(content)
        if (envelope.format != "strangeinfinity.notes.backup") {
            throw IllegalArgumentException("Invalid backup format: '${envelope.format}'")
        }
        if (envelope.formatVersion > 1) {
            throw IllegalArgumentException("Unsupported backup version: ${envelope.formatVersion}")
        }
        envelope
    }

    suspend fun decryptPayload(envelope: BackupEnvelope, password: String?): BackupPayload = withContext(Dispatchers.IO) {
        if (envelope.unencrypted) {
            return@withContext envelope.plainPayload
                ?: throw IllegalArgumentException("Unencrypted backup contains no payload")
        }

        val encryption = envelope.encryption
            ?: throw IllegalArgumentException("Missing encryption header in encrypted backup")
        val ciphertext = envelope.ciphertext
            ?: throw IllegalArgumentException("Missing ciphertext in encrypted backup")

        require(!password.isNullOrBlank()) { "Password is required to decrypt this backup" }

        val decryptedJson = BackupCrypto.decrypt(
            ciphertextBase64 = ciphertext,
            password = password.toCharArray(),
            saltBase64 = encryption.salt,
            nonceBase64 = encryption.nonce,
            iterations = encryption.iterations
        )

        json.decodeFromString<BackupPayload>(decryptedJson)
    }

    suspend fun previewBackup(envelope: BackupEnvelope, password: String?): BackupPreview = withContext(Dispatchers.IO) {
        val payload = decryptPayload(envelope, password)
        BackupPreview(
            exportedAt = payload.exportedAt,
            noteCount = payload.notes.size,
            folderCount = payload.folders.size,
            tagCount = payload.tags.size,
            attachmentCount = payload.attachments.size,
            isEncrypted = !envelope.unencrypted
        )
    }

    suspend fun restorePayload(payload: BackupPayload, mode: ImportMode): RestoreResult = withContext(Dispatchers.IO) {
        val noteDao = database.noteDao()
        val folderDao = database.folderDao()
        val tagDao = database.tagDao()
        val attachmentDao = database.attachmentDao()

        // Validate payload records
        payload.notes.forEach { note ->
            require(note.id.isNotBlank()) { "Note ID cannot be blank" }
        }

        // 1. Restore attachment files into private storage
        for (att in payload.attachments) {
            if (!att.dataBase64.isNullOrBlank()) {
                val extension = att.fileName.substringAfterLast('.', "")
                val relativePath = if (extension.isNotBlank()) "${att.id}.$extension" else att.id
                attachmentManager.restoreAttachmentBytes(relativePath, att.dataBase64)
            }
        }

        database.withTransaction {
            if (mode == ImportMode.REPLACE) {
                noteDao.deleteAllNotes()
                folderDao.deleteAllFolders()
                tagDao.deleteAllTags()
                tagDao.deleteAllCrossRefs()
                attachmentDao.deleteAllAttachments()
            }

            // Insert folders
            val folderEntities = payload.folders.map { FolderEntity.fromDomain(it) }
            folderDao.insertAll(folderEntities)

            // Insert tags
            val tagEntities = payload.tags.map { TagEntity.fromDomain(it) }
            tagDao.insertAll(tagEntities)

            // Insert notes
            val noteEntities = payload.notes.map { NoteEntity.fromDomain(it) }
            noteDao.insertAll(noteEntities)

            // Insert note-tag cross refs
            val crossRefs = payload.noteTagRefs.map { NoteTagCrossRef(it.noteId, it.tagId) }
            tagDao.insertCrossRefs(crossRefs)

            // Insert attachments
            val attachmentEntities = payload.attachments.map {
                val extension = it.fileName.substringAfterLast('.', "")
                val relativePath = if (extension.isNotBlank()) "${it.id}.$extension" else it.id
                AttachmentEntity(
                    id = it.id,
                    noteId = it.noteId,
                    fileName = it.fileName,
                    mimeType = it.mimeType,
                    relativePath = relativePath,
                    fileSize = it.fileSize,
                    createdAt = it.createdAt
                )
            }
            attachmentDao.insertAll(attachmentEntities)
        }

        RestoreResult(
            notesRestored = payload.notes.size,
            foldersRestored = payload.folders.size,
            tagsRestored = payload.tags.size,
            attachmentsRestored = payload.attachments.size,
            mode = mode
        )
    }
}
