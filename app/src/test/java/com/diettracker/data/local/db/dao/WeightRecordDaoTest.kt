package com.diettracker.data.local.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.diettracker.data.local.db.entity.WeightRecordEntity
import com.diettracker.testutil.TestDb
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeightRecordDaoTest {

    private lateinit var db: com.diettracker.data.local.db.AppDatabase
    private lateinit var dao: WeightRecordDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = TestDb.inMemory(context)
        dao = db.weightRecordDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `同一天 upsert 覆盖而非新增（每日一条）`() = runBlocking {
        dao.upsert(weight(LocalDate.of(2026, 1, 1), 70f))
        dao.upsert(weight(LocalDate.of(2026, 1, 1), 69.5f))

        val range = dao.observeRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)).first()
        assertEquals(1, range.size)
        assertEquals(69.5f, range[0].weightKg)
    }

    @Test
    fun `范围查询按日期排序且仅返回区间内`() = runBlocking {
        dao.upsert(weight(LocalDate.of(2026, 1, 2), 70f))
        dao.upsert(weight(LocalDate.of(2026, 1, 1), 71f))
        dao.upsert(weight(LocalDate.of(2026, 1, 5), 69f))

        val range = dao.observeRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3)).first()
        assertEquals(2, range.size)
        assertEquals(LocalDate.of(2026, 1, 1), range[0].date)
        assertEquals(LocalDate.of(2026, 1, 2), range[1].date)
    }

    private fun weight(date: LocalDate, weightKg: Float): WeightRecordEntity =
        WeightRecordEntity(
            date = date,
            weightKg = weightKg,
            note = null,
            updatedAt = 0L,
        )
}
