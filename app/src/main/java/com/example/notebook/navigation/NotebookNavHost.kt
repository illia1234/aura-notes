package com.example.notebook.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.notebook.AppContainer
import com.example.notebook.ui.components.LocalNavAnimatedScope
import com.example.notebook.ui.components.LocalSharedTransitionScope
import com.example.notebook.ui.screens.bin.BinMode
import com.example.notebook.ui.screens.bin.BinScreen
import com.example.notebook.ui.screens.editor.EditorScreen
import com.example.notebook.ui.screens.labels.LabelsScreen
import com.example.notebook.ui.screens.notes.NotesScreen
import com.example.notebook.ui.screens.search.SearchScreen
import com.example.notebook.ui.screens.settings.SettingsScreen
import com.example.notebook.ui.theme.AuraColors
import kotlinx.coroutines.launch

/**
 * Root of the Aura UI: the navigation graph inside a SharedTransitionLayout (so a card can
 * expand into the editor), the bottom tab bar (only on the five tabs) and one snackbar host
 * for the whole app (messages like "Заметка в корзине · Отменить" from any screen).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AuraApp(container: AppContainer, navController: NavHostController = rememberNavController()) {
    val entry by navController.currentBackStackEntryAsState()
    val currentTab = entry?.let(::tabOf)
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources

    LaunchedEffect(Unit) {
        container.userMessenger.messages.collect { message ->
            val label = message.actionLabel?.invoke(resources)
            val result = snackbarHostState.showSnackbar(
                message = message.text(resources),
                actionLabel = label,
                duration = if (label != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                message.action?.let { action -> container.applicationScope.launch { action() } }
            }
        }
    }

    SharedTransitionLayout(Modifier.fillMaxSize().background(AuraColors.Background)) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            Box(Modifier.fillMaxSize()) {
                AuraNavHost(navController, container)

                AnimatedVisibility(
                    visible = currentTab != null,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    AuraBottomBar(
                        current = currentTab,
                        onSelect = { tab -> navController.openTab(tab) },
                    )
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = if (currentTab != null) BottomBarHeight else 0.dp),
                )
            }
        }
    }
}

private fun tabOf(entry: NavBackStackEntry): AuraTab? =
    AuraTab.entries.firstOrNull { tab -> entry.destination.hierarchy.any { it.hasRoute(tab.route::class) } }

/**
 * Standard tab switching: one copy of each tab, its scroll position and state are kept,
 * and "Back" from any tab returns to Главная before leaving the app.
 */
private fun NavHostController.openTab(tab: AuraTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AuraNavHost(navController: NavHostController, container: AppContainer) {
    val openNote: (Long) -> Unit = { id -> navController.navigate(EditorRoute(noteId = id)) { launchSingleTop = true } }
    val newNote: (Long?) -> Unit = { categoryId ->
        navController.navigate(EditorRoute(categoryId = categoryId)) { launchSingleTop = true }
    }

    // Plain cross-fade between screens; the card ↔ editor morph is done by shared bounds.
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { fadeIn(tween(TRANSITION_MS)) },
        exitTransition = { fadeOut(tween(TRANSITION_MS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MS)) },
        popExitTransition = { fadeOut(tween(TRANSITION_MS)) },
        modifier = Modifier.fillMaxSize(),
    ) {
        screen<HomeRoute> {
            NotesScreen(
                favoritesOnly = false,
                onOpenNote = openNote,
                onCreateNote = newNote,
                onOpenSearch = { navController.openTab(AuraTab.Search) },
            )
        }
        screen<LabelsRoute> {
            LabelsScreen(
                onOpenLabel = { categoryId ->
                    container.homeCategoryFilter.value = categoryId
                    navController.openTab(AuraTab.Home)
                },
                onOpenArchive = { navController.navigate(ArchiveRoute) { launchSingleTop = true } },
                onOpenTrash = { navController.navigate(TrashRoute) { launchSingleTop = true } },
            )
        }
        screen<SearchRoute> { SearchScreen(onOpenNote = openNote) }
        screen<FavoritesRoute> {
            NotesScreen(
                favoritesOnly = true,
                onOpenNote = openNote,
                onCreateNote = newNote,
                onOpenSearch = { navController.openTab(AuraTab.Search) },
            )
        }
        screen<SettingsRoute> { SettingsScreen() }

        // Each screen's "Back" pops only itself, and only once: dropUnlessResumed ignores
        // a second tap during the transition (it would pop the screen below too).
        screen<EditorRoute> { EditorScreen(onBack = dropUnlessResumed { navController.popBackStack() }) }
        screen<ArchiveRoute> {
            BinScreen(mode = BinMode.Archive, onOpenNote = openNote, onBack = dropUnlessResumed { navController.popBackStack() })
        }
        screen<TrashRoute> {
            BinScreen(mode = BinMode.Trash, onOpenNote = openNote, onBack = dropUnlessResumed { navController.popBackStack() })
        }
    }
}

/** A destination whose content can take part in shared transitions. */
private inline fun <reified T : Any> NavGraphBuilder.screen(
    noinline content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        CompositionLocalProvider(LocalNavAnimatedScope provides this) { content(entry) }
    }
}

private const val TRANSITION_MS = 300
