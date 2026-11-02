package com.languagelearn.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.languagelearn.app.AppContainer
import com.languagelearn.core.model.CefrLevel
import com.languagelearn.core.model.ItemType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loading: Boolean = true,
    val dueCount: Int = 0,
    val newToday: Int = 0,
    val chunksKnown: Int = 0,
    val level: CefrLevel = CefrLevel.A1,
    /** 0..1 progress inside the current level band. */
    val levelProgress: Float = 0f,
    val streakDays: Int = 0,
)

class HomeViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.learningRepository

    val state: StateFlow<HomeUiState> = combine(repo.profile, repo.allCards) { profile, cards ->
        if (profile == null) return@combine HomeUiState()
        val now = System.currentTimeMillis()
        val level = CefrLevel.forAbility(profile.ability)
        val due = cards.count { it.dueAt <= now }
        HomeUiState(
            loading = false,
            dueCount = due,
            newToday = (profile.dailyNewLimit - (due - 30).coerceAtLeast(0) / 5).coerceAtLeast(0),
            chunksKnown = cards.count { it.itemType == ItemType.CHUNK && it.stability >= KNOWN_STABILITY_DAYS },
            level = level,
            levelProgress = ((profile.ability - level.bandStart) / 20.0).toFloat().coerceIn(0f, 1f),
            streakDays = profile.streakDays,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        /** A chunk counts as known once it would be recalled 90% of the time after a week. */
        const val KNOWN_STABILITY_DAYS = 7.0
    }
}
