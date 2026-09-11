package com.shiyun.app.data.db

object MatchQueryBuilder {
    // FTS 索引按单字分词(searchText 以空格分隔每字),查询构造为短语匹配
    fun build(raw: String): String? {
        val phrases = raw.trim().split(Regex("\\s+")).mapNotNull { chunk ->
            val chars = chunk.filter { it.isLetterOrDigit() }
            if (chars.isEmpty()) null else chars.toList().joinToString(" ", "\"", "\"")
        }
        return if (phrases.isEmpty()) null else phrases.joinToString(" ")
    }

    fun segment(text: String): String =
        text.filter { it.isLetterOrDigit() }.toList().joinToString(" ")
}
