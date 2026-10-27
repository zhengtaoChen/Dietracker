package com.diettracker.data.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.FoodSource
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MealRepositoryImplTest {

    private lateinit var db: com.diettracker.data.local.db.AppDatabase
    private lateinit var repo: MealRepositoryImpl

    private val date = LocalDate.of(2026, 1, 1)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = TestDb.inMemory(context)
        repo = MealRepositoryImpl(db, db.mealRecordDao(), db.foodItemDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `营养汇总排除草稿`() = runBlocking {
        repo.saveMeal(meal(status = RecordStatus.SAVED, override = 500f), emptyList())
        repo.saveMeal(meal(status = RecordStatus.DRAFT, override = 9000f), emptyList())

        val summary = repo.observeNutritionSummary(date).first()
        assertEquals(500f, summary.calories)
    }

    @Test
    fun `营养汇总覆盖值优先且不与明细重复相加`() = runBlocking {
        val meal = meal(status = RecordStatus.SAVED, override = 400f)
        val items = listOf(item(meal.id, calories = 300f, protein = 20f, carbs = 30f, fat = 10f))
        repo.saveMeal(meal, items)

        val summary = repo.observeNutritionSummary(date).first()
        assertEquals(400f, summary.calories)
        assertTrue(summary.isCaloriesComplete)
    }

    @Test
    fun `缺失营养字段汇总为 null 而非 0`() = runBlocking {
        val meal = meal(status = RecordStatus.SAVED, override = null)
        val items = listOf(item(meal.id, calories = 100f, protein = null, carbs = null, fat = null))
        repo.saveMeal(meal, items)

        val summary = repo.observeNutritionSummary(date).first()
        assertEquals(100f, summary.calories)
        assertNull(summary.protein)
        assertNull(summary.carbs)
        assertNull(summary.fat)
    }

    @Test
    fun `保存餐次替换旧明细`() = runBlocking {
        val meal = meal(status = RecordStatus.SAVED, override = null)
        repo.saveMeal(meal, listOf(item(meal.id, calories = 100f), item(meal.id, calories = 200f)))
        repo.saveMeal(meal, listOf(item(meal.id, calories = 50f)))

        val withItems = repo.getMealWithItems(meal.id)
        assertEquals(1, withItems?.items?.size)
        assertEquals(50f, withItems?.items?.firstOrNull()?.calories)
    }

    private fun meal(status: RecordStatus, override: Float?): MealRecordEntity =
        MealRecordEntity(
            id = UUID.randomUUID().toString(),
            date = date,
            mealType = MealType.LUNCH,
            name = "测试餐",
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
