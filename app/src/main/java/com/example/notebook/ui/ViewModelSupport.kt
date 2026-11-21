package com.example.notebook.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.notebook.AppContainer
import com.example.notebook.NotebookApplication

/**
 * Gives ViewModel initializers access to the app's [AppContainer] (manual DI).
 * Usage in a screen: `viewModel { NotesViewModel(appContainer().noteRepository, ...) }`.
 */
fun CreationExtras.appContainer(): AppContainer =
    (checkNotNull(this[APPLICATION_KEY]) as NotebookApplication).container
