package com.shiyun.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_states")
data class QuizState(@PrimaryKey val levelId: String, val stars: Int, val unlocked: Boolean)
