package com.example.notebook.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe destinations for Navigation Compose.
 * Each screen is a @Serializable class; arguments are its fields,
 * so the compiler checks them instead of hand-written "editor/{id}" strings.
 */

@Serializable data object NotesRoute

/**
 * noteId == null means "create a new note"; [categoryId] then pre-selects its category
 * (used when creating a note while a category filter is active).
 */
@Serializable data class EditorRoute(val noteId: Long? = null, val categoryId: Long? = null)

@Serializable data object CategoriesRoute

@Serializable data object ArchiveRoute

@Serializable data object TrashRoute

@Serializable data object SettingsRoute
