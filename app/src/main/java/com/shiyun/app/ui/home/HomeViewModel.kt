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
    val loadFailed: Boolean = false,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val today: LocalDate = LocalDate.now()
    private val poem = MutableStateFlow<Poem?>(null)
    private val loading = MutableStateFlow(true)
    private val loadFailed = MutableStateFlow(false)

    val state: StateFlow<HomeUiState> =
        combine(
            poem,
            container.dailyRepository.observeByDate(today),
            container.dailyRepository.observeStreak(),
            loading,
            loadFailed,
        ) { p, record, streak, isLoading, failed ->
            HomeUiState(
                loading = isLoading,
                poem = p,
                checkedIn = record?.checkedIn == true,
                streak = streak,
                loadFailed = failed,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        load()
    }

    fun load() {
        loading.value = true
        loadFailed.value = false
        viewModelScope.launch {
            try {
                container.poemRepository.importIfNeeded()
                val repo = container.poemRepository
                val pool = repo.featured().ifEmpty { repo.all() }
                if (pool.isEmpty() && repo.isEmpty()) {
                    loadFailed.value = true
                } else {
                    poem.value = DailyPoemSelector.select(pool, today)
                }
            } catch (e: Exception) {
                loadFailed.value = true
            } finally {
                loading.value = false
            }
        }
    }

    fun checkIn() {
        val current = poem.value ?: return
        viewModelScope.launch { container.dailyRepository.checkIn(today, current.id) }
    }
}
