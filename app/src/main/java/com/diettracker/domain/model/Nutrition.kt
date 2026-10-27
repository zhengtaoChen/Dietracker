package com.diettracker.domain.model

/**
 * 由本地营养库按重量计算得出的完整营养值（每 100g 参考值 × 克数）。
 * 四项均为已知值（本地库条目要求数据完整），故为非空。
 */
data class NutritionValues(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
)

/**
 * 某一天的营养汇总结果。
 *
 * 四项营养字段均为「已知值的合计」：
 * - 若当天无任何已知值，对应字段为 null（表示「未知」，而非 0）。
 * - [isCaloriesComplete] 表示热量是否完整（无缺失明细、无空餐次）。
 * - [isMacrosComplete] 表示三大营养素是否完整（每条明细三项均已知）。
 */
data class NutritionSummary(
    val calories: Float?,
    val protein: Float?,
    val carbs: Float?,
    val fat: Float?,
    val isCaloriesComplete: Boolean,
    val isMacrosComplete: Boolean,
)
