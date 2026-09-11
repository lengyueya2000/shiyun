package com.shiyun.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyun.app.data.db.ShiyunDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoriteRepositoryTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var repo: FavoriteRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), ShiyunDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = FavoriteRepository(db.favoriteDao(), kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `toggle 收藏与取消`() = runTest {
        repo.toggle(1)
        assertTrue(repo.observeIsFavorite(1).first())
        repo.toggle(1)
        assertFalse(repo.observeIsFavorite(1).first())
    }

    @Test
    fun `列表按收藏时间倒序`() = runTest {
        repo.toggle(1)
        Thread.sleep(5)
        repo.toggle(2)
        assertEquals(listOf(2L, 1L), repo.observeAll().first().map { it.poemId })
    }
}
