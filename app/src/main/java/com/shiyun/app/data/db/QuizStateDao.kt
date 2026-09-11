package com.shiyun.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizStateDao {
    @Query("SELECT * FROM quiz_states")
    fun observeAll(): Flow<List<QuizState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: QuizState)
}
