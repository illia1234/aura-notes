package com.example.notebook.ui.screens.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.domain.SortOrder
import com.example.notebook.ui.LocalAppSettings
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.ScreenTitle
import com.example.notebook.ui.components.SectionLabel
import com.example.notebook.ui.components.glass
import com.example.notebook.ui.components.labelRes
import com.example.notebook.ui.components.tabBarInset
import com.example.notebook.ui.theme.AuraColors

/** "Настройки" tab; every change is saved to DataStore immediately. */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel {
        val c = appContainer()
        SettingsViewModel(c.settingsRepository, c.backupRepository, c.userMessenger, c.applicationScope)
    },
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current

    // System file dialogs (Storage Access Framework): the user picks where to save / what to open.
    // No storage permission is needed, the app only gets access to that one file.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::export) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::import) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = tabBarInset() + 16.dp),
    ) {
        ScreenTitle(stringResource(R.string.tab_settings))

        SectionLabel(stringResource(R.string.sort))
        GlassGroup {
            RadioGroup(
                options = SortOrder.entries,
                selected = settings.sortOrder,
                label = { stringResource(it.labelRes()) },
                onSelect = viewModel::setSortOrder,
            )
        }

        SectionLabel(stringResource(R.string.settings_text_size), Modifier.padding(top = 12.dp))
        GlassGroup {
            RadioGroup(
                options = EditorTextSize.entries,
                selected = settings.editorTextSize,
                label = { stringResource(it.labelRes()) },
                onSelect = viewModel::setEditorTextSize,
            )
        }

        SectionLabel(stringResource(R.string.backup_section), Modifier.padding(top = 12.dp))
        GlassGroup {
            if (viewModel.isBusy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
            }
            ActionRow(
                icon = Icons.Outlined.Upload,
                title = stringResource(R.string.backup_export),
                subtitle = stringResource(R.string.backup_export_hint),
                enabled = !viewModel.isBusy,
                onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
            )
            RowDivider()
            ActionRow(
                icon = Icons.Outlined.Download,
                title = stringResource(R.string.backup_import),
                subtitle = stringResource(R.string.backup_import_hint),
                enabled = !viewModel.isBusy,
                // Some file managers label .json files as plain text or as generic binary.
                onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
            )
        }

        SectionLabel(stringResource(R.string.settings_about), Modifier.padding(top = 12.dp))
        GlassGroup {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, color = AuraColors.TextPrimary)
                Text(
                    text = stringResource(R.string.settings_version_value, appVersion(context)),
                    style = MaterialTheme.typography.labelMedium,
                    color = AuraColors.TextSecondary,
                )
            }
        }
    }
}

/** A glass panel that groups related settings. */
@Composable
private fun GlassGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().glass().padding(vertical = 4.dp), content = content)
}

@Composable
private fun RowDivider() = HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = AuraColors.RimDim)

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(horizontal = 8.dp),
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = AuraColors.AccentViolet,
                        unselectedColor = AuraColors.TextSecondary,
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                Text(label(option), style = MaterialTheme.typography.bodyLarge, color = AuraColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AuraColors.TextPrimary)
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AuraColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = AuraColors.TextSecondary)
        }
    }
}

private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull() ?: "—"

private fun EditorTextSize.labelRes(): Int = when (this) {
    EditorTextSize.SMALL -> R.string.text_size_small
    EditorTextSize.MEDIUM -> R.string.text_size_medium
    EditorTextSize.LARGE -> R.string.text_size_large
}
