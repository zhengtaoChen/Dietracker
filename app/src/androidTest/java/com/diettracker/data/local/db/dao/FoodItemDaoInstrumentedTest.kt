package com.diettracker.data.local.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.FoodSource
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.RecordSource
import com.diettracker.domain.model.RecordStatus
import com.diettracker.testutil.InstrumentedTestDb
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 仪器化测试：验证真实 Android SQLite 上食物明细的写入、读取、快照与重启持久化。
 */
@RunWith(AndroidJUnit4::class)
class FoodItemDaoInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "food-item-test.db"

    private lateinit var db: com.diettracker.data.local.db.AppDatabase

    private val date = LocalDate.of(2026, 1, 1)

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        db = InstrumentedTestDb.fileDb(context, dbName)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun 明细写入后按餐次读取() = runBlocking {
        val mealId = "meal-1"
        db.mealRecordDao().upsert(meal(mealId))
        db.foodItemDao().insertAll(listOf(item(mealId, "米饭", 130f), item(mealId, "鸡胸肉", 120f)))

        val items = db.foodItemDao().getByMeal(mealId)
        assertEquals(2, items.size)
        assertEquals(listOf("米饭", "鸡胸肉"), items.map { it.name })
    }

    @Test
    fun 数据库关闭重开后明细仍然存在() = runBlocking {
        val mealId = "meal-1"
        db.mealRecordDao().upsert(meal(mealId))
        db.foodItemDao().insertAll(listOf(item(mealId, "米饭", 130f)))
        db.close()

        val reopened = InstrumentedTestDb.fileDb(context, dbName)
        try {
            val items = reopened.foodItemDao().getByMeal(mealId)
            assertEquals(1, items.size)
            assertEquals("米饭", items[0].name)
            assertEquals(130f, items[0].calories)
            assertNotNull(items[0].foodDbVersion)
        } finally {
            reopened.close()
        }
    }

    private fun meal(id: String): MealRecordEntity = MealRecordEntity(
        id = id,
        date = date,
        mealType = MealType.LUNCH,
        name = "午餐",
        time = null,
        note = null,
        photoUri = null,
        totalCaloriesOverride = null,
        proteinOverride = null,
        carbsOverride = null,
        fatOverride = null,
        status = RecordStatus.SAVED,
        source = RecordSource.MANUAL_DETAIL,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun item(
        mealRecordId: String,
        name: String,
        calories: Float?,
    ): FoodItemEntity = FoodItemEntity(
        id = UUID.randomUUID().toString(),
        mealRecordId = mealRecordId,
        sortOrder = 0,
        name = name,
        category = "主食",
        amount = 100f,
        amountUnit = "g",
        calories = calories,
        protein = null,
        carbs = null,
        fat = null,
        source = FoodSource.LOCAL_DB,
        foodDbId = "f1",
        foodDbVersion = 1,
    )
}
