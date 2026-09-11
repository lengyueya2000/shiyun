package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizGeneratorTest {
    private fun poem(id: Long, author: String, lines: List<String>, dynasty: String = "唐") = Poem(
        id, "诗$id", dynasty, author, lines, "诗", null, null, null, emptyList(), 1, false,
        lines.joinToString(""),
    )

    private val pool = listOf(
        poem(1, "李白", listOf("床前明月光", "疑是地上霜", "举头望明月", "低头思故乡")),
        poem(2, "孟浩然", listOf("春眠不觉晓", "处处闻啼鸟", "夜来风雨声", "花落知多少")),
        poem(3, "王维", listOf("空山新雨后", "天气晚来秋", "明月松间照", "清泉石上流")),
        poem(4, "杜甫", listOf("好雨知时节", "当春乃发生", "随风潜入夜", "润物细无声")),
        poem(5, "白居易", listOf("离离原上草", "一岁一枯荣", "野火烧不尽", "春风吹又生")),
    )
    private val generator = QuizGenerator(java.util.Random(7))

    @Test
    fun `生成指定数量题目且选项合规`() {
        val questions = generator.generate(pool, pool)
        assertEquals(5, questions.size)
        for (q in questions) {
            val options = when (q) {
                is QuizQuestion.FillBlank -> q.options
                is QuizQuestion.NextLine -> q.options
                is QuizQuestion.AuthorAttribution -> q.options
            }
            val answer = when (q) {
                is QuizQuestion.FillBlank -> q.answer
                is QuizQuestion.NextLine -> q.answer
                is QuizQuestion.AuthorAttribution -> q.answer
            }
            assertEquals(4, options.size)
            assertEquals(options.size, options.distinct().size)
            assertTrue(answer in options)
        }
    }

    @Test
    fun `素材充足时三种题型都出现`() {
        val types = (1..20).flatMap { generator.generate(pool, pool) }.map {
            when (it) {
                is QuizQuestion.FillBlank -> "F"
                is QuizQuestion.NextLine -> "N"
                is QuizQuestion.AuthorAttribution -> "A"
            }
        }.toSet()
        assertEquals(setOf("F", "N", "A"), types)
    }

    @Test
    fun `素材不足降级为作者题`() {
        val questions = generator.generate(listOf(pool[0]), pool)
        assertTrue(questions.all { it is QuizQuestion.AuthorAttribution })
    }

    @Test
    fun `DynastyLevels 只含不少于五首的朝代`() {
        val poems = pool + (6L..10L).map { poem(it, "杜牧", listOf("清明时节雨纷纷"), dynasty = "宋") }
        assertEquals(listOf("唐", "宋"), DynastyLevels.available(poems))
    }
}
