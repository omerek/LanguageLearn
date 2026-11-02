package com.languagelearn.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

@Composable
fun HomeScreen(container: AppContainer, onStartSession: () -> Unit, onOpenProgress: () -> Unit) {
    val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(container) } })
    val state by vm.state.collectAsStateWithLifecycle()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Bugün", style = MaterialTheme.typography.headlineSmall)
        TodaySummaryCard(state)
        LevelProgressRow(state)
        StreakBadge(state.streakDays)
        Button(onClick = onStartSession, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Oturumu başlat")
        }
        OutlinedButton(onClick = onOpenProgress, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("İlerleme")
        }
    }
}

@Composable
private fun TodaySummaryCard(state: HomeUiState) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Stat("Tekrar", state.dueCount.toString())
            Stat("Yeni", state.newToday.toString())
            Stat("Bilinen öbek", state.chunksKnown.toString())
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LevelProgressRow(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Seviye ${state.level.name}", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { state.levelProgress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StreakBadge(days: Int) {
    Text(
        if (days > 0) "$days günlük seri" else "Bugün ilk oturumunla seriye başla",
        style = MaterialTheme.typography.bodyMedium,
    )
}
