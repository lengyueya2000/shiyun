package com.shiyun.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.data.db.Poem
import com.shiyun.app.domain.DailyPoemSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val loading: Boolean = true,
    val poem: Poem? = null,
    val checkedIn: Boolean = false,
    val streak: Int = 0,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val today: LocalDate = LocalDate.now()
    private val poem = MutableStateFlow<Poem?>(null)

    val state: StateFlow<HomeUiState> =
        combine(poem, container.dailyRepository.observeByDate(today), container.dailyRepository.observeStreak()) { p, record, streak ->
            HomeUiState(loading = false, poem = p, checkedIn = record?.checkedIn == true, streak = streak)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            container.poemRepository.importIfNeeded()
            val pool = container.poemRepository.featured().ifEmpty { container.poemRepository.all() }
            poem.value = DailyPoemSelector.select(pool, today)
        }
    }

    fun checkIn() {
        val current = poem.value ?: return
        viewModelScope.launch { container.dailyRepository.checkIn(today, current.id) }
    }
}
