package com.strangeinfinity.notes.core.data

import android.net.Uri
import com.strangeinfinity.notes.core.database.NotesDatabase
import com.strangeinfinity.notes.core.database.entity.AttachmentEntity
import com.strangeinfinity.notes.core.database.entity.FolderEntity
import com.strangeinfinity.notes.core.database.entity.NoteEntity
import com.strangeinfinity.notes.core.database.entity.NoteTagCrossRef
import com.strangeinfinity.notes.core.database.entity.TagEntity
import com.strangeinfinity.notes.core.model.Attachment
import com.strangeinfinity.notes.core.model.Folder
import com.strangeinfinity.notes.core.model.Note
import com.strangeinfinity.notes.core.model.NoteWithDetails
import com.strangeinfinity.notes.core.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class NotesRepository(
    private val database: NotesDatabase,
    private val attachmentManager: AttachmentManager
) {
    private val noteDao = database.noteDao()
    private val folderDao = database.folderDao()
    private val tagDao = database.tagDao()
    private val attachmentDao = database.attachmentDao()

    fun getActiveNotesFlow(): Flow<List<NoteWithDetails>> {
        return combine(
            noteDao.getAllActiveNotesFlow(),
            folderDao.getAllFoldersFlow()
        ) { noteEntities, folderEntities ->
            val foldersById = folderEntities.associate { it.id to it.toDomain() }
            noteEntities.map { entity ->
                val note = entity.toDomain()
                val folder = note.folderId?.let { foldersById[it] }
                NoteWithDetails(
                    note = note,
                    folder = folder,
                    tags = emptyList(),
                    attachments = emptyList()
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getTrashedNotesFlow(): Flow<List<Note>> {
        return noteDao.getTrashedNotesFlow().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getNoteWithDetails(id: String): NoteWithDetails? = withContext(Dispatchers.IO) {
        val noteEntity = noteDao.getNoteById(id) ?: return@withContext null
        val note = noteEntity.toDomain()
        val folder = note.folderId?.let { folderDao.getFolderById(it)?.toDomain() }
        val tags = tagDao.getTagsForNote(id).map { it.toDomain() }
        val attachments = attachmentDao.getAttachmentsForNote(id).map { it.toDomain() }

        NoteWithDetails(
            note = note,
            folder = folder,
            tags = tags,
            attachments = attachments
        )
    }

    fun getNoteWithDetailsFlow(id: String): Flow<NoteWithDetails?> {
        return combine(
            noteDao.getNoteByIdFlow(id),
            folderDao.getAllFoldersFlow(),
            tagDao.getTagsForNoteFlow(id),
            attachmentDao.getAttachmentsForNoteFlow(id)
        ) { noteEntity, folderEntities, tagEntities, attachmentEntities ->
            if (noteEntity == null) return@combine null
            val note = noteEntity.toDomain()
            val folder = note.folderId?.let { fid ->
                folderEntities.firstOrNull { it.id == fid }?.toDomain()
            }
            NoteWithDetails(
                note = note,
                folder = folder,
                tags = tagEntities.map { it.toDomain() },
                attachments = attachmentEntities.map { it.toDomain() }
            )
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        val entity = NoteEntity.fromDomain(note.copy(updatedAt = System.currentTimeMillis()))
        noteDao.insert(entity)
    }

    suspend fun duplicateNote(noteId: String): String = withContext(Dispatchers.IO) {
        val original = noteDao.getNoteById(noteId) ?: throw IllegalArgumentException("Note not found: $noteId")
        val newId = UUID.randomUUID().toString()
        val copiedNote = original.copy(
            id = newId,
            title = if (original.title.isNotBlank()) "${original.title} (Copy)" else "Untitled (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insert(copiedNote)

        // Copy tag associations
        val tags = tagDao.getTagsForNote(noteId)
        tags.forEach { tag ->
            tagDao.insertCrossRef(NoteTagCrossRef(noteId = newId, tagId = tag.id))
        }

        newId
    }

    suspend fun setTrashState(noteIds: List<String>, isTrashed: Boolean) = withContext(Dispatchers.IO) {
        val trashedAt = if (isTrashed) System.currentTimeMillis() else null
        noteDao.setTrashState(noteIds, isTrashed, trashedAt)
    }

    suspend fun setPinState(noteIds: List<String>, isPinned: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setPinState(noteIds, isPinned)
    }

    suspend fun moveToFolder(noteIds: List<String>, folderId: String?) = withContext(Dispatchers.IO) {
        noteDao.moveToFolder(noteIds, folderId)
    }

    suspend fun deletePermanently(noteIds: List<String>) = withContext(Dispatchers.IO) {
        noteIds.forEach { id ->
            val attachments = attachmentDao.getAttachmentsForNote(id)
            attachments.forEach { att ->
                attachmentManager.deleteAttachmentFile(att.toDomain())
            }
            attachmentDao.deleteForNote(id)
            tagDao.deleteCrossRefsForNote(id)
            noteDao.deleteById(id)
        }
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val trashed = noteDao.getTrashedNotes()
        deletePermanently(trashed.map { it.id })
    }

    // Folders
    fun getAllFoldersFlow(): Flow<List<Folder>> {
        return folderDao.getAllFoldersFlow().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun saveFolder(folder: Folder) = withContext(Dispatchers.IO) {
        folderDao.insert(FolderEntity.fromDomain(folder))
    }

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        noteDao.detachFolderNotes(folderId)
        folderDao.deleteById(folderId)
    }

    // Tags
    fun getAllTagsFlow(): Flow<List<Tag>> {
        return tagDao.getAllTagsFlow().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun addTagToNote(noteId: String, tagName: String): Tag = withContext(Dispatchers.IO) {
        val cleanName = tagName.trim().removePrefix("#")
        var tag = tagDao.getTagByName(cleanName)
        if (tag == null) {
            val newTag = TagEntity(id = UUID.randomUUID().toString(), name = cleanName)
            tagDao.insert(newTag)
            tag = newTag
        }
        tagDao.insertCrossRef(NoteTagCrossRef(noteId = noteId, tagId = tag.id))
        tag.toDomain()
    }

    suspend fun removeTagFromNote(noteId: String, tagId: String) = withContext(Dispatchers.IO) {
        tagDao.deleteCrossRef(noteId, tagId)
    }

    // Attachments
    suspend fun addAttachment(noteId: String, uri: Uri): Attachment = withContext(Dispatchers.IO) {
        val attachment = attachmentManager.saveAttachmentFromUri(noteId, uri)
        attachmentDao.insert(AttachmentEntity.fromDomain(attachment))
        attachment
    }

    suspend fun deleteAttachment(attachment: Attachment) = withContext(Dispatchers.IO) {
        attachmentManager.deleteAttachmentFile(attachment)
        attachmentDao.deleteById(attachment.id)
    }

    // Stats
    fun countActiveNotesFlow(): Flow<Int> = noteDao.countActiveNotesFlow()
    fun countTrashedNotesFlow(): Flow<Int> = noteDao.countTrashedNotesFlow()
    suspend fun countActiveNotes(): Int = noteDao.countActiveNotes()
    suspend fun countTrashedNotes(): Int = noteDao.countTrashedNotes()
    suspend fun calculateStorageUsed(): Long = attachmentManager.calculateTotalStorageUsed()
}
