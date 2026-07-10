package com.widlily.wicompress.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _theme = MutableStateFlow(getThemePreference())
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(getHapticPreference())
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _autoDeleteEnabled = MutableStateFlow(getAutoDeletePreference())
    val autoDeleteEnabled: StateFlow<Boolean> = _autoDeleteEnabled.asStateFlow()

    private val _outputDirectory = MutableStateFlow(getOutputDirectoryPreference())
    val outputDirectory: StateFlow<String> = _outputDirectory.asStateFlow()

    fun setTheme(themeMode: String) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode).apply()
        _theme.value = themeMode
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _hapticEnabled.value = enabled
    }

    fun setAutoDeleteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DELETE, enabled).apply()
        _autoDeleteEnabled.value = enabled
    }

    fun setOutputDirectory(path: String) {
        prefs.edit().putString(KEY_OUTPUT_DIR, path).apply()
        _outputDirectory.value = path
    }

    private fun getThemePreference(): String = prefs.getString(KEY_THEME_MODE, "Auto") ?: "Auto"
    private fun getHapticPreference(): Boolean = prefs.getBoolean(KEY_HAPTIC, true)
    private fun getAutoDeletePreference(): Boolean = prefs.getBoolean(KEY_AUTO_DELETE, false)
    private fun getOutputDirectoryPreference(): String = prefs.getString(KEY_OUTPUT_DIR, "WiCompress") ?: "WiCompress"

    companion object {
        private const val PREFS_NAME = "wicompress_preferences"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_AUTO_DELETE = "key_auto_delete"
        private const val KEY_OUTPUT_DIR = "key_output_dir"
    }
}
