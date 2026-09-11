package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyPoemSelectorTest {
    private fun poem(id: Long) = Poem(id, "t$id", "唐", "a", listOf("x"), "诗", null, null, null, emptyList(), 1, true, "")

    @Test
    fun `同一天选取稳定`() {
        val pool = (1L..50L).map(::poem)
        val date = LocalDate.of(2026, 9, 11)
        assertEquals(DailyPoemSelector.select(pool, date), DailyPoemSelector.select(pool, date))
    }

    @Test
    fun `选取结果在池内且跨日可变`() {
        val pool = (1L..50L).map(::poem)
        val picks = (0L..30L).map { DailyPoemSelector.select(pool, LocalDate.of(2026, 1, 1).plusDays(it))!!.id }
        assertTrue(picks.all { it in 1L..50L })
        assertTrue(picks.distinct().size > 1)
    }

    @Test
    fun `空池返回 null`() {
        assertNull(DailyPoemSelector.select(emptyList(), LocalDate.of(2026, 9, 11)))
    }
}
