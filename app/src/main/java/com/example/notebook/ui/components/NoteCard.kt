package com.example.notebook.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.notebook.R
import com.example.notebook.domain.Note
import com.example.notebook.domain.NoteSearch
import com.example.notebook.ui.theme.AuraColors
import com.example.notebook.ui.theme.accent
import com.example.notebook.ui.theme.glows
import com.example.notebook.ui.util.formatShortDate

/**
 * A note in the list (Aura): a full-width glass card with the glowing color dot on the left,
 * title and one line of text in the middle, the date below, and the tag pill on the right.
 * A favorite (pinned) note shows a small heart. Tapping opens the editor with the
 * card-expands transition; a long press starts multi-select.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    categoryName: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: String? = null,
) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }

    // A note without a title uses its first line as the title, so no card looks empty.
    val title = note.title.trim().ifEmpty { note.content.trim().lineSequence().firstOrNull().orEmpty() }
    val body = if (note.title.isBlank()) {
        note.content.trim().lines().drop(1).joinToString(" ").trim()
    } else {
        note.content.trim().replace('\n', ' ')
    }
    val shownBody = if (highlight == null) body else NoteSearch.snippet(body, highlight)
    // Formatting a date is relatively expensive; remember it so scrolling does not redo it.
    val date = remember(note.updatedAt) { formatShortDate(context, note.updatedAt) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .sharedNoteBounds(noteTransitionKey(note.id))
            .pressScale(interaction)
            .glass(selected = isSelected)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { selected = isSelected }
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        GlowDot(color = note.color.accent(), glow = note.color.glows)
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = highlighted(title, highlight),
                    style = MaterialTheme.typography.titleMedium,
                    color = AuraColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (note.isPinned) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = stringResource(R.string.note_is_favorite),
                        tint = AuraColors.AccentViolet,
                        modifier = Modifier.padding(start = 6.dp).size(14.dp),
                    )
                }
            }
            if (shownBody.isNotEmpty()) {
                Text(
                    text = highlighted(shownBody, highlight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuraColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = AuraColors.TextSecondary,
            )
        }
        if (categoryName != null) {
            Spacer(Modifier.width(10.dp))
            TagPill("#$categoryName", Modifier.widthIn(max = 120.dp))
        }
    }
}

/** Marks every match of the search query in [text] with an accent highlight. */
@Composable
private fun highlighted(text: String, query: String?): AnnotatedString {
    if (query.isNullOrBlank()) return AnnotatedString(text)
    val ranges = NoteSearch.matchRanges(text, query)
    if (ranges.isEmpty()) return AnnotatedString(text)
    val style = SpanStyle(
        background = AuraColors.AccentViolet.copy(alpha = 0.35f),
        color = AuraColors.TextPrimary,
    )
    return buildAnnotatedString {
        var last = 0
        for (range in ranges) {
            append(text, last, range.first)
            withStyle(style) { append(text.substring(range)) }
            last = range.last + 1
        }
        append(text, last, text.length)
    }
}
