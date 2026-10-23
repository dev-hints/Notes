package com.strangeinfinity.notes.feature.editor

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strangeinfinity.notes.core.data.NotesRepository
import com.strangeinfinity.notes.core.model.Attachment
import com.strangeinfinity.notes.core.model.ChecklistItem
import com.strangeinfinity.notes.core.model.Folder
import com.strangeinfinity.notes.core.model.Note
import com.strangeinfinity.notes.core.model.NoteWithDetails
import com.strangeinfinity.notes.core.model.Tag
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

enum class SaveStatus {
    SAVED,
    SAVING,
    ERROR
}

data class EditorUiState(
    val noteId: String = "",
    val title: String = "",
    val content: TextFieldValue = TextFieldValue(""),
    val isPinned: Boolean = false,
    val colorIndex: Int = 0,
    val folderId: String? = null,
    val folder: Folder? = null,
    val tags: List<Tag> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val checklistItems: List<ChecklistItem> = emptyList(),
    val isChecklistMode: Boolean = false,
    val drawingJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val saveStatus: SaveStatus = SaveStatus.SAVED,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val availableFolders: List<Folder> = emptyList(),
    val isColorPickerOpen: Boolean = false,
    val isAddTagDialogOpen: Boolean = false,
    val isNoteLoaded: Boolean = false
)

private data class TextSnapshot(val title: String, val content: String)

