package com.example.notebook.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe destinations for Navigation Compose.
 * Each screen is a @Serializable class; arguments are its fields,
 * so the compiler checks them instead of hand-written "editor/{id}" strings.
 */

// The five tabs of the bottom bar (docs/REDESIGN_AURA.md).
@Serializable data object HomeRoute

@Serializable data object LabelsRoute

@Serializable data object SearchRoute

@Serializable data object FavoritesRoute

@Serializable data object SettingsRoute

/**
 * noteId == null means "create a new note"; [categoryId] then pre-selects its tag
 * (used when creating a note while a tag filter is active).
 */
@Serializable data class EditorRoute(val noteId: Long? = null, val categoryId: Long? = null)

@Serializable data object ArchiveRoute

@Serializable data object TrashRoute
