package com.example.notebook.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.notebook.domain.AppSettings

/**
 * Current settings for any screen that only needs to *read* them (list layout,
 * editor text size). Provided once in MainActivity, so screens do not each load DataStore.
 * Changing a setting still goes through a ViewModel and SettingsRepository.
 */
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }
