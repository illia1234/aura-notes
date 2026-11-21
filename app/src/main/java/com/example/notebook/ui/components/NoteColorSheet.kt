package com.example.notebook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notebook.R
import com.example.notebook.domain.NoteColor
import com.example.notebook.ui.theme.accent

/** Russian name of a note color, for TalkBack and labels. */
fun NoteColor.labelRes(): Int = when (this) {
    NoteColor.DEFAULT -> R.string.color_default
    NoteColor.CORAL -> R.string.color_coral
    NoteColor.PEACH -> R.string.color_peach
    NoteColor.SAND -> R.string.color_sand
    NoteColor.MINT -> R.string.color_mint
    NoteColor.SAGE -> R.string.color_sage
    NoteColor.FOG -> R.string.color_fog
    NoteColor.STORM -> R.string.color_storm
    NoteColor.DUSK -> R.string.color_dusk
}

/**
 * Bottom sheet with a row of 48 dp color circles; the current one has a check mark.
 * [selected] is null when several notes with different colors are selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteColorSheet(
    selected: NoteColor?,
    onSelect: (NoteColor) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.note_color_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            NoteColor.entries.forEach { color ->
                ColorCircle(color = color, isSelected = color == selected, onClick = { onSelect(color) })
            }
        }
    }
}

@Composable
private fun ColorCircle(color: NoteColor, isSelected: Boolean, onClick: () -> Unit) {
    val name = stringResource(color.labelRes())
    val outline = when {
        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        color == NoteColor.DEFAULT -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        else -> null
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color.accent())
            .then(if (outline != null) Modifier.border(outline, CircleShape) else Modifier)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
    ) {
        if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF121212))
        }
    }
}
