package com.shiyun.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.domain.DynastyLevels
import com.shiyun.app.domain.QuizGenerator
import com.shiyun.app.domain.QuizQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LevelUi(val dynasty: String, val unlocked: Boolean, val stars: Int)

data class QuizSession(
    val dynasty: String,
    val questions: List<QuizQuestion>,
    val index: Int = 0,
    val correct: Int = 0,
    val selected: Int = -1,
    val finished: Boolean = false,
)

data class QuizUiState(val levels: List<LevelUi> = emptyList(), val active: QuizSession? = null)

class QuizViewModel(private val container: AppContainer) : ViewModel() {
    private val generator = QuizGenerator()
    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state

    init {
        viewModelScope.launch {
            container.poemRepository.importIfNeeded()
            container.poemRepository.all().let { poems -> rebuild(poems, emptyList()) }
            container.quizRepository.observeStates().collect { states ->
                _state.update { it.copy(levels = buildLevels(states)) }
            }
        }
    }

    private var allPoems: List<com.shiyun.app.data.db.Poem> = emptyList()

    private fun rebuild(poems: List<com.shiyun.app.data.db.Poem>, states: List<com.shiyun.app.data.db.QuizState>) {
        allPoems = poems
        _state.update { it.copy(levels = buildLevels(states)) }
    }

    private fun buildLevels(states: List<com.shiyun.app.data.db.QuizState>): List<LevelUi> {
        val dynasties = DynastyLevels.available(allPoems)
        val stateByLevel = states.associateBy { it.levelId }
        var previousPassed = true
        return dynasties.map { dynasty ->
            val quizState = stateByLevel[dynasty]
            val unlocked = previousPassed
            val stars = if (unlocked) quizState?.stars ?: 0 else 0
            previousPassed = (quizState?.stars ?: 0) >= 2
            LevelUi(dynasty, unlocked, stars)
        }
    }

    fun openLevel(dynasty: String) {
        viewModelScope.launch {
            val levelPoems = container.poemRepository.browse(dynasty, null)
            if (levelPoems.isEmpty()) return@launch
            val questions = generator.generate(levelPoems, allPoems)
            _state.update { it.copy(active = QuizSession(dynasty, questions)) }
        }
    }

    fun selectOption(index: Int) {
        val session = _state.value.active ?: return
        if (session.selected != -1) return
        val question = session.questions[session.index]
        val answer = when (question) {
            is QuizQuestion.FillBlank -> question.answer
            is QuizQuestion.NextLine -> question.answer
            is QuizQuestion.AuthorAttribution -> question.answer
        }
        val options = when (question) {
            is QuizQuestion.FillBlank -> question.options
            is QuizQuestion.NextLine -> question.options
            is QuizQuestion.AuthorAttribution -> question.options
        }
        val isCorrect = options.getOrNull(index) == answer
        _state.update {
            it.copy(
                active = session.copy(
                    selected = index,
                    correct = if (isCorrect) session.correct + 1 else session.correct,
                )
            )
        }
    }

    fun next() {
        val session = _state.value.active ?: return
        if (session.index + 1 >= session.questions.size) {
            viewModelScope.launch { container.quizRepository.saveResult(session.dynasty, session.correct) }
            _state.update { it.copy(active = session.copy(finished = true)) }
        } else {
            _state.update { it.copy(active = session.copy(index = session.index + 1, selected = -1)) }
        }
    }

    fun exitSession() {
        _state.update { it.copy(active = null) }
    }

    fun retry() {
        val dynasty = _state.value.active?.dynasty ?: return
        exitSession()
        openLevel(dynasty)
    }
}
