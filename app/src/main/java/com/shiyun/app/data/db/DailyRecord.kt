package com.shiyun.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class DailyRecord(@PrimaryKey val date: String, val poemId: Long, val checkedIn: Boolean)
