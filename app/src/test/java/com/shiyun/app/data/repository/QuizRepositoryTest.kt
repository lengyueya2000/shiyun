package com.shiyun.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyun.app.data.db.ShiyunDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuizRepositoryTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var repo: QuizRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), ShiyunDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = QuizRepository(db.quizStateDao(), kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `星级按答对数映射且保留最高记录`() = runTest {
        repo.saveResult("唐", correct = 5)
        repo.saveResult("唐", correct = 4)
        assertEquals(3, repo.observeStates().first().first { it.levelId == "唐" }.stars)
    }

    @Test
    fun `未过关记录零星`() = runTest {
        repo.saveResult("唐", correct = 2)
        assertEquals(0, repo.observeStates().first().first().stars)
    }
}
