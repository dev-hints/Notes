package com.strangeinfinity.notes.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.strangeinfinity.notes.core.model.LayoutMode
import com.strangeinfinity.notes.core.model.SortOrder
import com.strangeinfinity.notes.core.model.ThemeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesRepository(private val context: Context) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_config")
        private val KEY_LAYOUT = stringPreferencesKey("layout_mode")
        private val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        private const val QUICK_PREFS = "notes_quick_prefs"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val quickCache = context.getSharedPreferences(QUICK_PREFS, Context.MODE_PRIVATE)

    fun getInitialTheme(): ThemeConfig {
        val raw = quickCache.getString("theme_config", null) ?: ThemeConfig.SYSTEM.name
        return try {
            ThemeConfig.valueOf(raw)
        } catch (e: Exception) {
            ThemeConfig.SYSTEM
        }
    }

    fun getInitialLayoutMode(): LayoutMode {
        val raw = quickCache.getString("layout_mode", null) ?: LayoutMode.GRID.name
        return try {
            LayoutMode.valueOf(raw)
        } catch (e: Exception) {
            LayoutMode.GRID
        }
    }

    fun getInitialSortOrder(): SortOrder {
        val raw = quickCache.getString("sort_order", null) ?: SortOrder.MODIFIED_DESC.name
        return try {
            SortOrder.valueOf(raw)
        } catch (e: Exception) {
            SortOrder.MODIFIED_DESC
        }
    }

    val themeConfig: StateFlow<ThemeConfig> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: ThemeConfig.SYSTEM.name
        val config = try {
            ThemeConfig.valueOf(raw)
        } catch (e: Exception) {
            ThemeConfig.SYSTEM
        }
        quickCache.edit().putString("theme_config", config.name).apply()
        config
    }.flowOn(Dispatchers.IO).stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = getInitialTheme()
    )

    val layoutMode: StateFlow<LayoutMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_LAYOUT] ?: LayoutMode.GRID.name
        val mode = try {
            LayoutMode.valueOf(raw)
        } catch (e: Exception) {
            LayoutMode.GRID
        }
        quickCache.edit().putString("layout_mode", mode.name).apply()
        mode
    }.flowOn(Dispatchers.IO).stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = getInitialLayoutMode()
    )

    val sortOrder: StateFlow<SortOrder> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SORT_ORDER] ?: SortOrder.MODIFIED_DESC.name
        val order = try {
            SortOrder.valueOf(raw)
        } catch (e: Exception) {
            SortOrder.MODIFIED_DESC
        }
        quickCache.edit().putString("sort_order", order.name).apply()
        order
    }.flowOn(Dispatchers.IO).stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = getInitialSortOrder()
    )

    suspend fun setThemeConfig(theme: ThemeConfig) {
        quickCache.edit().putString("theme_config", theme.name).apply()
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    suspend fun setLayoutMode(mode: LayoutMode) {
        quickCache.edit().putString("layout_mode", mode.name).apply()
        context.dataStore.edit { prefs ->
            prefs[KEY_LAYOUT] = mode.name
        }
    }

    suspend fun setSortOrder(order: SortOrder) {
        quickCache.edit().putString("sort_order", order.name).apply()
        context.dataStore.edit { prefs ->
            prefs[KEY_SORT_ORDER] = order.name
        }
    }
}
