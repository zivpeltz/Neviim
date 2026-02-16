package com.neviim.market.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user settings such as theme mode and language preference.
 * Persists choices using SharedPreferences.
 */
object SettingsRepository {

    private const val PREFS_NAME = "neviim_settings"
    private const val KEY_THEME_MODE = "theme_mode"

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private var prefs: SharedPreferences? = null

    /**
     * Call once from Application or MainActivity to initialise.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs?.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        _themeMode.value = try { ThemeMode.valueOf(stored) } catch (_: Exception) { ThemeMode.SYSTEM }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }
}
