package com.strangeinfinity.notes.feature.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strangeinfinity.notes.core.data.NotesRepository
import com.strangeinfinity.notes.core.model.DrawingData
import com.strangeinfinity.notes.core.model.DrawingStroke
import com.strangeinfinity.notes.core.model.Note
import com.strangeinfinity.notes.feature.drawing.components.DrawingColorPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

data class DrawingUiState(
    val noteId: String,
    val strokes: List<DrawingStroke> = emptyList(),
    val selectedColor: Long = DrawingColorPalette[0],
    val selectedWidth: Float = 10f,
    val isEraser: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaved: Boolean = true
)

class DrawingViewModel(
    private val notesRepository: NotesRepository,
    private val targetNoteId: String?
) : ViewModel() {

    private val noteId = targetNoteId ?: UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(DrawingUiState(noteId = noteId))
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<List<DrawingStroke>>()
    private val redoStack = mutableListOf<List<DrawingStroke>>()

    init {
        loadDrawing()
    }

    private fun loadDrawing() {
        viewModelScope.launch {
            if (targetNoteId != null) {
                val details = notesRepository.getNoteWithDetails(targetNoteId)
                if (details?.note?.drawingJson != null) {
                    try {
                        val data = Json.decodeFromString<DrawingData>(details.note.drawingJson)
                        _uiState.value = _uiState.value.copy(
                            strokes = data.strokes,
                            canUndo = data.strokes.isNotEmpty()
                        )
                    } catch (e: Exception) {
                        // ignore malformed
                    }
                }
            }
        }
    }

    fun onAddStroke(stroke: DrawingStroke) {
        val currentStrokes = _uiState.value.strokes
        undoStack.add(currentStrokes)
        redoStack.clear()

        val updated = currentStrokes + stroke
        _uiState.value = _uiState.value.copy(
            strokes = updated,
            canUndo = true,
            canRedo = false,
            isSaved = false
        )
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentStrokes = _uiState.value.strokes
        redoStack.add(currentStrokes)
        val previous = undoStack.removeAt(undoStack.lastIndex)

        _uiState.value = _uiState.value.copy(
            strokes = previous,
            canUndo = undoStack.isNotEmpty(),
            canRedo = true,
            isSaved = false
        )
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentStrokes = _uiState.value.strokes
        undoStack.add(currentStrokes)
        val next = redoStack.removeAt(redoStack.lastIndex)

        _uiState.value = _uiState.value.copy(
            strokes = next,
            canUndo = true,
            canRedo = redoStack.isNotEmpty(),
            isSaved = false
        )
    }

    fun clearCanvas() {
        if (_uiState.value.strokes.isEmpty()) return
        undoStack.add(_uiState.value.strokes)
        redoStack.clear()

        _uiState.value = _uiState.value.copy(
            strokes = emptyList(),
            canUndo = true,
            canRedo = false,
            isSaved = false
        )
    }

    fun onColorSelected(color: Long) {
        _uiState.value = _uiState.value.copy(selectedColor = color, isEraser = false)
    }

    fun onWidthSelected(width: Float) {
        _uiState.value = _uiState.value.copy(selectedWidth = width)
    }

    fun onToggleEraser(eraser: Boolean) {
        _uiState.value = _uiState.value.copy(isEraser = eraser)
    }

    fun saveDrawing(onComplete: () -> Unit) {
        viewModelScope.launch {
            val strokes = _uiState.value.strokes
            val drawingJson = if (strokes.isNotEmpty()) {
                Json.encodeToString(DrawingData(strokes))
            } else null

            val existing = notesRepository.getNoteWithDetails(noteId)
            if (existing != null) {
                notesRepository.saveNote(
                    existing.note.copy(drawingJson = drawingJson, updatedAt = System.currentTimeMillis())
                )
            } else {
                // If creating a brand new drawing note
                notesRepository.saveNote(
                    Note(
                        id = noteId,
                        title = "Drawing Note",
                        content = "",
                        drawingJson = drawingJson,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
            onComplete()
        }
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository,
            noteId: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DrawingViewModel(notesRepository, noteId) as T
            }
        }
    }
}
