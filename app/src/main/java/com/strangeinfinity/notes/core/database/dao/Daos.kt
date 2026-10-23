package com.strangeinfinity.notes.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.strangeinfinity.notes.core.database.entity.AttachmentEntity
import com.strangeinfinity.notes.core.database.entity.FolderEntity
import com.strangeinfinity.notes.core.database.entity.NoteEntity
import com.strangeinfinity.notes.core.database.entity.NoteTagCrossRef
import com.strangeinfinity.notes.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllActiveNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesRaw(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun getTrashedNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    suspend fun getTrashedNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteByIdFlow(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolderFlow(folderId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotesFlow(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE notes SET isTrashed = :isTrashed, trashedAt = :trashedAt WHERE id IN (:ids)")
    suspend fun setTrashState(ids: List<String>, isTrashed: Boolean, trashedAt: Long?)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id IN (:ids)")
    suspend fun setPinState(ids: List<String>, isPinned: Boolean)

    @Query("UPDATE notes SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun moveToFolder(ids: List<String>, folderId: String?)

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun detachFolderNotes(folderId: String)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("SELECT COUNT(*) FROM notes WHERE isTrashed = 0")
    fun countActiveNotesFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE isTrashed = 0")
    suspend fun countActiveNotes(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isTrashed = 1")
    fun countTrashedNotesFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE isTrashed = 1")
    suspend fun countTrashedNotes(): Int

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFoldersFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY name ASC")
    suspend fun getAllFolders(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    @Update
    suspend fun update(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTagsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: NoteTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<NoteTagCrossRef>)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun deleteCrossRef(noteId: String, tagId: String)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteCrossRefsForNote(noteId: String)

    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tagId
        WHERE note_tag_cross_ref.noteId = :noteId
        ORDER BY tags.name ASC
    """)
    fun getTagsForNoteFlow(noteId: String): Flow<List<TagEntity>>

    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tagId
        WHERE note_tag_cross_ref.noteId = :noteId
        ORDER BY tags.name ASC
    """)
    suspend fun getTagsForNote(noteId: String): List<TagEntity>

    @Query("SELECT * FROM note_tag_cross_ref")
    suspend fun getAllCrossRefs(): List<NoteTagCrossRef>

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM note_tag_cross_ref")
    suspend fun deleteAllCrossRefs()
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getAttachmentsForNoteFlow(noteId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC")
    suspend fun getAttachmentsForNote(noteId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments ORDER BY createdAt ASC")
    suspend fun getAllAttachments(): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<AttachmentEntity>)

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun deleteForNote(noteId: String)

    @Query("DELETE FROM attachments")
    suspend fun deleteAllAttachments()
}
