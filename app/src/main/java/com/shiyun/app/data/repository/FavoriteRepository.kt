package com.shiyun.app.data.repository

import com.shiyun.app.data.db.Favorite
import com.shiyun.app.data.db.FavoriteDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FavoriteRepository(
    private val dao: FavoriteDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeAll(): Flow<List<Favorite>> = dao.observeAll()

    fun observeIsFavorite(poemId: Long): Flow<Boolean> = dao.observeIsFavorite(poemId)

    suspend fun toggle(poemId: Long) = withContext(io) {
        if (dao.isFavorite(poemId)) dao.remove(poemId)
        else dao.add(Favorite(poemId, System.currentTimeMillis()))
    }
}
