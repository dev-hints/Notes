package com.strangeinfinity.notes.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strangeinfinity.notes.core.data.NotesRepository
import com.strangeinfinity.notes.core.model.Folder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class FolderWithCount(
    val folder: Folder,
    val noteCount: Int
)

data class FoldersUiState(
    val folders: List<FolderWithCount> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val folderToEdit: Folder? = null,
    val folderToDelete: Folder? = null
)

class FoldersViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _isCreateDialogOpen = MutableStateFlow(false)
    private val _folderToEdit = MutableStateFlow<Folder?>(null)
    private val _folderToDelete = MutableStateFlow<Folder?>(null)

    val uiState: StateFlow<FoldersUiState> = combine(
        notesRepository.getAllFoldersFlow(),
        notesRepository.getActiveNotesFlow(),
        _isCreateDialogOpen,
        _folderToEdit,
        _folderToDelete
    ) { folders, notes, isCreateOpen, folderToEdit, folderToDelete ->
        val counts = folders.map { folder ->
            val count = notes.count { it.note.folderId == folder.id }
            FolderWithCount(folder, count)
        }
        FoldersUiState(
            folders = counts,
            isCreateDialogOpen = isCreateOpen,
            folderToEdit = folderToEdit,
            folderToDelete = folderToDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FoldersUiState()
    )

    fun openCreateDialog(open: Boolean) {
        _isCreateDialogOpen.value = open
    }

    fun openEditDialog(folder: Folder?) {
        _folderToEdit.value = folder
    }

    fun openDeleteDialog(folder: Folder?) {
        _folderToDelete.value = folder
    }

    fun saveFolder(name: String, color: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val editing = _folderToEdit.value
            val folder = if (editing != null) {
                editing.copy(name = name.trim(), color = color)
            } else {
                Folder(id = UUID.randomUUID().toString(), name = name.trim(), color = color)
            }
            notesRepository.saveFolder(folder)
            _isCreateDialogOpen.value = false
            _folderToEdit.value = null
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            notesRepository.deleteFolder(folderId)
            _folderToDelete.value = null
        }
    }

    companion object {
        fun provideFactory(notesRepository: NotesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FoldersViewModel(notesRepository) as T
                }
            }
    }
}
