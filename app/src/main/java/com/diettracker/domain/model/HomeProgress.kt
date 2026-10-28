package com.diettracker.domain.model

import com.diettracker.data.local.db.entity.CalorieGoalEntity

/**
 * 首页热量目标进度（PRD §8）。
 *
 * - [consumedCalories]/[goalCalories] 为 null 表示未知/未设置（≠ 0，PRD §13.4/§13.5）。
 * - [remainingCalories] = 目标 − 摄入，可为负（超出目标）。
 * - [progressFraction] 为 0..1 的展示进度，仅当两者均已知时非空；超出目标时封顶 1。
 */
data class HomeProgress(
    val goalCalories: Float?,
    val consumedCalories: Float?,
    val remainingCalories: Float?,
    val progressFraction: Float?,
    val isOverGoal: Boolean,
)

/**
 * 由今日营养汇总与当日生效目标计算首页进度。
 * 纯函数，无 Android 依赖，可独立单元测试。
 */
fun computeHomeProgress(summary: NutritionSummary?, goal: CalorieGoalEntity?): HomeProgress {
    val consumed = summary?.calories
    val goalCal = goal?.calories
    val remaining = if (goalCal != null && consumed != null) goalCal - consumed else null
    val fraction = if (goalCal != null && consumed != null && goalCal > 0f) {
        (consumed / goalCal).coerceIn(0f, 1f)
    } else null
    return HomeProgress(
        goalCalories = goalCal,
        consumedCalories = consumed,
        remainingCalories = remaining,
        progressFraction = fraction,
        isOverGoal = remaining != null && remaining < 0f,
    )
}
