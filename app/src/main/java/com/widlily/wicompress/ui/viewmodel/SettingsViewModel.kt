package com.widlily.wicompress.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.widlily.wicompress.data.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application.applicationContext)

    val theme: StateFlow<String> = settingsRepository.theme
    val hapticEnabled: StateFlow<Boolean> = settingsRepository.hapticEnabled
    val autoDeleteEnabled: StateFlow<Boolean> = settingsRepository.autoDeleteEnabled
    val outputDirectory: StateFlow<String> = settingsRepository.outputDirectory

    fun setTheme(themeMode: String) {
        settingsRepository.setTheme(themeMode)
    }

    fun setHapticEnabled(enabled: Boolean) {
        settingsRepository.setHapticEnabled(enabled)
    }

    fun setAutoDeleteEnabled(enabled: Boolean) {
        settingsRepository.setAutoDeleteEnabled(enabled)
    }

    fun setOutputDirectory(path: String) {
        settingsRepository.setOutputDirectory(path)
    }
}
