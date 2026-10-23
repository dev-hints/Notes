package com.strangeinfinity.notes.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strangeinfinity.notes.core.data.NotesRepository
import com.strangeinfinity.notes.core.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashUiState(
    val trashedNotes: List<Note> = emptyList(),
    val isEmptyTrashDialogOpen: Boolean = false,
    val noteToDeletePermanently: Note? = null
)

class TrashViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _isEmptyTrashDialogOpen = MutableStateFlow(false)
    private val _noteToDelete = MutableStateFlow<Note?>(null)

    val uiState: StateFlow<TrashUiState> = combine(
        notesRepository.getTrashedNotesFlow(),
        _isEmptyTrashDialogOpen,
        _noteToDelete
    ) { notes, isEmptyOpen, noteToDelete ->
        TrashUiState(
            trashedNotes = notes,
            isEmptyTrashDialogOpen = isEmptyOpen,
            noteToDeletePermanently = noteToDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrashUiState()
    )

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.setTrashState(listOf(noteId), false)
        }
    }

    fun openDeletePermanentlyDialog(note: Note?) {
        _noteToDelete.value = note
    }

    fun deletePermanently(noteId: String) {
        viewModelScope.launch {
            notesRepository.deletePermanently(listOf(noteId))
            _noteToDelete.value = null
        }
    }

    fun openEmptyTrashDialog(open: Boolean) {
        _isEmptyTrashDialogOpen.value = open
    }

    fun emptyTrash() {
        viewModelScope.launch {
            notesRepository.emptyTrash()
            _isEmptyTrashDialogOpen.value = false
        }
    }

    companion object {
        fun provideFactory(notesRepository: NotesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrashViewModel(notesRepository) as T
                }
            }
    }
}
