package com.shiyun.app.data.repository

import com.shiyun.app.data.db.QuizState
import com.shiyun.app.data.db.QuizStateDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class QuizRepository(
    private val dao: QuizStateDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeStates(): Flow<List<QuizState>> = dao.observeAll()

    suspend fun saveResult(levelId: String, correct: Int) = withContext(io) {
        val stars = when (correct) {
            5 -> 3
            4 -> 2
            else -> 0
        }
        val existing = dao.observeAll().first().firstOrNull { it.levelId == levelId }
        val best = maxOf(existing?.stars ?: 0, stars)
        dao.upsert(QuizState(levelId, best, unlocked = true))
    }
}
