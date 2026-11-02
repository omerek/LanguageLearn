package com.languagelearn.app.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.languagelearn.app.AppContainer
import com.languagelearn.app.ui.components.AnswerTray
import com.languagelearn.app.ui.components.ChunkIntroCard
import com.languagelearn.app.ui.components.ChunkTileBank
import com.languagelearn.app.ui.components.ClozeOptions
import com.languagelearn.app.ui.components.FeedbackCard
import com.languagelearn.app.ui.components.PromptCard
import com.languagelearn.app.ui.components.ReadingView
import com.languagelearn.app.ui.components.SessionProgressBar
import com.languagelearn.core.exercise.Exercise
import com.languagelearn.core.hint.FeedbackKind

@Composable
fun SessionScreen(container: AppContainer, onExit: () -> Unit) {
    val vm: SessionViewModel = viewModel(factory = viewModelFactory { initializer { SessionViewModel(container) } })
    val state by vm.state.collectAsStateWithLifecycle()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("Kapat") }
            Box(Modifier.weight(1f)) { SessionProgressBar(state.done, state.total) }
        }
        val summary = state.summary
        if (summary != null) {
            SessionSummaryView(summary, onExit)
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val ex = state.exercise) {
                is Exercise.ChunkIntro -> ChunkIntroCard(ex.chunk)
                is Exercise.Assemble -> AssembleContent(ex, state, vm::onTileTapped)
                is Exercise.Cloze -> {
                    PromptCard("Boşluğu doldur", ex.sentence.tr)
                    ClozeOptions(ex.before, ex.after, ex.options, state.wrongOptions, state.settled, vm::onClozeOption)
                }
                is Exercise.Read -> ReadingView(ex.reading)
                null -> Unit
            }
            state.feedback?.let { FeedbackCard(it, attempt = attemptNumber(state)) }
        }
        PrimaryActionButton(state, onCheck = vm::onCheck, onContinue = vm::onContinue)
    }
}

@Composable
private fun AssembleContent(ex: Exercise.Assemble, state: SessionUiState, onTile: (String) -> Unit) {
    val tray = state.trayTileIds.mapNotNull { id -> ex.tiles.firstOrNull { it.id == id } }
    val bank = ex.tiles.filter { it.id !in state.trayTileIds }
    val focus = state.feedback?.takeIf { it.kind != FeedbackKind.CORRECT }?.focusSegment
    PromptCard(if (ex.newChunkId != null) "İngilizcesini kur · yeni öbek" else "İngilizcesini kur", ex.sentence.tr)
    AnswerTray(tray, focus, enabled = !state.settled) { onTile(it.id) }
    ChunkTileBank(bank, enabled = !state.settled) { onTile(it.id) }
}

/** Counts wrong attempts shown so far from the feedback stage. */
private fun attemptNumber(state: SessionUiState): Int = when (state.feedback?.kind) {
    FeedbackKind.NUDGE -> 1
    FeedbackKind.RULE -> 2
    else -> 3
}

@Composable
private fun PrimaryActionButton(state: SessionUiState, onCheck: () -> Unit, onContinue: () -> Unit) {
    val (label, action, enabled) = when (val ex = state.exercise) {
        is Exercise.Assemble -> when {
            state.settled -> Triple("Devam", onContinue, true)
            state.feedback != null && state.feedback.kind != FeedbackKind.CORRECT -> Triple("Tekrar dene", onCheck, state.trayTileIds.isNotEmpty())
            else -> Triple("Kontrol et", onCheck, state.trayTileIds.size >= ex.solutionTileCount)
        }
        is Exercise.Cloze -> Triple("Devam", onContinue, state.settled)
        else -> Triple("Devam", onContinue, true)
    }
    Button(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(label) }
}

@Composable
private fun SessionSummaryView(summary: SessionSummary, onExit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Oturum bitti", style = MaterialTheme.typography.headlineSmall)
        Text("${summary.exercises} alıştırmanın ${summary.firstTry} tanesini ilk denemede bildin.")
        Text("Yeni öbek: ${summary.newChunks}")
        if (summary.weakTagTitles.isNotEmpty()) {
            Text("Zorlandığın yapılar: ${summary.weakTagTitles.joinToString(", ")}")
            Text("Bunlar sonraki oturumlarda öne alınacak.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Ana sayfaya dön") }
    }
}
