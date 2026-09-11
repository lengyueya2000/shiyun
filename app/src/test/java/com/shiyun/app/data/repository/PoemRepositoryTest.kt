package com.shiyun.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyun.app.data.assets.AssetsPoemImporter
import com.shiyun.app.data.db.MatchQueryBuilder
import com.shiyun.app.data.db.Poem
import com.shiyun.app.data.db.PoemDao
import com.shiyun.app.data.db.ShiyunDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoemRepositoryTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var dao: PoemDao
    private lateinit var repo: PoemRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShiyunDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.poemDao()
        repo = PoemRepository(dao, AssetsPoemImporter(context), kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `search 委托 FTS 并空查询返回空`() = runTest {
        dao.insertAll(
            listOf(
                Poem(1, "静夜思", "唐", "李白", listOf("床前明月光"), "诗", null, null, null, emptyList(), 1, false,
                    MatchQueryBuilder.segment("静夜思李白床前明月光")),
            )
        )
        assertEquals(1, repo.search("月光", null, null).size)
        assertTrue(repo.search("  ", null, null).isEmpty())
    }

    @Test
    fun `browse 透传过滤参数`() = runTest {
        dao.insertAll(
            listOf(
                Poem(1, "春晓", "唐", "孟浩然", listOf("春眠不觉晓"), "诗", null, null, null, emptyList(), 1, false, "春晓孟浩然春眠不觉晓"),
                Poem(2, "水调歌头", "宋", "苏轼", listOf("明月几时有"), "词", null, null, null, emptyList(), 1, false, "水调歌头苏轼明月几时有"),
            )
        )
        assertEquals(listOf("春晓"), repo.browse("唐", "诗").map { it.title })
    }

    @Test
    fun `importIfNeeded 导入真实 assets 全量数据`() = runTest {
        repo.importIfNeeded()
        assertEquals(2003, dao.count())
        repo.importIfNeeded()
        assertEquals(2003, dao.count())
    }
}
