package com.languagelearn.app.ui.placement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.languagelearn.app.AppContainer
import com.languagelearn.core.model.CefrLevel
import kotlinx.coroutines.launch
import java.time.LocalDate

private val levelInfo = listOf(
    CefrLevel.A1 to "Yeni başlıyorum. Kendimi tanıtmak, bir şey istemek.",
    CefrLevel.A2 to "Basit cümleler kurabiliyorum. Geçmişi ve planlarımı anlatmak istiyorum.",
    CefrLevel.B1 to "Günlük konuşmaları anlıyorum. Bağlaçlı, yan tümceli cümleler kurmak istiyorum.",
    CefrLevel.B2 to "Rahat okuyorum. Varsayım, zıtlık ve edilgen yapıları oturtmak istiyorum.",
    CefrLevel.C1 to "İleri seviyedeyim. Vurgu ve üslup yapılarını çalışmak istiyorum.",
)

@Composable
fun PlacementScreen(container: AppContainer, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Nereden başlayalım?", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Seviyeni seç. Uygulama cevaplarına göre zorluğu kendisi ayarlar; yanlış seçim sorun olmaz.",
            style = MaterialTheme.typography.bodyMedium,
        )
        levelInfo.forEach { (level, text) ->
            Card(
                Modifier.fillMaxWidth().clickable {
                    scope.launch {
                        container.learningRepository.savePlacement(level, LocalDate.now().toEpochDay())
                        onDone()
                    }
                },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(level.name, style = MaterialTheme.typography.titleLarge)
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
