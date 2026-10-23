package com.strangeinfinity.notes.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strangeinfinity.notes.core.data.NotesRepository
import com.strangeinfinity.notes.core.data.PreferencesRepository
import com.strangeinfinity.notes.core.model.LayoutMode
import com.strangeinfinity.notes.core.model.SortOrder
import com.strangeinfinity.notes.core.model.ThemeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeConfig: ThemeConfig = ThemeConfig.SYSTEM,
    val layoutMode: LayoutMode = LayoutMode.GRID,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val activeNotesCount: Int = 0,
    val trashedNotesCount: Int = 0,
    val storageSizeBytes: Long = 0L
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _storageSize = MutableStateFlow(0L)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.themeConfig,
        preferencesRepository.layoutMode,
        preferencesRepository.sortOrder,
        notesRepository.countActiveNotesFlow(),
        notesRepository.countTrashedNotesFlow(),
        _storageSize
    ) { args: Array<Any> ->
        SettingsUiState(
            themeConfig = args[0] as ThemeConfig,
            layoutMode = args[1] as LayoutMode,
            sortOrder = args[2] as SortOrder,
            activeNotesCount = args[3] as Int,
            trashedNotesCount = args[4] as Int,
            storageSizeBytes = args[5] as Long
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        refreshStorageSize()
    }

    fun refreshStorageSize() {
        viewModelScope.launch {
            _storageSize.value = notesRepository.calculateStorageUsed()
        }
    }

    fun setTheme(theme: ThemeConfig) {
        viewModelScope.launch {
            preferencesRepository.setThemeConfig(theme)
        }
    }

    fun setLayoutMode(mode: LayoutMode) {
        viewModelScope.launch {
            preferencesRepository.setLayoutMode(mode)
        }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            preferencesRepository.setSortOrder(order)
        }
    }

    companion object {
        fun provideFactory(
            preferencesRepository: PreferencesRepository,
            notesRepository: NotesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(preferencesRepository, notesRepository) as T
            }
        }
    }
}
