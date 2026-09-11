package com.shiyun.app.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchQueryBuilderTest {
    @Test
    fun `单字查询构造为短语`() {
        assertEquals("\"月\"", MatchQueryBuilder.build("月"))
    }

    @Test
    fun `连续汉字构造为空格分隔短语`() {
        assertEquals("\"明 月 几 时\"", MatchQueryBuilder.build("明月几时"))
    }

    @Test
    fun `多词查询按 AND 短语拼接`() {
        assertEquals("\"明 月\" \"苏 轼\"", MatchQueryBuilder.build("明月 苏轼"))
    }

    @Test
    fun `过滤标点与引号`() {
        assertEquals("\"春 眠\"", MatchQueryBuilder.build("春\"眠!"))
    }

    @Test
    fun `空白输入返回 null`() {
        assertNull(MatchQueryBuilder.build("  "))
        assertNull(MatchQueryBuilder.build(""))
    }
}
