package com.strangeinfinity.notes.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.strangeinfinity.notes.core.model.Attachment
import com.strangeinfinity.notes.core.model.Folder
import com.strangeinfinity.notes.core.model.Note
import com.strangeinfinity.notes.core.model.Tag

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["isPinned"]),
        Index(value = ["isTrashed"]),
        Index(value = ["updatedAt"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val folderId: String?,
    val isPinned: Boolean,
    val isTrashed: Boolean,
    val trashedAt: Long?,
    val colorIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val checklistJson: String?,
    val drawingJson: String?
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        folderId = folderId,
        isPinned = isPinned,
        isTrashed = isTrashed,
        trashedAt = trashedAt,
        colorIndex = colorIndex,
        createdAt = createdAt,
        updatedAt = updatedAt,
        checklistJson = checklistJson,
        drawingJson = drawingJson
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            folderId = note.folderId,
            isPinned = note.isPinned,
            isTrashed = note.isTrashed,
            trashedAt = note.trashedAt,
            colorIndex = note.colorIndex,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            checklistJson = note.checklistJson,
            drawingJson = note.drawingJson
        )
    }
}

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: Long,
    val createdAt: Long
) {
    fun toDomain(): Folder = Folder(
        id = id,
        name = name,
        color = color,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(folder: Folder): FolderEntity = FolderEntity(
            id = folder.id,
            name = folder.name,
            color = folder.color,
            createdAt = folder.createdAt
        )
    }
}

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String
) {
    fun toDomain(): Tag = Tag(
        id = id,
        name = name
    )

    companion object {
        fun fromDomain(tag: Tag): TagEntity = TagEntity(
            id = tag.id,
            name = tag.name
        )
    }
}

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["tagId"])
    ]
)
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String
)

@Entity(
    tableName = "attachments",
    indices = [Index(value = ["noteId"])]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val relativePath: String,
    val fileSize: Long,
    val createdAt: Long
) {
    fun toDomain(): Attachment = Attachment(
        id = id,
        noteId = noteId,
        fileName = fileName,
        mimeType = mimeType,
        relativePath = relativePath,
        fileSize = fileSize,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(attachment: Attachment): AttachmentEntity = AttachmentEntity(
            id = attachment.id,
            noteId = attachment.noteId,
            fileName = attachment.fileName,
            mimeType = attachment.mimeType,
            relativePath = attachment.relativePath,
            fileSize = attachment.fileSize,
            createdAt = attachment.createdAt
        )
    }
}
