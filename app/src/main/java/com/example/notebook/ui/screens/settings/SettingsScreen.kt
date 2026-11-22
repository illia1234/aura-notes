package com.example.notebook.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.notebook.domain.formatTime
import com.example.notebook.ui.screens.calendar.TimeDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.example.notebook.domain.AuraStyle
import com.example.notebook.ui.theme.auraBackground
import com.example.notebook.ui.theme.palette
import com.example.notebook.ui.util.AppLanguage
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
        SettingsViewModel(c.settingsRepository, c.backupRepository, c.userMessenger, c.applicationScope, c.workRepository)
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

        // Display style (v1.1): a row of small previews of each background.
        SectionLabel(stringResource(R.string.settings_style))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        ) {
            AuraStyle.entries.forEach { style ->
                StyleTile(
                    style = style,
                    selected = style == settings.style,
                    onClick = { viewModel.setStyle(style) },
                )
            }
        }

        // App language (v1.1). Android 13+ only; older versions follow the system language.
        if (AppLanguage.isSupported) {
            var language by remember { mutableStateOf(AppLanguage.current(context)) }
            SectionLabel(stringResource(R.string.settings_language), Modifier.padding(top = 12.dp))
            GlassGroup {
                RadioGroup(
                    options = AppLanguage.entries,
                    selected = language,
                    label = { stringResource(it.labelRes()) },
                    onSelect = {
                        language = it
                        AppLanguage.set(context, it) // the screen restarts in the new language
                    },
                )
            }
        }

        // Work log (v1.2): pay per hour and the time a tap puts on a day.
        SectionLabel(stringResource(R.string.settings_work), Modifier.padding(top = 12.dp))
        GlassGroup { WorkSettings(settings.hourlyRateCents, settings.defaultShiftStart, settings.defaultShiftEnd, viewModel) }

        SectionLabel(stringResource(R.string.sort), Modifier.padding(top = 12.dp))
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

/** A small preview of a display style: its background with a mini glass card, and its name. */
@Composable
private fun StyleTile(style: AuraStyle, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val palette = style.palette()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 76.dp, height = 104.dp)
                .clip(shape)
                .auraBackground(palette)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) AuraColors.TextPrimary else AuraColors.RimBright,
                    shape = shape,
                )
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        ) {
            // Mini glass card with the style's rim, to show how cards look on it.
            Box(
                Modifier
                    .size(width = 52.dp, height = 22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AuraColors.GlassTop)
                    .border(1.dp, Brush.linearGradient(listOf(palette.rimBright, palette.rimDim)), RoundedCornerShape(8.dp)),
            )
        }
        Text(
            text = stringResource(style.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AuraColors.TextPrimary else AuraColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun AuraStyle.labelRes(): Int = when (this) {
    AuraStyle.BLACK -> R.string.style_black
    AuraStyle.AURORA -> R.string.style_aurora
    AuraStyle.NEON -> R.string.style_neon
    AuraStyle.SUNSET -> R.string.style_sunset
    AuraStyle.GRAPHITE -> R.string.style_graphite
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.RUSSIAN -> R.string.language_russian
    AppLanguage.ENGLISH -> R.string.language_english
}

@Composable
private fun WorkSettings(rateCents: Int, shiftStart: Int, shiftEnd: Int, viewModel: SettingsViewModel) {
    var rateText by remember(rateCents) {
        mutableStateOf(if (rateCents % 100 == 0) (rateCents / 100).toString() else "%d,%02d".format(rateCents / 100, rateCents % 100))
    }
    var rateError by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Boolean?>(null) } // true = start, false = end
    val focusManager = LocalFocusManager.current

    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        OutlinedTextField(
            value = rateText,
            onValueChange = {
                rateText = it
                rateError = false
            },
            label = { Text(stringResource(R.string.settings_rate)) },
            singleLine = true,
            isError = rateError,
            suffix = { Text("€") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                rateError = !viewModel.setHourlyRate(rateText)
                if (!rateError) focusManager.clearFocus()
            }),
            modifier = Modifier
                .fillMaxWidth()
                // Saved when the field loses focus too, not only on "Done".
                .onFocusChanged { if (!it.isFocused && rateText.isNotBlank()) rateError = !viewModel.setHourlyRate(rateText) },
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.settings_default_shift), style = MaterialTheme.typography.bodyLarge, color = AuraColors.TextPrimary, modifier = Modifier.weight(1f))
            TextButton(onClick = { editing = true }) { Text(formatTime(shiftStart)) }
            Text("–", color = AuraColors.TextSecondary)
            TextButton(onClick = { editing = false }) { Text(formatTime(shiftEnd)) }
        }
    }

    editing?.let { isStart ->
        TimeDialog(
            initialMinute = if (isStart) shiftStart else shiftEnd,
            onPick = { minute ->
                if (isStart) viewModel.setDefaultShift(minute, shiftEnd) else viewModel.setDefaultShift(shiftStart, minute)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}
