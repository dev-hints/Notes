package com.strangeinfinity.notes

import com.google.common.truth.Truth.assertThat
import com.strangeinfinity.notes.core.database.entity.NoteEntity
import com.strangeinfinity.notes.core.model.ChecklistItem
import com.strangeinfinity.notes.core.model.DrawingData
import com.strangeinfinity.notes.core.model.DrawingPoint
import com.strangeinfinity.notes.core.model.DrawingStroke
import com.strangeinfinity.notes.core.model.Note
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class NoteModelTest {

    @Test
    fun testNoteDomainEntityConversion() {
        val note = Note(
            id = "test-id-1",
            title = "Architectural Guidelines",
            content = "Offline-first Room database with AES-256 backup.",
            folderId = "folder-work",
            isPinned = true,
            isTrashed = false,
            colorIndex = 3,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = NoteEntity.fromDomain(note)
        assertThat(entity.id).isEqualTo(note.id)
        assertThat(entity.title).isEqualTo(note.title)
        assertThat(entity.content).isEqualTo(note.content)
        assertThat(entity.folderId).isEqualTo(note.folderId)
        assertThat(entity.isPinned).isTrue()
        assertThat(entity.colorIndex).isEqualTo(3)

        val convertedBack = entity.toDomain()
        assertThat(convertedBack).isEqualTo(note)
    }

    @Test
    fun testChecklistSerializationRoundTrip() {
        val items = listOf(
            ChecklistItem(id = "1", text = "Design system", isCompleted = true, order = 0),
            ChecklistItem(id = "2", text = "Room DB migration", isCompleted = false, order = 1),
            ChecklistItem(id = "3", text = "ADB testing", isCompleted = false, order = 2)
        )

        val jsonString = Json.encodeToString(items)
        val deserialized = Json.decodeFromString<List<ChecklistItem>>(jsonString)

        assertThat(deserialized.size).isEqualTo(3)
        assertThat(deserialized[0].isCompleted).isTrue()
        assertThat(deserialized[1].text).isEqualTo("Room DB migration")
        assertThat(deserialized[2].isCompleted).isFalse()
    }

    @Test
    fun testDrawingDataSerializationRoundTrip() {
        val stroke = DrawingStroke(
            points = listOf(DrawingPoint(10f, 20f), DrawingPoint(15f, 25f), DrawingPoint(30f, 40f)),
            color = 0xFF6366F1,
            strokeWidth = 8f,
            isEraser = false
        )
        val drawingData = DrawingData(strokes = listOf(stroke))

        val jsonString = Json.encodeToString(drawingData)
        val deserialized = Json.decodeFromString<DrawingData>(jsonString)

        assertThat(deserialized.strokes.size).isEqualTo(1)
        assertThat(deserialized.strokes[0].points.size).isEqualTo(3)
        assertThat(deserialized.strokes[0].strokeWidth).isEqualTo(8f)
    }
}
