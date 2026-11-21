package com.example.notebook.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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

    // Screen transitions: short fade + slight slide, 250 ms with standard M3 easing
    // (the spec's fallback for the card-to-editor animation). Compose follows the
    // system "animation scale" setting, so turning animations off disables these too.
    NavHost(
        navController = navController,
        startDestination = NotesRoute,
        enterTransition = { fadeIn(tween(ENTER_MS)) + slideInHorizontally(tween(ENTER_MS)) { it / SLIDE_FRACTION } },
        exitTransition = { fadeOut(tween(EXIT_MS)) },
        popEnterTransition = { fadeIn(tween(ENTER_MS)) },
        popExitTransition = { fadeOut(tween(EXIT_MS)) + slideOutHorizontally(tween(ENTER_MS)) { it / SLIDE_FRACTION } },
    ) {
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

private const val ENTER_MS = 250
private const val EXIT_MS = 200

/** The new screen slides in from 1/10 of the width: noticeable but calm. */
private const val SLIDE_FRACTION = 10
