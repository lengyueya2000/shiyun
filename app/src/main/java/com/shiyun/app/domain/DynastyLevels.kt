package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem

object DynastyLevels {
    val ORDER = listOf("先秦", "汉", "魏晋", "唐", "宋", "元", "明清")
    const val MIN_POEMS_PER_LEVEL = 5

    fun available(poems: List<Poem>): List<String> =
        ORDER.filter { dynasty -> poems.count { it.dynasty == dynasty } >= MIN_POEMS_PER_LEVEL }
}
