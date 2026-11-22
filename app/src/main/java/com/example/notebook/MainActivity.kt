package com.example.notebook

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notebook.navigation.AuraApp
import com.example.notebook.ui.LocalAppSettings
import com.example.notebook.ui.theme.AuraTheme
import com.example.notebook.ui.theme.LocalAuraPalette
import com.example.notebook.ui.theme.palette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status bar and gesture bar; screens add insets themselves.
        // Aura is always dark, so the system bar icons are always light.
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)

        val container = (application as NotebookApplication).container
        // Read settings once before the first frame (a tiny file, a few milliseconds),
        // so the first screen already uses the saved sort order and text size.
        val initialSettings = runBlocking { container.settingsRepository.settings.first() }

        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialSettings)
            AuraTheme {
                CompositionLocalProvider(
                    LocalAppSettings provides settings,
                    LocalAuraPalette provides settings.style.palette(),
                ) {
                    AuraApp(container)
                }
            }
        }
    }
}
