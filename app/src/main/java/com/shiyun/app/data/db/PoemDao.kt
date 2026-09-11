package com.shiyun.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PoemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(poems: List<Poem>)

    @Query("SELECT COUNT(*) FROM poems")
    suspend fun count(): Int

    @Query("SELECT * FROM poems WHERE id = :id")
    fun observeById(id: Long): Flow<Poem?>

    @Query("SELECT * FROM poems WHERE id = :id")
    suspend fun getById(id: Long): Poem?

    @Query(
        """SELECT p.* FROM poems p JOIN poems_fts f ON p.id = f.docid
        WHERE poems_fts MATCH :match
        AND (:dynasty IS NULL OR p.dynasty = :dynasty)
        AND (:kind IS NULL OR p.kind = :kind)
        LIMIT 200"""
    )
    suspend fun searchFiltered(match: String, dynasty: String?, kind: String?): List<Poem>

    @Query(
        """SELECT * FROM poems
        WHERE (:dynasty IS NULL OR dynasty = :dynasty)
        AND (:kind IS NULL OR kind = :kind)
        ORDER BY id LIMIT 200"""
    )
    suspend fun browse(dynasty: String?, kind: String?): List<Poem>

    @Query("SELECT * FROM poems WHERE featured = 1")
    suspend fun featured(): List<Poem>

    @Query("SELECT * FROM poems")
    suspend fun all(): List<Poem>
}
