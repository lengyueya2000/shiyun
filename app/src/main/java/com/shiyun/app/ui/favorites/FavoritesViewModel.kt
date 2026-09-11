package com.shiyun.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.data.db.Poem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FavoritesUiState(val poems: List<Poem> = emptyList())

class FavoritesViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<FavoritesUiState> =
        container.favoriteRepository.observeAll().map { favorites ->
            FavoritesUiState(poems = favorites.mapNotNull { container.poemRepository.poemById(it.poemId) })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoritesUiState())
}
