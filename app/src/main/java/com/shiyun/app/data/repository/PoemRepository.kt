package com.shiyun.app.data.repository

import com.shiyun.app.data.assets.AssetsPoemImporter
import com.shiyun.app.data.db.MatchQueryBuilder
import com.shiyun.app.data.db.Poem
import com.shiyun.app.data.db.PoemDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PoemRepository(
    private val dao: PoemDao,
    private val importer: AssetsPoemImporter,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun importIfNeeded() = withContext(io) {
        if (dao.count() == 0) importer.importInto(dao)
    }

    fun observePoem(id: Long): Flow<Poem?> = dao.observeById(id)

    suspend fun search(raw: String, dynasty: String?, kind: String?): List<Poem> = withContext(io) {
        MatchQueryBuilder.build(raw)?.let { dao.searchFiltered(it, dynasty, kind) } ?: emptyList()
    }

    suspend fun browse(dynasty: String?, kind: String?): List<Poem> = withContext(io) {
        dao.browse(dynasty, kind)
    }

    suspend fun featured(): List<Poem> = withContext(io) { dao.featured() }

    suspend fun all(): List<Poem> = withContext(io) { dao.all() }
}
