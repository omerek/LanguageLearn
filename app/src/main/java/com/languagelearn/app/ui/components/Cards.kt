package com.languagelearn.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.languagelearn.app.ui.theme.FeedbackColors
import com.languagelearn.core.hint.Feedback
import com.languagelearn.core.hint.FeedbackKind
import com.languagelearn.core.model.Chunk
import com.languagelearn.core.model.ChunkKind

@Composable
fun SessionProgressBar(done: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("$done / $total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PromptCard(label: String, text: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Graded feedback: green for correct, amber for hints. The title always says which. */
@Composable
fun FeedbackCard(feedback: Feedback, attempt: Int) {
    val color = if (feedback.kind == FeedbackKind.CORRECT) FeedbackColors.correct else FeedbackColors.hint
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, color),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(feedback.titleTr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            feedback.answerEn?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Text(feedback.messageTr, style = MaterialTheme.typography.bodyMedium)
            val stage = when (feedback.kind) {
                FeedbackKind.NUDGE -> "$attempt. deneme · yönlendirme"
                FeedbackKind.RULE -> "$attempt. deneme · kural"
                FeedbackKind.REVEAL -> "Birazdan tekrar soracağım"
                FeedbackKind.CORRECT -> null
            }
            stage?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** A new chunk: big, with meaning, kind and examples in which the chunk is highlighted. No rule. */
@Composable
fun ChunkIntroCard(chunk: Chunk) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Yeni öbek · ${kindTr(chunk.kind)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(chunk.en, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(chunk.tr, style = MaterialTheme.typography.titleMedium)
            chunk.examples.take(2).forEach { ex -> Text(highlight(ex, chunk.en), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

private fun highlight(sentence: String, chunk: String) = buildAnnotatedString {
    val i = sentence.indexOf(chunk, ignoreCase = true)
    if (i < 0) {
        append(sentence)
    } else {
        append(sentence.substring(0, i))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(sentence.substring(i, i + chunk.length)) }
        append(sentence.substring(i + chunk.length))
    }
}

fun kindTr(kind: ChunkKind) = when (kind) {
    ChunkKind.FRAME -> "cümle kalıbı"
    ChunkKind.COLLOCATION -> "eşdizim"
    ChunkKind.FIXED -> "kalıp ifade"
    ChunkKind.CONNECTOR -> "bağlaç"
    ChunkKind.TIME -> "zaman ifadesi"
}
