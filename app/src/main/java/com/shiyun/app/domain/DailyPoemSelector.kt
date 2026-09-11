package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem
import java.time.LocalDate

object DailyPoemSelector {
    fun select(pool: List<Poem>, date: LocalDate): Poem? {
        if (pool.isEmpty()) return null
        val index = Math.floorMod(date.toEpochDay() * 7919L, pool.size).toInt()
        return pool[index]
    }
}
