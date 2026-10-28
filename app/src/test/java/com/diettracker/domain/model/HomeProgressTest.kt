package com.diettracker.domain.model

import com.diettracker.data.local.db.entity.CalorieGoalEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeProgressTest {

    @Test
    fun `无目标无摄入时全部为空`() {
        val p = computeHomeProgress(null, null)
        assertNull(p.goalCalories)
        assertNull(p.consumedCalories)
        assertNull(p.remainingCalories)
        assertNull(p.progressFraction)
        assertFalse(p.isOverGoal)
    }

    @Test
    fun `无目标但有摄入时不显示剩余和进度`() {
        val p = computeHomeProgress(summary(calories = 1200f), null)
        assertEquals(1200f, p.consumedCalories)
        assertNull(p.goalCalories)
        assertNull(p.remainingCalories)
        assertNull(p.progressFraction)
        assertFalse(p.isOverGoal)
    }

    @Test
    fun `有目标但摄入未知时不显示剩余和进度`() {
        val p = computeHomeProgress(summary(calories = null), goal(2000f))
        assertEquals(2000f, p.goalCalories)
        assertNull(p.consumedCalories)
        assertNull(p.remainingCalories)
        assertNull(p.progressFraction)
        assertFalse(p.isOverGoal)
    }

    @Test
    fun `正常进度计算正确`() {
        val p = computeHomeProgress(summary(calories = 1500f), goal(2000f))
        assertEquals(500f, p.remainingCalories)
        assertEquals(0.75f, p.progressFraction)
        assertFalse(p.isOverGoal)
    }

    @Test
    fun `超出目标时剩余为负且进度封顶`() {
        val p = computeHomeProgress(summary(calories = 2500f), goal(2000f))
        assertEquals(-500f, p.remainingCalories)
        assertEquals(1f, p.progressFraction)
        assertTrue(p.isOverGoal)
    }

    @Test
    fun `恰好达标时剩余为零且不判为超出`() {
        val p = computeHomeProgress(summary(calories = 2000f), goal(2000f))
        assertEquals(0f, p.remainingCalories)
        assertEquals(1f, p.progressFraction)
        assertFalse(p.isOverGoal)
    }

    private fun summary(calories: Float?): NutritionSummary =
        NutritionSummary(calories, null, null, null, false, false)

    private fun goal(calories: Float): CalorieGoalEntity =
        CalorieGoalEntity(
            id = "2026-01-01",
            effectiveDate = LocalDate.of(2026, 1, 1),
            calories = calories,
            goalType = GoalType.MANUAL,
            estimateParams = null,
            createdAt = 0L,
        )
}
