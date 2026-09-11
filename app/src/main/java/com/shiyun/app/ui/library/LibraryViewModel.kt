package com.shiyun.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.data.db.Poem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val query: String = "",
    val dynasty: String? = null,
    val kind: String? = null,
    val results: List<Poem> = emptyList(),
    val searched: Boolean = false,
)

class LibraryViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state

    init {
        refresh()
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        refresh()
    }

    fun onDynastyChange(dynasty: String?) {
        _state.update { it.copy(dynasty = dynasty) }
        refresh()
    }

    fun onKindChange(kind: String?) {
        _state.update { it.copy(kind = kind) }
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val s = _state.value
            val results = if (s.query.isBlank()) {
                container.poemRepository.browse(s.dynasty, s.kind)
            } else {
                container.poemRepository.search(s.query, s.dynasty, s.kind)
            }
            _state.update { it.copy(results = results, searched = true) }
        }
    }
}
