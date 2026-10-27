package com.diettracker.data.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.FoodSource
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.RawCooked
import com.diettracker.domain.model.RecordSource
import com.diettracker.domain.model.RecordStatus
import com.diettracker.testutil.TestDb
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private lateinit var photoRepo: PhotoRepositoryImpl
    private lateinit var context: Context

    private val date = LocalDate.of(2026, 1, 1)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = TestDb.inMemory(context)
        photoRepo = PhotoRepositoryImpl(context)
        repo = MealRepositoryImpl(db, db.mealRecordDao(), db.foodItemDao(), photoRepo)
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

    @Test
    fun `营养快照不随本地库更新改变`() = runBlocking {
        // 食物 v1：每 100g 130 kcal
        db.foodDbDao().insertAll(listOf(food(id = "f1", kcal = 130f, version = 1)))

        val meal = meal(status = RecordStatus.SAVED, override = null)
        repo.saveMeal(meal, listOf(item(meal.id, calories = 130f, foodDbId = "f1", foodDbVersion = 1)))

        // 本地库更新为 v2：200 kcal
        db.foodDbDao().upsert(food(id = "f1", kcal = 200f, version = 2))

        val withItems = repo.getMealWithItems(meal.id)
        val savedItem = withItems?.items?.firstOrNull()
        assertEquals(130f, savedItem?.calories)
        assertEquals(1, savedItem?.foodDbVersion)
    }

    @Test
    fun `删除餐次连带删除明细且不影响其他餐次`() = runBlocking {
        val mealA = meal(status = RecordStatus.SAVED, override = null)
        val mealB = meal(status = RecordStatus.SAVED, override = null)
        repo.saveMeal(mealA, listOf(item(mealA.id, calories = 100f), item(mealA.id, calories = 200f)))
        repo.saveMeal(mealB, listOf(item(mealB.id, calories = 300f)))

        repo.deleteMeal(mealA.id)

        assertNull(repo.getMealWithItems(mealA.id))
        assertTrue(db.foodItemDao().getByMeal(mealA.id).isEmpty())
        assertEquals(1, db.foodItemDao().getByMeal(mealB.id).size)
        assertEquals(300f, repo.getMealWithItems(mealB.id)?.items?.firstOrNull()?.calories)
    }

    @Test
    fun `删除餐次连带删除照片文件`() = runBlocking {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "test.jpg").apply { writeText("x") }

        val meal = meal(status = RecordStatus.SAVED, override = 500f).copy(photoUri = "photos/test.jpg")
        repo.saveMeal(meal, emptyList())

        repo.deleteMeal(meal.id)

        assertFalse(file.exists())
    }

    @Test
    fun `清理无主照片且不删被引用的照片`() = runBlocking {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val referenced = File(dir, "keep.jpg").apply { writeText("k") }
        val orphan = File(dir, "drop.jpg").apply { writeText("o") }

        val meal = meal(status = RecordStatus.SAVED, override = 500f).copy(photoUri = "photos/keep.jpg")
        repo.saveMeal(meal, emptyList())

        val removed = repo.deleteOrphanedPhotos()

        assertEquals(1, removed)
        assertTrue(referenced.exists())
        assertFalse(orphan.exists())
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
        foodDbId: String? = null,
        foodDbVersion: Int? = null,
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
        foodDbId = foodDbId,
        foodDbVersion = foodDbVersion,
    )

    private fun food(id: String, kcal: Float, version: Int): FoodDbEntity = FoodDbEntity(
        id = id,
        name = "米饭",
        category = "主食",
        aliases = "",
        kcalPer100g = kcal,
        proteinPer100g = 2.7f,
        carbsPer100g = 28.2f,
        fatPer100g = 0.3f,
        rawCooked = RawCooked.COOKED,
        source = "test",
        sourceId = null,
        version = version,
        isCustom = false,
        isFavorite = false,
        createdAt = 0L,
    )
}
