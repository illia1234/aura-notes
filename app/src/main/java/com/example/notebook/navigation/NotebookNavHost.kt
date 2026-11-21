package com.example.notebook.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.notebook.ui.screens.bin.BinMode
import com.example.notebook.ui.screens.bin.BinScreen
import com.example.notebook.ui.screens.categories.CategoriesScreen
import com.example.notebook.ui.screens.editor.EditorScreen
import com.example.notebook.ui.screens.notes.DrawerDestination
import com.example.notebook.ui.screens.notes.NotesScreen
import com.example.notebook.ui.screens.settings.SettingsScreen

/** All screens of the app live in one Activity; this graph switches between them. */
@Composable
fun NotebookNavHost(navController: NavHostController = rememberNavController()) {
    val openNote: (Long) -> Unit = { id -> navController.navigate(EditorRoute(noteId = id)) { launchSingleTop = true } }

    NavHost(navController = navController, startDestination = NotesRoute) {
        composable<NotesRoute> {
            // launchSingleTop: a quick double tap cannot open the editor twice.
            NotesScreen(
                onOpenNote = openNote,
                onCreateNote = { categoryId -> navController.navigate(EditorRoute(categoryId = categoryId)) { launchSingleTop = true } },
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
        // Each screen's "Back" pops only itself, and only once: dropUnlessResumed ignores
        // a second tap during the transition (it would pop the main screen too).
        composable<EditorRoute> { EditorScreen(onBack = dropUnlessResumed { navController.popBackStack() }) }
        composable<CategoriesRoute> { CategoriesScreen(onBack = dropUnlessResumed { navController.popBackStack() }) }
        composable<ArchiveRoute> {
            BinScreen(mode = BinMode.Archive, onOpenNote = openNote, onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<TrashRoute> {
            BinScreen(mode = BinMode.Trash, onOpenNote = openNote, onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<SettingsRoute> { SettingsScreen(onBack = dropUnlessResumed { navController.popBackStack() }) }
    }
}
