package com.diettracker.domain.model

import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.data.local.db.entity.MealRecordWithItems
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    // ---- mealCalories：覆盖值优先、缺失不视为 0 ----

    @Test
    fun `手动覆盖总热量优先于明细合计`() {
        val meal = meal(totalCaloriesOverride = 500f)
        val items = listOf(item(calories = 100f), item(calories = 200f))
        assertEquals(500f, NutritionCalculator.mealCalories(meal, items))
    }

    @Test
    fun `无覆盖时取明细已知热量合计且忽略缺失项`() {
        val meal = meal(totalCaloriesOverride = null)
        val items = listOf(item(calories = 100f), item(calories = null), item(calories = 150f))
        assertEquals(250f, NutritionCalculator.mealCalories(meal, items))
    }

    @Test
    fun `无覆盖且明细全部缺失热量时返回 null 而非 0`() {
        val meal = meal(totalCaloriesOverride = null)
        val items = listOf(item(calories = null))
        assertNull(NutritionCalculator.mealCalories(meal, items))
    }

    @Test
    fun `无覆盖且无明细时返回 null`() {
        assertNull(NutritionCalculator.mealCalories(meal(totalCaloriesOverride = null), emptyList()))
    }

    // ---- isMealCaloriesComplete ----

    @Test
    fun `有覆盖值时热量视为完整`() {
        assertTrue(NutritionCalculator.isMealCaloriesComplete(meal(totalCaloriesOverride = 500f), emptyList()))
    }

    @Test
    fun `明细全部热量已知时完整`() {
        val items = listOf(item(calories = 100f), item(calories = 200f))
        assertTrue(NutritionCalculator.isMealCaloriesComplete(meal(), items))
    }

    @Test
    fun `明细存在缺失热量时不完整`() {
        val items = listOf(item(calories = 100f), item(calories = null))
        assertFalse(NutritionCalculator.isMealCaloriesComplete(meal(), items))
    }

    @Test
    fun `无覆盖且无明细时不完整`() {
        assertFalse(NutritionCalculator.isMealCaloriesComplete(meal(), emptyList()))
    }

    // ---- dailySummary ----

    @Test
    fun `无任何餐次时全为 null 且不完整`() {
        val summary = NutritionCalculator.dailySummary(emptyList())
        assertNull(summary.calories)
        assertNull(summary.protein)
        assertFalse(summary.isCaloriesComplete)
        assertFalse(summary.isMacrosComplete)
    }

    @Test
    fun `覆盖值与明细互斥取一不重复相加`() {
        val meal = meal(totalCaloriesOverride = 400f)
        val items = listOf(item(calories = 300f, protein = 20f, carbs = 30f, fat = 10f))
        val summary = NutritionCalculator.dailySummary(listOf(MealRecordWithItems(meal, items)))
        // 热量取覆盖值 400，而非 400 + 300
        assertEquals(400f, summary.calories)
        // 三大营养素仍来自明细
        assertEquals(20f, summary.protein)
        assertTrue(summary.isCaloriesComplete)
    }

    @Test
    fun `缺失蛋白质不视为 0 且汇总标记不完整`() {
        val meal = meal(totalCaloriesOverride = null)
        val items = listOf(
            item(calories = 100f, protein = 10f, carbs = 20f, fat = 5f),
            item(calories = 50f, protein = null, carbs = 10f, fat = 2f),
        )
        val summary = NutritionCalculator.dailySummary(listOf(MealRecordWithItems(meal, items)))
        assertEquals(150f, summary.calories)
        // 缺失项不计入，仅统计已知值
        assertEquals(10f, summary.protein)
        assertEquals(30f, summary.carbs)
        assertEquals(7f, summary.fat)
        assertTrue(summary.isCaloriesComplete)
        assertFalse(summary.isMacrosComplete)
    }

    // ---- 餐次级手动营养覆盖（快速模式，PRD §4.2）----

    @Test
    fun `餐次级蛋白质覆盖优先于明细合计`() {
        val meal = meal(proteinOverride = 50f)
        val items = listOf(item(calories = 100f, protein = 20f, carbs = 30f, fat = 10f))
        val summary = NutritionCalculator.dailySummary(listOf(MealRecordWithItems(meal, items)))
        assertEquals(50f, summary.protein)
    }

    @Test
    fun `快速模式无明细时餐次级营养正确汇总`() {
        val meal = meal(
            totalCaloriesOverride = 850f,
            proteinOverride = 40f,
            carbsOverride = 100f,
            fatOverride = 30f,
        )
        val summary = NutritionCalculator.dailySummary(listOf(MealRecordWithItems(meal, emptyList())))
        assertEquals(850f, summary.calories)
        assertEquals(40f, summary.protein)
        assertEquals(100f, summary.carbs)
        assertEquals(30f, summary.fat)
        assertTrue(summary.isMacrosComplete)
    }

    @Test
    fun `快速模式缺失餐次级营养不视为0`() {
        val meal = meal(totalCaloriesOverride = 850f, proteinOverride = 40f)
        val summary = NutritionCalculator.dailySummary(listOf(MealRecordWithItems(meal, emptyList())))
        assertEquals(40f, summary.protein)
        assertNull(summary.carbs)
        assertNull(summary.fat)
        assertFalse(summary.isMacrosComplete)
    }

    // ---- computeFromFood：按克折算 ----

    @Test
    fun `按克数折算每100g营养`() {
        val food = FoodDbEntity(
            id = "f", name = "米饭", category = "主食", aliases = "",
            kcalPer100g = 130f, proteinPer100g = 2.7f, carbsPer100g = 28.2f, fatPer100g = 0.3f,
            rawCooked = RawCooked.COOKED, source = "test", sourceId = null,
            version = 1, isCustom = false, isFavorite = false, createdAt = 0L,
        )
        val result = NutritionCalculator.computeFromFood(food, 200f)
        assertEquals(260f, result.calories, 0.001f)
        assertEquals(5.4f, result.protein, 0.001f)
        assertEquals(56.4f, result.carbs, 0.001f)
        assertEquals(0.6f, result.fat, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `重量非正数时抛出异常`() {
        val food = FoodDbEntity(
            id = "f", name = "x", category = "c", aliases = "",
            kcalPer100g = 1f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 0f,
            rawCooked = RawCooked.RAW, source = "t", sourceId = null,
            version = 1, isCustom = false, isFavorite = false, createdAt = 0L,
        )
        NutritionCalculator.computeFromFood(food, 0f)
    }

    // ---- 工厂 ----

    private fun meal(
        id: String = UUID.randomUUID().toString(),
        totalCaloriesOverride: Float? = null,
        proteinOverride: Float? = null,
        carbsOverride: Float? = null,
        fatOverride: Float? = null,
    ): MealRecordEntity = MealRecordEntity(
        id = id,
        date = LocalDate.of(2026, 1, 1),
        mealType = MealType.LUNCH,
        name = "测试餐",
        time = null,
        note = null,
        photoUri = null,
        totalCaloriesOverride = totalCaloriesOverride,
        proteinOverride = proteinOverride,
        carbsOverride = carbsOverride,
        fatOverride = fatOverride,
        status = RecordStatus.SAVED,
        source = RecordSource.MANUAL_QUICK,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun item(
        calories: Float?,
        protein: Float? = null,
        carbs: Float? = null,
        fat: Float? = null,
    ): FoodItemEntity = FoodItemEntity(
        id = UUID.randomUUID().toString(),
        mealRecordId = "meal-1",
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
