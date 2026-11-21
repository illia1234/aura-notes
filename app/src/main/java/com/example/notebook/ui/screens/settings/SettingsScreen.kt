package com.example.notebook.ui.screens.settings

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.domain.ListLayout
import com.example.notebook.domain.SortOrder
import com.example.notebook.domain.ThemeMode
import com.example.notebook.ui.LocalAppSettings
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.BackButton
import com.example.notebook.ui.components.labelRes

/** Settings from docs/SPEC.md; every change is saved to DataStore immediately. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(appContainer().settingsRepository) },
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.settings_theme))
            RadioGroup(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                label = { stringResource(it.labelRes()) },
                onSelect = viewModel::setThemeMode,
            )
            val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.settings_dynamic_color_hint)) },
                trailingContent = {
                    Switch(checked = settings.dynamicColor && dynamicAvailable, onCheckedChange = null, enabled = dynamicAvailable)
                },
                modifier = Modifier.toggleable(
                    value = settings.dynamicColor,
                    enabled = dynamicAvailable,
                    role = Role.Switch,
                    onValueChange = viewModel::setDynamicColor,
                ),
            )

            Divider()
            SectionTitle(stringResource(R.string.settings_list_layout))
            RadioGroup(
                options = ListLayout.entries,
                selected = settings.listLayout,
                label = { stringResource(if (it == ListLayout.GRID) R.string.layout_grid else R.string.layout_list) },
                onSelect = viewModel::setListLayout,
            )

            Divider()
            SectionTitle(stringResource(R.string.sort))
            RadioGroup(
                options = SortOrder.entries,
                selected = settings.sortOrder,
                label = { stringResource(it.labelRes()) },
                onSelect = viewModel::setSortOrder,
            )

            Divider()
            SectionTitle(stringResource(R.string.settings_text_size))
            RadioGroup(
                options = EditorTextSize.entries,
                selected = settings.editorTextSize,
                label = { stringResource(it.labelRes()) },
                onSelect = viewModel::setEditorTextSize,
            )

            Divider()
            SectionTitle(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(appVersion(context)) },
            )
        }
    }
}

@Composable
private fun <T> RadioGroup(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            val isSelected = option == selected
            ListItem(
                headlineContent = { Text(label(option)) },
                leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                modifier = Modifier.selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = { onSelect(option) },
                ),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(top = 8.dp))

private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull() ?: "—"

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun EditorTextSize.labelRes(): Int = when (this) {
    EditorTextSize.SMALL -> R.string.text_size_small
    EditorTextSize.MEDIUM -> R.string.text_size_medium
    EditorTextSize.LARGE -> R.string.text_size_large
}
