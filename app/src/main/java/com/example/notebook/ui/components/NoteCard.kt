package com.example.notebook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notebook.R
import com.example.notebook.domain.Note
import com.example.notebook.domain.NoteSearch
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.notebook.ui.theme.containerColor
import com.example.notebook.ui.theme.contentColor
import com.example.notebook.ui.theme.hasOutline
import com.example.notebook.ui.util.formatShortDate

private val CardShape = RoundedCornerShape(16.dp)

/**
 * A note in the list or grid: title (2 lines), start of the text, category chip,
 * modification date and a pin for pinned notes. Rounded 16 dp, no shadow.
 */
@Composable
fun NoteCard(
    note: Note,
    categoryName: String?,
    maxContentLines: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: String? = null,
) {
    val border = when {
        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        note.color.hasOutline -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> null
    }
    Surface(
        shape = CardShape,
        color = note.color.containerColor(),
        contentColor = note.color.contentColor(),
        border = border,
        modifier = modifier
            .clip(CardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { selected = isSelected },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (note.title.isNotBlank() || note.isPinned) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = highlighted(note.title.trim(), highlight),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.isPinned) {
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = stringResource(R.string.note_is_pinned),
                            modifier = Modifier.padding(start = 4.dp).size(18.dp),
                        )
                    }
                }
            }
            if (note.content.isNotBlank()) {
                Text(
                    text = highlighted(
                        if (highlight == null) note.content.trim() else NoteSearch.snippet(note.content.trim(), highlight),
                        highlight,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = maxContentLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NoteCardFooter(categoryName = categoryName, updatedAt = note.updatedAt)
        }
    }
}

@Composable
private fun NoteCardFooter(categoryName: String?, updatedAt: Long) {
    val context = LocalContext.current
    val muted = LocalContentColor.current.copy(alpha = 0.75f)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (categoryName != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LocalContentColor.current.copy(alpha = 0.08f),
                modifier = Modifier.padding(end = 8.dp).weight(1f, fill = false),
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Text(
            text = formatShortDate(context, updatedAt),
            style = MaterialTheme.typography.labelSmall,
            color = muted,
            maxLines = 1,
        )
    }
}

/** Marks every match of the search query in [text] with a highlight background. */
@Composable
private fun highlighted(text: String, query: String?): AnnotatedString {
    if (query.isNullOrBlank()) return AnnotatedString(text)
    val ranges = NoteSearch.matchRanges(text, query)
    if (ranges.isEmpty()) return AnnotatedString(text)
    val style = SpanStyle(
        background = MaterialTheme.colorScheme.tertiaryContainer,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
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
