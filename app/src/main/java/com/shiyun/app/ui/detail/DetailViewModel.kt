package com.shiyun.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.data.db.Poem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(val poem: Poem? = null, val isFavorite: Boolean = false)

class DetailViewModel(private val container: AppContainer, poemId: Long) : ViewModel() {
    val state: StateFlow<DetailUiState> =
        combine(
            container.poemRepository.observePoem(poemId),
            container.favoriteRepository.observeIsFavorite(poemId),
        ) { poem, favorite ->
            DetailUiState(poem = poem, isFavorite = favorite)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    fun toggleFavorite() {
        val poem = state.value.poem ?: return
        viewModelScope.launch { container.favoriteRepository.toggle(poem.id) }
    }
}
