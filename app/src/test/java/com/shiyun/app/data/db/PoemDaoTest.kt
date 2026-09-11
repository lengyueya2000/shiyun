package com.shiyun.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoemDaoTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var dao: PoemDao

    private fun poem(id: Long, title: String, author: String, lines: List<String>, dynasty: String = "唐", kind: String = "诗", featured: Boolean = false) =
        Poem(
            id = id, title = title, dynasty = dynasty, author = author,
            paragraphs = lines, kind = kind, translation = null, notes = null,
            appreciation = null, tags = emptyList(), difficulty = 1, featured = featured,
            searchText = MatchQueryBuilder.segment(title + author + lines.joinToString("")),
        )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ShiyunDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.poemDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `插入后按单字检索命中诗句`() = runTest {
        dao.insertAll(listOf(poem(1, "静夜思", "李白", listOf("床前明月光", "疑是地上霜"))))
        assertEquals(1, dao.count())
        val hits = dao.searchFiltered(MatchQueryBuilder.build("月光")!!, null, null)
        assertEquals(1, hits.size)
        assertEquals("静夜思", hits[0].title)
    }

    @Test
    fun `未命中词返回空`() = runTest {
        dao.insertAll(listOf(poem(1, "静夜思", "李白", listOf("床前明月光"))))
        assertTrue(dao.searchFiltered(MatchQueryBuilder.build("黄河")!!, null, null).isEmpty())
    }

    @Test
    fun `朝代与类型过滤生效`() = runTest {
        dao.insertAll(
            listOf(
                poem(1, "水调歌头", "苏轼", listOf("明月几时有"), dynasty = "宋", kind = "词"),
                poem(2, "春晓", "孟浩然", listOf("春眠不觉晓"), dynasty = "唐", kind = "诗"),
            )
        )
        val hits = dao.searchFiltered(MatchQueryBuilder.build("明月")!!, "宋", "词")
        assertEquals(listOf("水调歌头"), hits.map { it.title })
        assertEquals(listOf("春晓"), dao.browse("唐", "诗").map { it.title })
    }

    @Test
    fun `按 id 观察与读取`() = runTest {
        dao.insertAll(listOf(poem(7, "春晓", "孟浩然", listOf("春眠不觉晓"))))
        assertEquals("春晓", dao.observeById(7).first()!!.title)
        assertEquals("春晓", dao.getById(7)!!.title)
    }
}
