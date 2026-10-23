package com.strangeinfinity.notes.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val folderId: String? = null,
    val isPinned: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val checklistJson: String? = null,
    val drawingJson: String? = null
)

@Serializable
data class Folder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Long = 0xFF6366F1,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Tag(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)

@Serializable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val order: Int = 0
)

@Serializable
data class DrawingPoint(
    val x: Float,
    val y: Float
)

@Serializable
data class DrawingStroke(
    val points: List<DrawingPoint>,
    val color: Long,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

@Serializable
data class DrawingData(
    val strokes: List<DrawingStroke> = emptyList()
)

@Serializable
data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val relativePath: String,
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ThemeConfig {
    SYSTEM,
    LIGHT,
    DARK
}

enum class LayoutMode {
    GRID,
    LIST
}

enum class SortOrder {
    MODIFIED_DESC,
    MODIFIED_ASC,
    CREATED_DESC,
    CREATED_ASC,
    TITLE_ASC,
    TITLE_DESC
}

data class NoteWithDetails(
    val note: Note,
    val folder: Folder? = null,
    val tags: List<Tag> = emptyList(),
    val attachments: List<Attachment> = emptyList()
)
