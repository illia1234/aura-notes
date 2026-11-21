package com.example.notebook.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.EmptyState
import com.example.notebook.ui.components.NoteCard
import com.example.notebook.ui.components.ScreenTitle
import com.example.notebook.ui.components.fadeTopEdge
import com.example.notebook.ui.components.glass
import com.example.notebook.ui.components.tabBarInset
import com.example.notebook.ui.theme.AuraColors

/** "Поиск" tab: a glass search field and live results with highlighted matches. */
@Composable
fun SearchScreen(
    onOpenNote: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel {
        val c = appContainer()
        SearchViewModel(c.noteRepository, c.categoryRepository, c.settingsRepository)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Opening the tab with an empty field puts the cursor there right away.
    LaunchedEffect(Unit) { if (viewModel.query.isEmpty()) focusRequester.requestFocus() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            ScreenTitle(stringResource(R.string.tab_search), Modifier.padding(top = 16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .glass(CircleShape)
                    .padding(start = 16.dp, end = 4.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = AuraColors.TextSecondary)
                Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    if (viewModel.query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = AuraColors.TextSecondary,
                        )
                    }
                    BasicTextField(
                        value = viewModel.query,
                        onValueChange = viewModel::onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = AuraColors.TextPrimary),
                        cursorBrush = SolidColor(AuraColors.AccentViolet),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                }
                if (viewModel.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.search_clear), tint = AuraColors.TextPrimary)
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = tabBarInset() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fadeTopEdge(16.dp).imePadding(),
        ) {
            if (state.showNothingFound) {
                item(key = "nothing") {
                    EmptyState(Icons.Outlined.SearchOff, stringResource(R.string.search_nothing_found))
                }
            }
            items(state.results, key = { it.note.id }) { item ->
                NoteCard(
                    note = item.note,
                    categoryName = item.categoryName,
                    isSelected = false,
                    onClick = { onOpenNote(item.note.id) },
                    onLongClick = {},
                    highlight = state.query,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}
