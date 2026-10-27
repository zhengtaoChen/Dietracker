package com.diettracker.data.local.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diettracker.data.local.db.entity.WeightRecordEntity
import com.diettracker.testutil.InstrumentedTestDb
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 仪器化测试：验证体重记录按日期唯一，同一天 upsert 覆盖、不同天新增（PRD §10.4）。
 */
@RunWith(AndroidJUnit4::class)
class WeightRecordDaoInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "weight-record-test.db"

    private lateinit var db: com.diettracker.data.local.db.AppDatabase
    private lateinit var dao: WeightRecordDao

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        db = InstrumentedTestDb.fileDb(context, dbName)
        dao = db.weightRecordDao()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun 同一天重复写入覆盖而非新增() = runBlocking {
        val date = LocalDate.of(2026, 1, 1)
        dao.upsert(weight(date, 70f))
        dao.upsert(weight(date, 69.5f))

        val range = dao.observeRange(date, date).first()
        assertEquals(1, range.size)
        assertEquals(69.5f, range[0].weightKg)
    }

    @Test
    fun 不同天分别保存互不影响() = runBlocking {
        dao.upsert(weight(LocalDate.of(2026, 1, 1), 70f))
        dao.upsert(weight(LocalDate.of(2026, 1, 2), 69.5f))

        val range = dao.observeRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)).first()
        assertEquals(2, range.size)
        assertEquals(70f, range[0].weightKg)
        assertEquals(69.5f, range[1].weightKg)
    }

    private fun weight(date: LocalDate, weightKg: Float): WeightRecordEntity =
        WeightRecordEntity(
            date = date,
            weightKg = weightKg,
            note = null,
            updatedAt = 0L,
        )
}
