package com.diettracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieEstimatorTest {

    @Test
    fun `男性基础代谢公式正确`() {
        // 10*70 + 6.25*175 - 5*30 + 5
        assertEquals(1648.75f, CalorieEstimator.bmr(Sex.MALE, 30, 175f, 70f), 0.01f)
    }

    @Test
    fun `女性基础代谢公式正确`() {
        // 10*70 + 6.25*175 - 5*30 - 161
        assertEquals(1482.75f, CalorieEstimator.bmr(Sex.FEMALE, 30, 175f, 70f), 0.01f)
    }

    @Test
    fun `TDEE等于基础代谢乘活动系数`() {
        val bmr = CalorieEstimator.bmr(Sex.MALE, 30, 175f, 70f)
        assertEquals(bmr * 1.2f, CalorieEstimator.tdee(bmr, ActivityLevel.SEDENTARY), 0.01f)
    }

    @Test
    fun `建议热量按目标增减`() {
        assertEquals(
            1478.5f,
            CalorieEstimator.suggestedCalories(Sex.MALE, 30, 175f, 70f, ActivityLevel.SEDENTARY, GoalType.LOSE),
            0.01f,
        )
        assertEquals(
            1978.5f,
            CalorieEstimator.suggestedCalories(Sex.MALE, 30, 175f, 70f, ActivityLevel.SEDENTARY, GoalType.MAINTAIN),
            0.01f,
        )
        assertEquals(
            2478.5f,
            CalorieEstimator.suggestedCalories(Sex.MALE, 30, 175f, 70f, ActivityLevel.SEDENTARY, GoalType.GAIN),
            0.01f,
        )
    }

    @Test
    fun `非法年龄抛出异常`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            CalorieEstimator.bmr(Sex.MALE, 10, 175f, 70f)
        }
        assertTrue(e.message!!.contains("年龄"))
    }

    @Test
    fun `非法身高抛出异常`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            CalorieEstimator.bmr(Sex.MALE, 30, 300f, 70f)
        }
        assertTrue(e.message!!.contains("身高"))
    }

    @Test
    fun `非法体重抛出异常`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            CalorieEstimator.bmr(Sex.MALE, 30, 175f, 20f)
        }
        assertTrue(e.message!!.contains("体重"))
    }

    @Test
    fun `估算参数快照包含关键字段`() {
        val json = CalorieEstimator.estimateParamsJson(Sex.FEMALE, 28, 160f, 55f, ActivityLevel.LIGHT)
        assertTrue(json.contains("\"sex\":\"FEMALE\""))
        assertTrue(json.contains("\"age\":28"))
        assertTrue(json.contains("\"heightCm\":160.0"))
        assertTrue(json.contains("\"weightKg\":55.0"))
        assertTrue(json.contains("\"activityLevel\":\"LIGHT\""))
    }
}
