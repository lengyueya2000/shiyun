package com.shiyun.app.data.repository

import com.shiyun.app.data.db.DailyRecord
import com.shiyun.app.data.db.DailyRecordDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

class DailyRepository(
    private val dao: DailyRecordDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeByDate(date: LocalDate): Flow<DailyRecord?> = dao.observeByDate(date.toString())

    fun observeStreak(): Flow<Int> = dao.observeAll().map { records ->
        val dates = records.filter { it.checkedIn }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
        var cursor = LocalDate.now()
        if (cursor !in dates) cursor = cursor.minusDays(1)   // 今天未打卡不中断连击
        var streak = 0
        while (cursor in dates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        streak
    }

    suspend fun checkIn(date: LocalDate, poemId: Long) = withContext(io) {
        dao.upsert(DailyRecord(date.toString(), poemId, checkedIn = true))
    }
}
