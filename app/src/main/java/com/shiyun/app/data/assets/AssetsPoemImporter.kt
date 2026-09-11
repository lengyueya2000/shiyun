package com.shiyun.app.data.assets

import android.content.Context
import com.shiyun.app.data.db.MatchQueryBuilder
import com.shiyun.app.data.db.Poem
import com.shiyun.app.data.db.PoemDao
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PoemJson(
    val id: Long,
    val title: String,
    val dynasty: String,
    val author: String,
    val paragraphs: List<String>,
    val kind: String,
    val translation: String? = null,
    val notes: String? = null,
    val appreciation: String? = null,
    val tags: List<String> = emptyList(),
    val difficulty: Int = 1,
    val featured: Boolean = false,
)

class AssetsPoemImporter(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importInto(dao: PoemDao) {
        val raw = context.assets.open("poems.json").bufferedReader().use { it.readText() }
        val poems = json.decodeFromString<List<PoemJson>>(raw).map { it.toEntity() }
        dao.insertAll(poems)
    }

    private fun PoemJson.toEntity() = Poem(
        id = id, title = title, dynasty = dynasty, author = author,
        paragraphs = paragraphs, kind = kind, translation = translation,
        notes = notes, appreciation = appreciation, tags = tags,
        difficulty = difficulty, featured = featured,
        searchText = MatchQueryBuilder.segment(title + author + paragraphs.joinToString("")),
    )
}
