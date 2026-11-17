package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.GridDensity
import com.example.data.model.LauncherPreferences
import com.example.data.model.WindowSizeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "fluent11_preferences"
)

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val SHOW_DESKTOP_LABELS = booleanPreferencesKey("show_desktop_labels")
        val GRID_DENSITY = stringPreferencesKey("grid_density")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val HAS_SEEDED_DEFAULTS = booleanPreferencesKey("has_seeded_defaults")
        val FORCE_LANDSCAPE_APPS = booleanPreferencesKey("force_landscape_apps")
        val USE_FREEFORM_BOUNDS = booleanPreferencesKey("use_freeform_bounds")
        val DEFAULT_WINDOW_MODE = stringPreferencesKey("default_window_mode")
        val KEEP_TASKBAR_OVER_APPS = booleanPreferencesKey("keep_taskbar_over_apps")
        val ANCHOR_TASKBAR_BOTTOM_EDGE = booleanPreferencesKey("anchor_taskbar_bottom_edge")
    }

    val preferencesFlow: Flow<LauncherPreferences> = context.launcherDataStore.data.map { prefs ->
        LauncherPreferences(
            weatherCity = prefs[Keys.WEATHER_CITY] ?: "São Paulo",
            showDesktopLabels = prefs[Keys.SHOW_DESKTOP_LABELS] ?: true,
            gridDensity = GridDensity.fromKey(prefs[Keys.GRID_DENSITY] ?: GridDensity.STANDARD.key),
            userDisplayName = prefs[Keys.USER_DISPLAY_NAME] ?: "Usuário",
            hasSeededDefaults = prefs[Keys.HAS_SEEDED_DEFAULTS] ?: false,
            forceLandscapeApps = prefs[Keys.FORCE_LANDSCAPE_APPS] ?: true,
            useFreeformLaunchBounds = prefs[Keys.USE_FREEFORM_BOUNDS] ?: true,
            defaultWindowMode = WindowSizeMode.fromKey(
                prefs[Keys.DEFAULT_WINDOW_MODE] ?: WindowSizeMode.MAXIMIZED.key
            ),
            keepTaskbarOverApps = prefs[Keys.KEEP_TASKBAR_OVER_APPS] ?: true,
            anchorTaskbarAtBottomEdge = prefs[Keys.ANCHOR_TASKBAR_BOTTOM_EDGE] ?: false
        )
    }

    suspend fun setWeatherCity(city: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.WEATHER_CITY] = city.trim().ifEmpty { "São Paulo" }
        }
    }

    suspend fun setShowDesktopLabels(show: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.SHOW_DESKTOP_LABELS] = show
        }
    }

    suspend fun setGridDensity(density: GridDensity) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.GRID_DENSITY] = density.key
        }
    }

    suspend fun setUserDisplayName(name: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.USER_DISPLAY_NAME] = name.trim().ifEmpty { "Usuário" }
        }
    }

    suspend fun markDefaultsSeeded() {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED_DEFAULTS] = true
        }
    }

    suspend fun setForceLandscapeApps(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.FORCE_LANDSCAPE_APPS] = enabled
        }
    }

    suspend fun setUseFreeformLaunchBounds(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.USE_FREEFORM_BOUNDS] = enabled
        }
    }

    suspend fun setDefaultWindowMode(mode: WindowSizeMode) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_WINDOW_MODE] = mode.key
        }
    }

    suspend fun setKeepTaskbarOverApps(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.KEEP_TASKBAR_OVER_APPS] = enabled
        }
    }

    suspend fun setAnchorTaskbarAtBottomEdge(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.ANCHOR_TASKBAR_BOTTOM_EDGE] = enabled
        }
    }
}
