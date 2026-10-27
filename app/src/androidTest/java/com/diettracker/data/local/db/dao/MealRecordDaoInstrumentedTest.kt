package com.diettracker.data.local.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.RecordSource
import com.diettracker.domain.model.RecordStatus
import com.diettracker.testutil.InstrumentedTestDb
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 仪器化测试：验证在真实 Android SQLite 上餐次记录的数据写入、读取、草稿过滤与持久化。
 *
 * 单元测试（Robolectric）已覆盖相同逻辑，本类用于确认真实设备上的 SQLite 行为与 JVM SQLite 一致。
 */
@RunWith(AndroidJUnit4::class)
class MealRecordDaoInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "meal-record-test.db"

    private lateinit var db: com.diettracker.data.local.db.AppDatabase
    private lateinit var dao: MealRecordDao

    private val date = LocalDate.of(2026, 1, 1)

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        db = InstrumentedTestDb.fileDb(context, dbName)
        dao = db.mealRecordDao()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun 写入后可按id读取() = runBlocking {
        val record = meal(id = "meal-1")
        dao.upsert(record)

        val loaded = dao.getById("meal-1")
        assertNotNull(loaded)
        assertEquals("meal-1", loaded?.id)
        assertEquals("午餐", loaded?.name)
        assertEquals(850f, loaded?.totalCaloriesOverride)
    }

    @Test
    fun 删除后读取为空() = runBlocking {
        dao.upsert(meal(id = "meal-1"))
        dao.deleteById("meal-1")

        assertNull(dao.getById("meal-1"))
    }

    @Test
    fun 已保存查询排除草稿() = runBlocking {
        dao.upsert(meal(id = "saved", status = RecordStatus.SAVED))
        dao.upsert(meal(id = "draft", status = RecordStatus.DRAFT))

        val saved = dao.observeSavedByDate(date).first()
        assertEquals(1, saved.size)
        assertEquals("saved", saved[0].id)
    }

    @Test
    fun 数据库关闭重开后数据仍然存在() = runBlocking {
        dao.upsert(meal(id = "persist-1", status = RecordStatus.SAVED))
        dao.upsert(meal(id = "persist-draft", status = RecordStatus.DRAFT))
        db.close()

        // 重新打开同一数据库文件，验证数据持久化（而非仅存在于内存）。
        val reopened = InstrumentedTestDb.fileDb(context, dbName)
        try {
            val saved = reopened.mealRecordDao().observeSavedByDate(date).first()
            assertEquals(1, saved.size)
            assertEquals("persist-1", saved[0].id)

            val all = reopened.mealRecordDao().observeAllByDate(date).first()
            assertEquals(2, all.size)
        } finally {
            reopened.close()
        }
    }

    @Test
    fun observeDrafts仅返回草稿且按更新时间倒序() = runBlocking {
        dao.upsert(meal(id = "saved", status = RecordStatus.SAVED))
        dao.upsert(meal(id = "older", status = RecordStatus.DRAFT, updatedAt = 100L))
        dao.upsert(meal(id = "newer", status = RecordStatus.DRAFT, updatedAt = 200L))

        val drafts = dao.observeDrafts().first()
        assertEquals(listOf("newer", "older"), drafts.map { it.id })
    }

    @Test
    fun 草稿转正式复用同一id不产生重复() = runBlocking {
        val id = "reuse-id"
        dao.upsert(meal(id = id, status = RecordStatus.DRAFT, updatedAt = 100L))

        dao.upsert(meal(id = id, status = RecordStatus.SAVED, updatedAt = 200L))

        assertEquals(1, dao.observeAllByDate(date).first().size)
        assertEquals(RecordStatus.SAVED, dao.getById(id)?.status)
    }

    @Test
    fun 删除草稿不影响其他记录() = runBlocking {
        dao.upsert(meal(id = "draft", status = RecordStatus.DRAFT))
        dao.upsert(meal(id = "saved", status = RecordStatus.SAVED))

        dao.deleteById("draft")

        assertNull(dao.getById("draft"))
        assertNotNull(dao.getById("saved"))
        assertEquals(1, dao.observeAllByDate(date).first().size)
    }

    private fun meal(
        id: String,
        status: RecordStatus = RecordStatus.SAVED,
        updatedAt: Long = 0L,
    ): MealRecordEntity = MealRecordEntity(
        id = id,
        date = date,
        mealType = MealType.LUNCH,
        name = "午餐",
        time = null,
        note = null,
        photoUri = null,
        totalCaloriesOverride = 850f,
        proteinOverride = null,
        carbsOverride = null,
        fatOverride = null,
        status = status,
        source = RecordSource.MANUAL_QUICK,
        createdAt = 0L,
        updatedAt = updatedAt,
    )
}
