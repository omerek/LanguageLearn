package com.languagelearn.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.languagelearn.app.AppContainer
import com.languagelearn.app.ui.home.HomeViewModel
import com.languagelearn.core.content.ContentIndex
import com.languagelearn.core.model.ItemType
import com.languagelearn.core.srs.Fsrs
import kotlinx.coroutines.flow.combine

@Composable
fun ProgressScreen(container: AppContainer, onBack: () -> Unit) {
    val index by produceState<ContentIndex?>(null) { value = container.contentRepository.index() }
    val data by combine(container.learningRepository.allCards, container.learningRepository.allTagStats) { c, t -> c to t }
        .collectAsStateWithLifecycle(initialValue = null)
    val idx = index
    val pair = data

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Geri") }
        Text("İlerleme", style = MaterialTheme.typography.headlineSmall)
        if (idx == null || pair == null) return@Column
        val (cards, stats) = pair
        val known = cards.filter { it.itemType == ItemType.CHUNK && it.stability >= HomeViewModel.KNOWN_STABILITY_DAYS }
            .map { it.itemId }.toSet()

        Text("Seviyelere göre bilinen öbekler", style = MaterialTheme.typography.titleMedium)
        idx.levels.forEach { lf ->
            val total = lf.units.sumOf { it.chunks.size }
            val got = lf.units.sumOf { u -> u.chunks.count { it.id in known } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(lf.level.name)
                Text("$got / $total")
            }
            LinearProgressIndicator(progress = { if (total == 0) 0f else got.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        }

        Text("En çok zorlandığın yapılar", style = MaterialTheme.typography.titleMedium)
        val weakest = stats.filter { it.attempts >= 3 }.sortedByDescending { it.errorRate }.take(3)
        if (weakest.isEmpty()) Text("Henüz yeterli veri yok.")
        weakest.forEach { s ->
            val title = idx.hint(s.tag)?.titleTr ?: s.tag
            Text("$title: %${(s.errorRate * 100).toInt()} hata (${s.attempts} deneme)")
        }

        val tomorrow = System.currentTimeMillis() + Fsrs.DAY
        Text("Yarınki tekrar yükü: ${cards.count { it.dueAt <= tomorrow }}", style = MaterialTheme.typography.titleMedium)
    }
}
