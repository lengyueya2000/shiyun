package com.shiyun.app.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "poems")
data class Poem(
    @PrimaryKey val id: Long,
    val title: String,
    val dynasty: String,
    val author: String,
    val paragraphs: List<String>,
    val kind: String,
    val translation: String?,
    val notes: String?,
    val appreciation: String?,
    val tags: List<String>,
    val difficulty: Int,
    val featured: Boolean,
    val searchText: String,
)

@Fts4(contentEntity = Poem::class)
@Entity(tableName = "poems_fts")
data class PoemFts(
    val title: String,
    val author: String,
    val searchText: String,
)
