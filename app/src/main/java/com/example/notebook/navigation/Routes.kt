package com.example.notebook.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe destinations for Navigation Compose.
 * Each screen is a @Serializable class; arguments are its fields,
 * so the compiler checks them instead of hand-written "editor/{id}" strings.
 */

@Serializable data object NotesRoute

/** noteId == null means "create a new note". */
@Serializable data class EditorRoute(val noteId: Long? = null)

@Serializable data object SearchRoute

@Serializable data object CategoriesRoute

@Serializable data object ArchiveRoute

@Serializable data object TrashRoute

@Serializable data object SettingsRoute
