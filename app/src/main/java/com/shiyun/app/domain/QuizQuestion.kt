package com.shiyun.app.domain

sealed interface QuizQuestion {
    data class FillBlank(val sentence: String, val blankIndex: Int, val answer: String, val options: List<String>) : QuizQuestion
    data class NextLine(val line: String, val answer: String, val options: List<String>) : QuizQuestion
    data class AuthorAttribution(val line: String, val answer: String, val options: List<String>) : QuizQuestion
}
