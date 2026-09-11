package com.shiyun.app.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiyun.app.ShiyunApp
import com.shiyun.app.domain.QuizQuestion
import com.shiyun.app.ui.theme.FadedInk

@Composable
fun QuizScreen() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShiyunApp
    val vm: QuizViewModel = viewModel { QuizViewModel(app.container) }
    val state by vm.state.collectAsStateWithLifecycle()

    val session = state.active
    if (session == null) {
        LevelList(state, onOpen = vm::openLevel, onRetry = vm::loadLevels)
    } else if (session.finished) {
        ResultView(session, onExit = vm::exitSession, onRetry = vm::retry)
    } else {
        QuestionView(session, onSelect = vm::selectOption, onNext = vm::next, onExit = vm::exitSession)
    }
}

@Composable
private fun LevelList(state: QuizUiState, onOpen: (String) -> Unit, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("背诗闯关", style = MaterialTheme.typography.headlineMedium)
        if (state.levels.isEmpty()) {
            if (state.loadFailed) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("数据加载失败", color = FadedInk)
                    TextButton(onClick = onRetry) { Text("重试") }
                }
            } else {
                Text("数据加载中…", color = FadedInk)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.levels, key = { it.dynasty }) { level ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(level.dynasty, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (level.unlocked) "★".repeat(level.stars).ifEmpty { "未挑战" } else "需先通过上一关",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FadedInk,
                            )
                        }
                        Button(onClick = { onOpen(level.dynasty) }, enabled = level.unlocked) { Text("开始") }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionView(session: QuizSession, onSelect: (Int) -> Unit, onNext: () -> Unit, onExit: () -> Unit) {
    val question = session.questions[session.index]
    val options = when (question) {
        is QuizQuestion.FillBlank -> question.options
        is QuizQuestion.NextLine -> question.options
        is QuizQuestion.AuthorAttribution -> question.options
    }
    val answer = when (question) {
        is QuizQuestion.FillBlank -> question.answer
        is QuizQuestion.NextLine -> question.answer
        is QuizQuestion.AuthorAttribution -> question.answer
    }
    val prompt = when (question) {
        is QuizQuestion.FillBlank -> {
            val s = question.sentence
            val shown = s.substring(0, question.blankIndex) + "____" + s.substring(question.blankIndex + 1)
            "补全诗句:\n$shown"
        }
        is QuizQuestion.NextLine -> "选择下句:\n${question.line}"
        is QuizQuestion.AuthorAttribution -> "此句出自哪位作者:\n${question.line}"
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onExit) { Text("退出") }
        Text("第 ${session.index + 1} / ${session.questions.size} 题 · 答对 ${session.correct}",
            style = MaterialTheme.typography.bodyMedium, color = FadedInk)
        Text(prompt, style = MaterialTheme.typography.titleLarge)

        options.forEachIndexed { index, option ->
            val isSelected = session.selected == index
            val isAnswer = option == answer
            OutlinedButton(
                onClick = { onSelect(index) },
                enabled = session.selected == -1,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    option,
                    color = when {
                        session.selected == -1 -> MaterialTheme.colorScheme.onSurface
                        isAnswer -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.error
                        else -> FadedInk
                    },
                )
            }
        }

        if (session.selected != -1) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("下一题") }
        }
    }
}

@Composable
private fun ResultView(session: QuizSession, onExit: () -> Unit, onRetry: () -> Unit) {
    val stars = when (session.correct) {
        5 -> 3
        4 -> 2
        else -> 0
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterVertically),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(if (session.correct >= 4) "过关!" else "再接再厉", style = MaterialTheme.typography.headlineMedium)
        Text("★".repeat(stars).ifEmpty { "☆☆☆" }, style = MaterialTheme.typography.displayLarge)
        Text("答对 ${session.correct} / ${session.questions.size} 题", color = FadedInk)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) { Text("再来一次") }
            OutlinedButton(onClick = onExit) { Text("返回关卡") }
        }
    }
}
