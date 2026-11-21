package com.example.notebook.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/*
 * "Card opens into the editor" (shared element transition).
 *
 * The whole navigation graph sits inside a SharedTransitionLayout. A note card and the
 * editor screen both mark themselves with the same key ("note-42"); while the screens
 * switch, Compose morphs the card's bounds into the full-screen editor and back.
 * These locals hand the needed scopes down without passing them through every function.
 */

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The animation scope of the current navigation destination. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Key that links a note card with its editor; a brand-new note links with the "+" button. */
fun noteTransitionKey(noteId: Long?): String = if (noteId == null) "note-new" else "note-$noteId"

/**
 * Marks this element as one end of the card ↔ editor transition.
 * Outside a SharedTransitionLayout (tests, previews) it does nothing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedNoteBounds(key: String): Modifier {
    val transitionScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedScope.current ?: return this
    return with(transitionScope) {
        this@sharedNoteBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedScope,
            enter = fadeIn(),
            exit = fadeOut(),
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(GlassShape),
        )
    }
}
