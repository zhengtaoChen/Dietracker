package com.diettracker.domain.model

import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.data.local.db.entity.MealRecordWithItems

/**
 * 营养计算规则（纯函数，无 Android 依赖，可独立单元测试）。
 *
 * 对应 PRD §13 一致性规则：
 * - §13.2/13.3 手动覆盖总热量优先，与明细合计互斥取一，不重复相加。
 * - §13.4 缺失营养数据不等于零（缺失项以 null 表示，不计入已知合计）。
 */
object NutritionCalculator {

    /**
     * 单餐热量：有手动覆盖值时取覆盖值，否则取明细「已知热量」合计。
     * 返回 null 表示该餐没有任何已知热量（而非 0）。
     */
    fun mealCalories(meal: MealRecordEntity, items: List<FoodItemEntity>): Float? {
        meal.totalCaloriesOverride?.let { return it }
        val known = items.mapNotNull { it.calories }
        return if (known.isEmpty()) null else known.sum()
    }

    /** 单餐热量是否完整：有覆盖值，或明细全部条目热量已知（且至少有一条明细）。 */
    fun isMealCaloriesComplete(meal: MealRecordEntity, items: List<FoodItemEntity>): Boolean {
        if (meal.totalCaloriesOverride != null) return true
        if (items.isEmpty()) return false
        return items.all { it.calories != null }
    }

    /**
     * 汇总某一天的营养。入参应为「已保存（SAVED）」的餐次；草稿由调用方过滤。
     */
    fun dailySummary(mealsWithItems: List<MealRecordWithItems>): NutritionSummary {
        if (mealsWithItems.isEmpty()) {
            return NutritionSummary(null, null, null, null, false, false)
        }

        val allItems = mealsWithItems.flatMap { it.items }

        val calories = mealsWithItems
            .mapNotNull { (meal, items) -> mealCalories(meal, items) }
            .takeIf { it.isNotEmpty() }
            ?.sum()

        val protein = allItems.mapNotNull { it.protein }.takeIf { it.isNotEmpty() }?.sum()
        val carbs = allItems.mapNotNull { it.carbs }.takeIf { it.isNotEmpty() }?.sum()
        val fat = allItems.mapNotNull { it.fat }.takeIf { it.isNotEmpty() }?.sum()

        val isCaloriesComplete = mealsWithItems.all { (meal, items) ->
            isMealCaloriesComplete(meal, items)
        }
        val isMacrosComplete = allItems.isNotEmpty() && allItems.all {
            it.protein != null && it.carbs != null && it.fat != null
        }

        return NutritionSummary(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            isCaloriesComplete = isCaloriesComplete,
            isMacrosComplete = isMacrosComplete,
        )
    }

    /**
     * 按克数由本地营养库（每 100g）折算营养值。
     * 本地库条目营养数据要求完整（非空），故结果四项均为已知值。
     */
    fun computeFromFood(food: FoodDbEntity, grams: Float): NutritionValues {
        require(grams > 0f) { "重量必须为正数" }
        val factor = grams / 100f
        return NutritionValues(
            calories = food.kcalPer100g * factor,
            protein = food.proteinPer100g * factor,
            carbs = food.carbsPer100g * factor,
            fat = food.fatPer100g * factor,
        )
    }
}
