package com.strangeinfinity.notes.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strangeinfinity.notes.core.data.NotesRepository
import com.strangeinfinity.notes.core.data.PreferencesRepository
import com.strangeinfinity.notes.core.model.Folder
import com.strangeinfinity.notes.core.model.LayoutMode
import com.strangeinfinity.notes.core.model.NoteWithDetails
import com.strangeinfinity.notes.core.model.SortOrder
import com.strangeinfinity.notes.core.model.Tag
import com.strangeinfinity.notes.feature.home.components.HomeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val notes: List<NoteWithDetails> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val layoutMode: LayoutMode = LayoutMode.GRID,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val selectedFilter: HomeFilter = HomeFilter.All,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedNoteIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isMoveFolderDialogOpen: Boolean = false,
    val isLoading: Boolean = false
)

private data class RepoData(
    val notes: List<NoteWithDetails> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList()
)

private data class UiControls(
    val filter: HomeFilter = HomeFilter.All,
    val query: String = "",
    val isSearchActive: Boolean = false,
    val selectedNoteIds: Set<String> = emptySet(),
    val isMoveDialogOpen: Boolean = false
)

class HomeViewModel(
    private val notesRepository: NotesRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _selectedFilter = MutableStateFlow<HomeFilter>(HomeFilter.All)
    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isMoveFolderDialogOpen = MutableStateFlow(false)

    private val _repoDataFlow = combine(
        notesRepository.getActiveNotesFlow(),
        notesRepository.getAllFoldersFlow(),
        notesRepository.getAllTagsFlow()
    ) { notes, folders, tags ->
        RepoData(notes, folders, tags)
    }

    private val _uiControlsFlow = combine(
        _selectedFilter,
        _searchQuery,
        _isSearchActive,
        _selectedNoteIds,
        _isMoveFolderDialogOpen
    ) { filter, query, isSearchActive, selectedIds, isMoveDialogOpen ->
        UiControls(
            filter = filter,
            query = query.trim(),
            isSearchActive = isSearchActive,
            selectedNoteIds = selectedIds,
            isMoveDialogOpen = isMoveDialogOpen
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _repoDataFlow,
        _uiControlsFlow,
        preferencesRepository.layoutMode,
        preferencesRepository.sortOrder
    ) { repoData, controls, layoutMode, sortOrder ->
        // 1. Search Filter
        val filtered = if (controls.query.isNotBlank()) {
            val q = controls.query.lowercase()
            repoData.notes.filter {
                it.note.title.lowercase().contains(q) ||
                it.note.content.lowercase().contains(q) ||
                it.tags.any { tag -> tag.name.lowercase().contains(q) } ||
                (it.folder?.name?.lowercase()?.contains(q) == true)
            }
        } else repoData.notes

        // 2. Chip Filter
        val chipFiltered = when (val filter = controls.filter) {
            is HomeFilter.All -> filtered
            is HomeFilter.Pinned -> filtered.filter { it.note.isPinned }
            is HomeFilter.ByFolder -> filtered.filter { it.note.folderId == filter.folder.id }
            is HomeFilter.ByTag -> filtered.filter { it.tags.any { tag -> tag.id == filter.tag.id } }
        }

        // 3. Sort Order
        val sorted = when (sortOrder) {
            SortOrder.MODIFIED_DESC -> chipFiltered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenByDescending { it.note.updatedAt }
            )
            SortOrder.MODIFIED_ASC -> chipFiltered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenBy { it.note.updatedAt }
            )
            SortOrder.CREATED_DESC -> chipFiltered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenByDescending { it.note.createdAt }
            )
            SortOrder.CREATED_ASC -> chipFiltered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenBy { it.note.createdAt }
            )
            SortOrder.TITLE_ASC -> chipFiltered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenBy { it.note.title.lowercase() }
            )
            SortOrder.TITLE_DESC -> chipFiltered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenByDescending { it.note.title.lowercase() }
            )
        }

        HomeUiState(
            notes = sorted,
            folders = repoData.folders,
            tags = repoData.tags,
            layoutMode = layoutMode,
            sortOrder = sortOrder,
            selectedFilter = controls.filter,
            searchQuery = controls.query,
            isSearchActive = controls.isSearchActive,
            selectedNoteIds = controls.selectedNoteIds,
            isSelectionMode = controls.selectedNoteIds.isNotEmpty(),
            isMoveFolderDialogOpen = controls.isMoveDialogOpen,
            isLoading = false
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            layoutMode = preferencesRepository.getInitialLayoutMode(),
            sortOrder = preferencesRepository.getInitialSortOrder(),
            isLoading = true
        )
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onToggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun onSelectFilter(filter: HomeFilter) {
        _selectedFilter.value = filter
    }

    fun onToggleLayoutMode() {
        val current = uiState.value.layoutMode
        val newMode = if (current == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID
        viewModelScope.launch {
            preferencesRepository.setLayoutMode(newMode)
        }
    }

    fun onSelectSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            preferencesRepository.setSortOrder(sortOrder)
        }
    }

    fun onNoteClick(noteId: String, onNavigate: (String) -> Unit) {
        if (uiState.value.isSelectionMode) {
            toggleNoteSelection(noteId)
        } else {
            onNavigate(noteId)
        }
    }

    fun onNoteLongClick(noteId: String) {
        toggleNoteSelection(noteId)
    }

    fun toggleNoteSelection(noteId: String) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) {
            current.remove(noteId)
        } else {
            current.add(noteId)
        }
        _selectedNoteIds.value = current
    }

    fun onSelectAll() {
        val allIds = uiState.value.notes.map { it.note.id }.toSet()
        _selectedNoteIds.value = allIds
    }

    fun onClearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun onBatchPin() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        val anyUnpinned = uiState.value.notes.filter { ids.contains(it.note.id) }.any { !it.note.isPinned }
        viewModelScope.launch {
            notesRepository.setPinState(ids, anyUnpinned)
            onClearSelection()
        }
    }

    fun onBatchDelete() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            notesRepository.setTrashState(ids, true)
            onClearSelection()
        }
    }

    fun showMoveFolderDialog(show: Boolean) {
        _isMoveFolderDialogOpen.value = show
    }

    fun onBatchMoveToFolder(folderId: String?) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            notesRepository.moveToFolder(ids, folderId)
            showMoveFolderDialog(false)
            onClearSelection()
        }
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository,
            preferencesRepository: PreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(notesRepository, preferencesRepository) as T
            }
        }
    }
}