class EditorViewModel(
    private val notesRepository: NotesRepository,
    private val noteIdParam: String?,
    private val initialFolderId: String?,
    private val initialType: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<TextSnapshot>()
    private val redoStack = mutableListOf<TextSnapshot>()
    private var autosaveJob: Job? = null
    private var lastSavedSnapshot = TextSnapshot("", "")

    init {
        loadNote()
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            notesRepository.getAllFoldersFlow().collect { folders ->
                _uiState.value = _uiState.value.copy(
                    availableFolders = folders,
                    folder = folders.firstOrNull { it.id == _uiState.value.folderId }
                )
            }
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            val targetId = noteIdParam ?: UUID.randomUUID().toString()
            val existing = if (noteIdParam != null) {
                notesRepository.getNoteWithDetails(targetId)
            } else null

            if (existing != null) {
                val checklist = parseChecklist(existing.note.checklistJson)
                val wordCount = calculateWordCount(existing.note.content)
                val charCount = existing.note.content.length

                _uiState.value = EditorUiState(
                    noteId = existing.note.id,
                    title = existing.note.title,
                    content = TextFieldValue(existing.note.content),
                    isPinned = existing.note.isPinned,
                    colorIndex = existing.note.colorIndex,
                    folderId = existing.note.folderId,
                    folder = existing.folder,
                    tags = existing.tags,
                    attachments = existing.attachments,
                    checklistItems = checklist,
                    isChecklistMode = checklist.isNotEmpty(),
                    drawingJson = existing.note.drawingJson,
                    createdAt = existing.note.createdAt,
                    updatedAt = existing.note.updatedAt,
                    saveStatus = SaveStatus.SAVED,
                    wordCount = wordCount,
                    charCount = charCount,
                    isNoteLoaded = true
                )
                lastSavedSnapshot = TextSnapshot(existing.note.title, existing.note.content)
            } else {
                // New Note
                val isChecklist = initialType == "checklist"
                val initialChecklist = if (isChecklist) {
                    listOf(ChecklistItem(text = "", isCompleted = false, order = 0))
                } else emptyList()

                _uiState.value = EditorUiState(
                    noteId = targetId,
                    title = "",
                    content = TextFieldValue(""),
                    folderId = initialFolderId,
                    checklistItems = initialChecklist,
                    isChecklistMode = isChecklist,
                    saveStatus = SaveStatus.SAVED,
                    isNoteLoaded = true
                )
                lastSavedSnapshot = TextSnapshot("", "")
            }
        }
    }

    private fun parseChecklist(json: String?): List<ChecklistItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            Json.decodeFromString<List<ChecklistItem>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateWordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun onTitleChange(newTitle: String) {
        val current = _uiState.value
        pushUndo(current.title, current.content.text)
        _uiState.value = current.copy(
            title = newTitle,
            canUndo = undoStack.isNotEmpty(),
            canRedo = false
        )
        redoStack.clear()
        triggerAutosave()
    }

    fun onContentChange(newContent: TextFieldValue) {
        val current = _uiState.value
        // Only push undo on significant edits or typing pauses
        if (undoStack.isEmpty() || current.content.text.length != newContent.text.length) {
            pushUndo(current.title, current.content.text)
        }
        val wordCount = calculateWordCount(newContent.text)
        val charCount = newContent.text.length

        _uiState.value = current.copy(
            content = newContent,
            wordCount = wordCount,
            charCount = charCount,
            canUndo = undoStack.isNotEmpty(),
            canRedo = false
        )
        redoStack.clear()
        triggerAutosave()
    }

    private fun pushUndo(title: String, content: String) {
        if (undoStack.size >= 50) {
            undoStack.removeAt(0)
        }
        undoStack.add(TextSnapshot(title, content))
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value
        redoStack.add(TextSnapshot(current.title, current.content.text))
        val previous = undoStack.removeAt(undoStack.lastIndex)

        _uiState.value = current.copy(
            title = previous.title,
            content = TextFieldValue(previous.content, selection = TextRange(previous.content.length)),
            wordCount = calculateWordCount(previous.content),
            charCount = previous.content.length,
            canUndo = undoStack.isNotEmpty(),
            canRedo = true
        )
        triggerAutosave()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value
        undoStack.add(TextSnapshot(current.title, current.content.text))
        val next = redoStack.removeAt(redoStack.lastIndex)

        _uiState.value = current.copy(
            title = next.title,
            content = TextFieldValue(next.content, selection = TextRange(next.content.length)),
            wordCount = calculateWordCount(next.content),
            charCount = next.content.length,
            canUndo = true,
            canRedo = redoStack.isNotEmpty()
        )
        triggerAutosave()
    }

    // Formatting Toolbar Actions
    fun applyBold() = wrapSelection("**", "**")
    fun applyItalic() = wrapSelection("*", "*")
    fun applyUnderline() = wrapSelection("<u>", "</u>")
    fun applyStrikethrough() = wrapSelection("~~", "~~")

    private fun wrapSelection(prefix: String, suffix: String) {
        val current = _uiState.value.content
        val text = current.text
        val selection = current.selection

        val selectedText = if (selection.start != selection.end) {
            text.substring(selection.min, selection.max)
        } else ""

        val newText = buildString {
            append(text.substring(0, selection.min))
            append(prefix)
            append(selectedText.ifEmpty { "text" })
            append(suffix)
            append(text.substring(selection.max))
        }

        val newCursor = if (selectedText.isEmpty()) {
            selection.min + prefix.length
        } else {
            selection.min + prefix.length + selectedText.length + suffix.length
        }

        onContentChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        )
    }

    fun applyHeading() = insertAtLineStart("# ")
    fun applyBulletList() = insertAtLineStart("• ")
    fun applyNumberedList() = insertAtLineStart("1. ")

    private fun insertAtLineStart(prefix: String) {
        val current = _uiState.value.content
        val text = current.text
        val selection = current.selection

        val lineStart = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }

        val newText = buildString {
            append(text.substring(0, lineStart))
            append(prefix)
            append(text.substring(lineStart))
        }

        onContentChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(selection.min + prefix.length)
            )
        )
    }

    fun toggleChecklistMode() {
        val current = _uiState.value
        val newMode = !current.isChecklistMode
        val items = if (newMode && current.checklistItems.isEmpty()) {
            listOf(ChecklistItem(text = "", isCompleted = false, order = 0))
        } else current.checklistItems

        _uiState.value = current.copy(
            isChecklistMode = newMode,
            checklistItems = items
        )
        triggerAutosave()
    }

    fun onToggleChecklistItem(itemId: String, isCompleted: Boolean) {
        val updated = _uiState.value.checklistItems.map {
            if (it.id == itemId) it.copy(isCompleted = isCompleted) else it
        }
        _uiState.value = _uiState.value.copy(checklistItems = updated)
        triggerAutosave()
    }

    fun onUpdateChecklistItemText(itemId: String, text: String) {
        val updated = _uiState.value.checklistItems.map {
            if (it.id == itemId) it.copy(text = text) else it
        }
        _uiState.value = _uiState.value.copy(checklistItems = updated)
        triggerAutosave()
    }

    fun onDeleteChecklistItem(itemId: String) {
        val updated = _uiState.value.checklistItems.filter { it.id != itemId }
        _uiState.value = _uiState.value.copy(checklistItems = updated)
        triggerAutosave()
    }

    fun onAddChecklistItem(text: String) {
        val newItem = ChecklistItem(
            text = text,
            isCompleted = false,
            order = _uiState.value.checklistItems.size
        )
        val updated = _uiState.value.checklistItems + newItem
        _uiState.value = _uiState.value.copy(checklistItems = updated)
        triggerAutosave()
    }

    fun onTogglePin() {
        val newPinned = !_uiState.value.isPinned
        _uiState.value = _uiState.value.copy(isPinned = newPinned)
        triggerAutosave()
    }

    fun onSelectColorIndex(index: Int) {
        _uiState.value = _uiState.value.copy(colorIndex = index, isColorPickerOpen = false)
        triggerAutosave()
    }

    fun toggleColorPicker(open: Boolean) {
        _uiState.value = _uiState.value.copy(isColorPickerOpen = open)
    }

    fun onSelectFolder(folderId: String?) {
        val folder = _uiState.value.availableFolders.firstOrNull { it.id == folderId }
        _uiState.value = _uiState.value.copy(folderId = folderId, folder = folder)
        triggerAutosave()
    }

    fun onAddTag(tagName: String) {
        if (tagName.isBlank()) return
        viewModelScope.launch {
            val tag = notesRepository.addTagToNote(_uiState.value.noteId, tagName)
            val updated = _uiState.value.tags.filter { it.id != tag.id } + tag
            _uiState.value = _uiState.value.copy(tags = updated, isAddTagDialogOpen = false)
        }
    }

    fun onRemoveTag(tagId: String) {
        viewModelScope.launch {
            notesRepository.removeTagFromNote(_uiState.value.noteId, tagId)
            val updated = _uiState.value.tags.filter { it.id != tagId }
            _uiState.value = _uiState.value.copy(tags = updated)
        }
    }

    fun toggleAddTagDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(isAddTagDialogOpen = open)
    }

    fun onAddAttachmentUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val attachment = notesRepository.addAttachment(_uiState.value.noteId, uri)
                _uiState.value = _uiState.value.copy(
                    attachments = _uiState.value.attachments + attachment
                )
            } catch (e: Exception) {
                // Keep UI informed
            }
        }
    }

    fun onRemoveAttachment(attachment: Attachment) {
        viewModelScope.launch {
            notesRepository.deleteAttachment(attachment)
            _uiState.value = _uiState.value.copy(
                attachments = _uiState.value.attachments.filter { it.id != attachment.id }
            )
        }
    }

    fun setDrawingJson(json: String?) {
        _uiState.value = _uiState.value.copy(drawingJson = json)
        triggerAutosave()
    }

    fun deleteNote(onComplete: () -> Unit) {
        viewModelScope.launch {
            notesRepository.setTrashState(listOf(_uiState.value.noteId), true)
            onComplete()
        }
    }

    fun duplicateNote(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            saveImmediately()
            val newId = notesRepository.duplicateNote(_uiState.value.noteId)
            onComplete(newId)
        }
    }

    // Debounced Autosave (800ms)
    private fun triggerAutosave() {
        autosaveJob?.cancel()
        _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVING)
        autosaveJob = viewModelScope.launch {
            delay(800)
            saveImmediately()
        }
    }

    suspend fun saveImmediately() {
        val state = _uiState.value
        if (!state.isNoteLoaded) return

        // If title and content are blank, and no checklists or drawings or attachments, don't persist empty dummy
        val hasChecklists = state.checklistItems.any { it.text.isNotBlank() }
        val hasDrawing = !state.drawingJson.isNullOrBlank()
        val hasAttachments = state.attachments.isNotEmpty()

        if (state.title.isBlank() && state.content.text.isBlank() && !hasChecklists && !hasDrawing && !hasAttachments) {
            _uiState.value = state.copy(saveStatus = SaveStatus.SAVED)
            return
        }

        try {
            val checklistJson = if (state.checklistItems.isNotEmpty()) {
                Json.encodeToString(state.checklistItems)
            } else null

            val note = Note(
                id = state.noteId,
                title = state.title,
                content = state.content.text,
                folderId = state.folderId,
                isPinned = state.isPinned,
                colorIndex = state.colorIndex,
                createdAt = state.createdAt,
                updatedAt = System.currentTimeMillis(),
                checklistJson = checklistJson,
                drawingJson = state.drawingJson
            )

            notesRepository.saveNote(note)
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVED)
            lastSavedSnapshot = TextSnapshot(state.title, state.content.text)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.ERROR)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Final synchronous or quick background flush on clearing
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository,
            noteId: String?,
            folderId: String?,
            type: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(notesRepository, noteId, folderId, type) as T
            }
        }
    }
}
