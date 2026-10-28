package com.diettracker.domain.model

/**
 * 每日热量目标自动估算（PRD §7）。
 *
 * 采用 Mifflin-St Jeor 公式计算基础代谢率（BMR），乘以活动系数得到 TDEE，
 * 再按体重管理目标增减建议热量。结果仅作参考，不作为医学建议。
 */
object CalorieEstimator {

    const val LOSE_ADJUSTMENT = -500f
    const val GAIN_ADJUSTMENT = 500f

    const val AGE_MIN = 15
    const val AGE_MAX = 100
    const val HEIGHT_MIN = 100f
    const val HEIGHT_MAX = 250f
    const val WEIGHT_MIN = 30f
    const val WEIGHT_MAX = 300f

    /** Mifflin-St Jeor 基础代谢率。四项入参必须齐备且合法，否则抛 [IllegalArgumentException]。 */
    fun bmr(sex: Sex, ageYears: Int, heightCm: Float, weightKg: Float): Float {
        require(ageYears in AGE_MIN..AGE_MAX) { "年龄需在 $AGE_MIN-$AGE_MAX 岁之间" }
        require(heightCm in HEIGHT_MIN..HEIGHT_MAX) {
            "身高需在 ${HEIGHT_MIN.toInt()}-${HEIGHT_MAX.toInt()} cm 之间"
        }
        require(weightKg in WEIGHT_MIN..WEIGHT_MAX) {
            "体重需在 ${WEIGHT_MIN.toInt()}-${WEIGHT_MAX.toInt()} kg 之间"
        }
        val base = 10f * weightKg + 6.25f * heightCm - 5f * ageYears
        return if (sex == Sex.MALE) base + 5f else base - 161f
    }

    /** TDEE = BMR × 活动系数。 */
    fun tdee(bmr: Float, activityLevel: ActivityLevel): Float = bmr * activityLevel.factor

    /** 建议每日热量 = TDEE + 目标调整（减脂 -500、增重 +500、维持 0）。 */
    fun suggestedCalories(
        sex: Sex,
        ageYears: Int,
        heightCm: Float,
        weightKg: Float,
        activityLevel: ActivityLevel,
        goalType: GoalType,
    ): Float {
        val tdee = tdee(bmr(sex, ageYears, heightCm, weightKg), activityLevel)
        return when (goalType) {
            GoalType.LOSE -> tdee + LOSE_ADJUSTMENT
            GoalType.GAIN -> tdee + GAIN_ADJUSTMENT
            GoalType.MAINTAIN, GoalType.MANUAL -> tdee
        }
    }

    /** 自动估算参数快照（JSON），作为参考留存在目标记录中。 */
    fun estimateParamsJson(
        sex: Sex,
        ageYears: Int,
        heightCm: Float,
        weightKg: Float,
        activityLevel: ActivityLevel,
    ): String =
        """{"sex":"${sex.name}","age":$ageYears,"heightCm":$heightCm,""" +
            """"weightKg":$weightKg,"activityLevel":"${activityLevel.name}"}"""
}
