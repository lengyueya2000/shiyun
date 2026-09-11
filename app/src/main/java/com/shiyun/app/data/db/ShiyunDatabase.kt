package com.shiyun.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Poem::class, PoemFts::class, Favorite::class, DailyRecord::class, QuizState::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ShiyunDatabase : RoomDatabase() {
    abstract fun poemDao(): PoemDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun quizStateDao(): QuizStateDao

    companion object {
        fun build(context: Context): ShiyunDatabase =
            Room.databaseBuilder(context, ShiyunDatabase::class.java, "shiyun.db").build()
    }
}
