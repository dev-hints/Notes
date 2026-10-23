package com.strangeinfinity.notes.core.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.strangeinfinity.notes.core.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.UUID

class AttachmentManager(private val context: Context) {
    private val attachmentsDir: File
        get() = File(context.filesDir, "attachments").apply {
            if (!exists()) mkdirs()
        }

    suspend fun saveAttachmentFromUri(noteId: String, uri: Uri): Attachment = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var fileName = "attachment_${System.currentTimeMillis()}"
        var mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        var fileSize = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) fileName = name
                }
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        val attachmentId = UUID.randomUUID().toString()
        val extension = fileName.substringAfterLast('.', "")
        val safeFileName = if (extension.isNotBlank()) "$attachmentId.$extension" else attachmentId
        val destinationFile = File(attachmentsDir, safeFileName)

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not open input stream from uri: $uri")

        if (fileSize <= 0) {
            fileSize = destinationFile.length()
        }

        Attachment(
            id = attachmentId,
            noteId = noteId,
            fileName = fileName,
            mimeType = mimeType,
            relativePath = safeFileName,
            fileSize = fileSize,
            createdAt = System.currentTimeMillis()
        )
    }

    suspend fun getFileForAttachment(attachment: Attachment): File = withContext(Dispatchers.IO) {
        File(attachmentsDir, attachment.relativePath)
    }

    suspend fun getAttachmentBytesBase64(attachment: Attachment): String? = withContext(Dispatchers.IO) {
        val file = File(attachmentsDir, attachment.relativePath)
        if (!file.exists() || !file.canRead()) return@withContext null
        val bytes = file.readBytes()
        Base64.getEncoder().encodeToString(bytes)
    }

    suspend fun restoreAttachmentBytes(relativePath: String, base64Data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(attachmentsDir, relativePath)
            val bytes = Base64.getDecoder().decode(base64Data)
            file.outputStream().use { it.write(bytes) }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteAttachmentFile(attachment: Attachment): Boolean = withContext(Dispatchers.IO) {
        val file = File(attachmentsDir, attachment.relativePath)
        if (file.exists()) file.delete() else true
    }

    suspend fun calculateTotalStorageUsed(): Long = withContext(Dispatchers.IO) {
        var total = 0L
        attachmentsDir.listFiles()?.forEach { file ->
            if (file.isFile) total += file.length()
        }
        total
    }

    suspend fun deleteAllAttachmentFiles() = withContext(Dispatchers.IO) {
        attachmentsDir.listFiles()?.forEach { it.delete() }
    }
}
