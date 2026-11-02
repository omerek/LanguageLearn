package com.languagelearn.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.languagelearn.core.content.ContentParser
import com.languagelearn.core.model.Reading

/** Sentence with a blank and three chunk options. Wrong options are outlined in red and disabled. */
@Composable
fun ClozeOptions(before: String, after: String, options: List<String>, wrong: Set<String>, settled: Boolean, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("$before ____ $after".trim(), style = MaterialTheme.typography.titleLarge)
        options.forEach { option ->
            val isWrong = option in wrong
            OutlinedButton(
                onClick = { onPick(option) },
                enabled = !settled && !isWrong,
                border = BorderStroke(if (isWrong) 2.dp else 1.dp, if (isWrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(if (isWrong) "$option  ✗" else option) }
        }
    }
}

/** Reading text; glossed words are underlined and show their Turkish meaning on tap. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingView(reading: Reading) {
    var gloss by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showTranslation by remember { mutableStateOf(false) }
    val glossByWord: Map<String, Pair<String, String>> = remember(reading) {
        reading.glosses.flatMap { (phrase, tr) -> ContentParser.words(phrase).map { it to (phrase to tr) } }.toMap()
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(reading.titleTr, style = MaterialTheme.typography.titleLarge)
        reading.paragraphs.forEach { p ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                p.en.split(' ').forEach { word ->
                    val g = glossByWord[ContentParser.normalize(word)]
                    Text(
                        word,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (g != null) TextDecoration.Underline else null,
                        color = if (g != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = if (g != null) Modifier.clickable { gloss = g } else Modifier,
                    )
                }
            }
            if (showTranslation) Text(p.tr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        gloss?.let { (en, tr) ->
            Card(Modifier.fillMaxWidth()) { Text("$en: $tr", Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge) }
        }
        TextButton(onClick = { showTranslation = !showTranslation }) {
            Text(if (showTranslation) "Çeviriyi gizle" else "Çeviriyi göster")
        }
    }
}
