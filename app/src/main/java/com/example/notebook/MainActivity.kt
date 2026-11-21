package com.example.notebook

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.LaunchedEffect
import com.example.notebook.ui.theme.LocalDarkTheme
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.notebook.domain.ThemeMode
import com.example.notebook.navigation.NotebookNavHost
import com.example.notebook.ui.theme.NotebookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status bar and gesture bar; screens add insets themselves.
        enableEdgeToEdge()
        setContent {
            // Stage 1: theme settings kept in memory (survive recreation, not app restart).
            // Stage 6 moves them to DataStore behind a ViewModel.
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            var dynamicColor by rememberSaveable { mutableStateOf(true) }

            NotebookTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                // The app theme can differ from the system one, so tell the system bars
                // which icon color to use (dark icons on a light app, light icons on a dark app).
                val darkTheme = LocalDarkTheme.current
                LaunchedEffect(darkTheme) {
                    val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                NotebookNavHost(
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                    dynamicColor = dynamicColor,
                    onDynamicColorChange = { dynamicColor = it },
                )
            }
        }
    }
}
