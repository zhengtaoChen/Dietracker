package com.diettracker.data.repo

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 仪器化测试：验证仓储层的草稿统计过滤与手动热量覆盖（PRD §13.1/13.2/13.3）。
 */
@RunWith(AndroidJUnit4::class)
class MealRepositoryInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "meal-repo-test.db"

    private lateinit var db: com.diettracker.data.local.db.AppDatabase
    private lateinit var repo: MealRepositoryImpl

    private val date = LocalDate.of(2026, 1, 1)

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        db = InstrumentedTestDb.fileDb(context, dbName)
        repo = MealRepositoryImpl(db, db.mealRecordDao(), db.foodItemDao(), PhotoRepositoryImpl(context))
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun 草稿不参与热量统计() = runBlocking {
        repo.saveMeal(meal(RecordStatus.SAVED, override = 500f), emptyList())
        repo.saveMeal(meal(RecordStatus.DRAFT, override = 9000f), emptyList())

        val summary = repo.observeNutritionSummary(date).first()
        assertEquals(500f, summary.calories)
    }

    @Test
    fun 手动覆盖热量优先且不与明细重复相加() = runBlocking {
        val record = meal(RecordStatus.SAVED, override = 400f)
        repo.saveMeal(
            record,
            listOf(item(record.id, calories = 300f, protein = 20f, carbs = 30f, fat = 10f)),
        )

        val summary = repo.observeNutritionSummary(date).first()
        assertEquals(400f, summary.calories)
    }

    @Test
    fun 缺失营养字段汇总为null而非0() = runBlocking {
        val record = meal(RecordStatus.SAVED, override = null)
        repo.saveMeal(
            record,
            listOf(item(record.id, calories = 100f, protein = null, carbs = null, fat = null)),
        )

        val summary = repo.observeNutritionSummary(date).first()
        assertEquals(100f, summary.calories)
        assertNull(summary.protein)
        assertNull(summary.carbs)
        assertNull(summary.fat)
    }

    private fun meal(status: RecordStatus, override: Float?): MealRecordEntity =
        MealRecordEntity(
            id = UUID.randomUUID().toString(),
            date = date,
            mealType = MealType.LUNCH,
            name = "午餐",
            time = null,
            note = null,
            photoUri = null,
            totalCaloriesOverride = override,
            proteinOverride = null,
            carbsOverride = null,
            fatOverride = null,
            status = status,
            source = RecordSource.MANUAL_QUICK,
            createdAt = 0L,
            updatedAt = 0L,
        )

    private fun item(
        mealRecordId: String,
        calories: Float?,
        protein: Float? = null,
        carbs: Float? = null,
        fat: Float? = null,
    ): FoodItemEntity = FoodItemEntity(
        id = UUID.randomUUID().toString(),
        mealRecordId = mealRecordId,
        sortOrder = 0,
        name = "食物",
        category = null,
        amount = 100f,
        amountUnit = "g",
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        source = FoodSource.LOCAL_DB,
        foodDbId = null,
        foodDbVersion = null,
    )
}
