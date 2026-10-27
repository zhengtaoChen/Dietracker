package com.diettracker.data.local.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.RecordSource
import com.diettracker.domain.model.RecordStatus
import com.diettracker.testutil.TestDb
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MealRecordDaoTest {

    private lateinit var db: com.diettracker.data.local.db.AppDatabase
    private lateinit var dao: MealRecordDao

    private val date = LocalDate.of(2026, 1, 1)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = TestDb.inMemory(context)
        dao = db.mealRecordDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `已保存查询排除草稿`() = runBlocking {
        dao.upsert(meal(status = RecordStatus.SAVED))
        dao.upsert(meal(status = RecordStatus.DRAFT))

        val saved = dao.observeSavedByDate(date).first()
        assertEquals(1, saved.size)
        assertEquals(RecordStatus.SAVED, saved[0].status)
    }

    @Test
    fun `按日期全量查询包含草稿`() = runBlocking {
        dao.upsert(meal(status = RecordStatus.SAVED))
        dao.upsert(meal(status = RecordStatus.DRAFT))

        val all = dao.observeAllByDate(date).first()
        assertEquals(2, all.size)
    }

    @Test
    fun `upsert 后可按 id 读取`() = runBlocking {
        val record = meal()
        dao.upsert(record)

        val loaded = dao.getById(record.id)
        assertNotNull(loaded)
        assertEquals(record.id, loaded?.id)
    }

    @Test
    fun `deleteById 删除记录`() = runBlocking {
        val record = meal()
        dao.upsert(record)
        dao.deleteById(record.id)

        assertNull(dao.getById(record.id))
    }

    @Test
    fun `findSavedByMealType 仅命中已保存的对应餐次`() = runBlocking {
        dao.upsert(meal(mealType = MealType.LUNCH, status = RecordStatus.SAVED))
        dao.upsert(meal(mealType = MealType.LUNCH, status = RecordStatus.DRAFT))
        dao.upsert(meal(mealType = MealType.BREAKFAST, status = RecordStatus.SAVED))

        val lunch = dao.findSavedByMealType(date, MealType.LUNCH)
        assertNotNull(lunch)
        assertEquals(MealType.LUNCH, lunch?.mealType)
    }

    @Test
    fun `observeDrafts 仅返回草稿且按更新时间倒序`() = runBlocking {
        dao.upsert(meal(status = RecordStatus.SAVED))
        dao.upsert(meal(id = "older", status = RecordStatus.DRAFT, updatedAt = 100L))
        dao.upsert(meal(id = "newer", status = RecordStatus.DRAFT, updatedAt = 200L))

        val drafts = dao.observeDrafts().first()
        assertEquals(listOf("newer", "older"), drafts.map { it.id })
    }

    @Test
    fun `草稿转正式复用同一 id 不产生重复`() = runBlocking {
        val id = "reuse-id"
        dao.upsert(meal(id = id, status = RecordStatus.DRAFT, updatedAt = 100L))

        // 模拟转正式：同 id 以 SAVED 状态 upsert。
        dao.upsert(meal(id = id, status = RecordStatus.SAVED, updatedAt = 200L))

        assertEquals(1, dao.observeAllByDate(date).first().size)
        assertEquals(RecordStatus.SAVED, dao.getById(id)?.status)
    }

    @Test
    fun `删除草稿不影响其他记录`() = runBlocking {
        dao.upsert(meal(id = "draft", status = RecordStatus.DRAFT))
        dao.upsert(meal(id = "saved", status = RecordStatus.SAVED))

        dao.deleteById("draft")

        assertNull(dao.getById("draft"))
        assertNotNull(dao.getById("saved"))
        assertEquals(1, dao.observeAllByDate(date).first().size)
    }

    private fun meal(
        id: String = UUID.randomUUID().toString(),
        mealType: MealType = MealType.LUNCH,
        status: RecordStatus = RecordStatus.SAVED,
        updatedAt: Long = 0L,
    ): MealRecordEntity = MealRecordEntity(
        id = id,
        date = date,
        mealType = mealType,
        name = "测试餐",
        time = null,
        note = null,
        photoUri = null,
        totalCaloriesOverride = null,
        proteinOverride = null,
        carbsOverride = null,
        fatOverride = null,
        status = status,
        source = RecordSource.MANUAL_QUICK,
        createdAt = 0L,
        updatedAt = updatedAt,
    )
}
