package com.shiyun.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyun.app.data.db.DailyRecordDao
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DailyRepositoryTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var dao: DailyRecordDao
    private lateinit var repo: DailyRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), ShiyunDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.dailyRecordDao()
        repo = DailyRepository(dao, kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `打卡幂等且可观察`() = runTest {
        val today = LocalDate.of(2026, 9, 11)
        repo.checkIn(today, poemId = 1)
        repo.checkIn(today, poemId = 1)
        val record = repo.observeByDate(today).first()!!
        assertEquals(1, record.poemId)
        assertTrue(record.checkedIn)
        assertFalse(dao.getByDate(today.toString()) == null)
    }

    @Test
    fun `连击计算包含今天与历史`() = runTest {
        val today = LocalDate.now()
        dao.upsert(com.shiyun.app.data.db.DailyRecord(today.toString(), 1, true))
        dao.upsert(com.shiyun.app.data.db.DailyRecord(today.minusDays(1).toString(), 1, true))
        dao.upsert(com.shiyun.app.data.db.DailyRecord(today.minusDays(3).toString(), 1, true))
        assertEquals(2, repo.observeStreak().first())
    }
}
