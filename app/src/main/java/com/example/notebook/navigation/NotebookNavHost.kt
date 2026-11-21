package com.example.notebook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.notebook.domain.ThemeMode
import com.example.notebook.ui.screens.bin.BinMode
import com.example.notebook.ui.screens.bin.BinScreen
import com.example.notebook.ui.screens.categories.CategoriesScreen
import com.example.notebook.ui.screens.editor.EditorScreen
import com.example.notebook.ui.screens.notes.DrawerDestination
import com.example.notebook.ui.screens.notes.NotesScreen
import com.example.notebook.ui.screens.search.SearchScreen
import com.example.notebook.ui.screens.settings.SettingsScreen

/** All screens of the app live in one Activity; this graph switches between them. */
@Composable
fun NotebookNavHost(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val goBack: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = NotesRoute) {
        composable<NotesRoute> {
            NotesScreen(
                onOpenNote = { id -> navController.navigate(EditorRoute(noteId = id)) },
                onCreateNote = { navController.navigate(EditorRoute()) },
                onOpenSearch = { navController.navigate(SearchRoute) },
                onDrawerDestination = { destination ->
                    val route: Any = when (destination) {
                        DrawerDestination.Categories -> CategoriesRoute
                        DrawerDestination.Archive -> ArchiveRoute
                        DrawerDestination.Trash -> TrashRoute
                        DrawerDestination.Settings -> SettingsRoute
                    }
                    navController.navigate(route) { launchSingleTop = true }
                },
            )
        }
        composable<EditorRoute> { entry ->
            val route = entry.toRoute<EditorRoute>()
            EditorScreen(noteId = route.noteId, onBack = goBack)
        }
        composable<SearchRoute> { SearchScreen(onBack = goBack) }
        composable<CategoriesRoute> { CategoriesScreen(onBack = goBack) }
        composable<ArchiveRoute> { BinScreen(mode = BinMode.Archive, onBack = goBack) }
        composable<TrashRoute> { BinScreen(mode = BinMode.Trash, onBack = goBack) }
        composable<SettingsRoute> {
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                dynamicColor = dynamicColor,
                onDynamicColorChange = onDynamicColorChange,
                onBack = goBack,
            )
        }
    }
}
