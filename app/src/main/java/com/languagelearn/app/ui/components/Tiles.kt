package com.languagelearn.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.languagelearn.app.ui.theme.FeedbackColors
import com.languagelearn.core.exercise.Tile

/** One chunk tile. At least 48dp tall; TalkBack reads "öbek, <text>, ekle/çıkar". */
@Composable
fun ChunkTile(tile: Tile, inTray: Boolean, highlighted: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = if (highlighted) FeedbackColors.hint else MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(if (highlighted) 2.dp else 1.5.dp, color),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "öbek, ${tile.text}, ${if (inTray) "çıkar" else "ekle"}" },
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                tile.text,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (highlighted) TextDecoration.Underline else null,
            )
        }
    }
}

/** Tiles the learner has placed, in order. The tile the error is on is underlined. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnswerTray(tiles: List<Tile>, focusSegment: Int?, enabled: Boolean, onTileClick: (Tile) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
    ) {
        FlowRow(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (tiles.isEmpty()) {
                Text("Öbeklere dokunarak cümleyi kur", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            }
            tiles.forEach { t ->
                ChunkTile(t, inTray = true, highlighted = focusSegment != null && t.segmentIndex == focusSegment, enabled = enabled) { onTileClick(t) }
            }
        }
    }
}

/** Shuffled tiles not yet used, distractors included. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChunkTileBank(tiles: List<Tile>, enabled: Boolean, onTileClick: (Tile) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tiles.forEach { t -> ChunkTile(t, inTray = false, highlighted = false, enabled = enabled) { onTileClick(t) } }
    }
}
