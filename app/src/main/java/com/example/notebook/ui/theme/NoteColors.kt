package com.example.notebook.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.notebook.domain.NoteColor

/**
 * Neon accent for each note color. In Aura the color is not a card fill any more:
 * it is the glowing dot on the card and the soft glow at the top of the editor.
 * The stored names (NoteColor) are unchanged, so old notes and backups keep their colors.
 */
fun NoteColor.accent(): Color = when (this) {
    NoteColor.DEFAULT -> Color(0xFF6B6B6B) // neutral gray, no glow
    NoteColor.CORAL -> Color(0xFFFF8A8A)
    NoteColor.PEACH -> Color(0xFFFFB27A) // 🟠 персик
    NoteColor.SAND -> Color(0xFFFFE27A) // 🟡 лимон
    NoteColor.MINT -> Color(0xFF7CF5C8) // 🟢 мята
    NoteColor.SAGE -> Color(0xFFA6E88A)
    NoteColor.FOG -> Color(0xFF7FD4FF) // 🔵 небо
    NoteColor.STORM -> Color(0xFF8AA8FF)
    NoteColor.DUSK -> Color(0xFFC9A7FF) // 🟣 лаванда
}

/** Only colored notes glow; "Без цвета" is a quiet gray dot. */
val NoteColor.glows: Boolean get() = this != NoteColor.DEFAULT
