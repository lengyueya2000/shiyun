package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem
import kotlin.random.Random

class QuizGenerator(private val random: Random = Random(42)) {

    /** 兼容 java.util.Random（测试以 `java.util.Random(7)` 构造）。 */
    constructor(random: java.util.Random) : this(Random(random.nextLong()))

    /**
     * 生成至多 [count] 道题。当某题在所有题型(含兜底作者题)下素材均不足时,
     * 该题会被跳过,因此返回列表长度可能小于 [count]。
     */
    fun generate(levelPoems: List<Poem>, distractorPool: List<Poem>, count: Int = 5): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        val types = listOf(Type.FILL, Type.NEXT, Type.AUTHOR).shuffled(random)
        for (i in 0 until count) {
            val question = buildInOrder(levelPoems, distractorPool, listOf(types[i % types.size], Type.AUTHOR))
            question?.let { questions += it }
        }
        return questions
    }

    private fun buildInOrder(levelPoems: List<Poem>, pool: List<Poem>, types: List<Type>): QuizQuestion? {
        for (type in types) {
            when (type) {
                Type.FILL -> fillBlank(levelPoems, pool)?.let { return it }
                Type.NEXT -> nextLine(levelPoems, pool)?.let { return it }
                Type.AUTHOR -> authorQuestion(levelPoems, pool)?.let { return it }
            }
        }
        // 所有题型素材均不足:跳过该题(返回 null,由 generate 过滤)
        return null
    }

    private fun options(answer: String, distractors: List<String>): List<String> =
        (listOf(answer) + distractors.filter { it != answer }.distinct().take(3))
            .shuffled(random)

    private fun fillBlank(poems: List<Poem>, pool: List<Poem>): QuizQuestion? {
        if (poems.size < 2) return null // 素材不足(少于两首),降级
        val chars = pool.asSequence().flatMap { it.paragraphs.asSequence() }
            .flatMap { it.asSequence() }.filter { it.isLetterOrDigit() }.toSet().toList()
        if (chars.size < 4) return null
        val candidates = poems.asSequence()
            .flatMap { it.paragraphs.asSequence() }
            .map { it.filter { c -> c.isLetterOrDigit() } }
            .filter { it.length >= 4 }
            .toList()
        if (candidates.isEmpty()) return null
        val sentence = candidates[random.nextInt(candidates.size)]
        val blankIndex = random.nextInt(sentence.length)
        val answer = sentence[blankIndex].toString()
        val distractors = buildList {
            while (size < 3) {
                val c = chars[random.nextInt(chars.size)].toString()
                if (c != answer && c !in this) add(c)
            }
        }
        return QuizQuestion.FillBlank(sentence, blankIndex, answer, options(answer, distractors))
    }

    private fun nextLine(poems: List<Poem>, pool: List<Poem>): QuizQuestion? {
        if (poems.size < 2) return null // 素材不足(少于两首),降级
        val pairs = poems.asSequence().flatMap { poem ->
            poem.paragraphs.zipWithNext().map { (a, b) -> a to b }
        }.toList()
        if (pairs.isEmpty()) return null
        val (line, answer) = pairs[random.nextInt(pairs.size)]
        val otherLines = pool.asSequence().flatMap { it.paragraphs.asSequence() }
            .filter { it != answer && it != line }.distinct().toList()
        if (otherLines.size < 3) return null
        val distractors = otherLines.shuffled(random).take(3)
        return QuizQuestion.NextLine(line, answer, options(answer, distractors))
    }

    private fun authorQuestion(poems: List<Poem>, pool: List<Poem>): QuizQuestion? {
        if (poems.isEmpty()) return null
        val poem = poems[random.nextInt(poems.size)]
        val answer = poem.author
        val others = pool.map { it.author }.filter { it != answer }.distinct()
        if (others.size < 3) return null
        val distractors = others.shuffled(random).take(3)
        return QuizQuestion.AuthorAttribution(poem.paragraphs.firstOrNull() ?: poem.title, answer, options(answer, distractors))
    }

    private enum class Type { FILL, NEXT, AUTHOR }
}
