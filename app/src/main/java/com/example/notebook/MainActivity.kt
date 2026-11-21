package com.example.notebook

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notebook.domain.ThemeMode
import com.example.notebook.navigation.NotebookNavHost
import com.example.notebook.ui.LocalAppSettings
import com.example.notebook.ui.theme.LocalDarkTheme
import com.example.notebook.ui.theme.NotebookTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status bar and gesture bar; screens add insets themselves.
        enableEdgeToEdge()

        val settingsRepository = (application as NotebookApplication).container.settingsRepository
        // Read settings once before the first frame (a tiny file, a few milliseconds),
        // so a user who chose the dark theme never sees a light frame at startup.
        val initialSettings = runBlocking { settingsRepository.settings.first() }
        if (initialSettings.themeMode == ThemeMode.DARK) window.decorView.setBackgroundColor(Color.BLACK)

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialSettings)

            NotebookTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                // The app theme can differ from the system one, so tell the system bars
                // which icon color to use (dark icons on a light app, light icons on a dark app).
                val darkTheme = LocalDarkTheme.current
                LaunchedEffect(darkTheme) {
                    val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                CompositionLocalProvider(LocalAppSettings provides settings) {
                    NotebookNavHost()
                }
            }
        }
    }
}
