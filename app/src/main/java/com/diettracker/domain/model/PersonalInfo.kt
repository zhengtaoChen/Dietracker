package com.diettracker.domain.model

/**
 * 用户个人资料（用于自动估算每日热量目标，PRD §7）。
 *
 * 所有字段均可空：未填写时不影响 App 正常使用（PRD §3 不强制填写个人信息）。
 * 个人资料属于非敏感设置，使用 DataStore 存储。
 */
data class PersonalInfo(
    val sex: Sex? = null,
    val ageYears: Int? = null,
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val activityLevel: ActivityLevel? = null,
) {
    /** 自动估算所需的全部字段是否齐备。 */
    val isCompleteForEstimate: Boolean
        get() = sex != null && ageYears != null && heightCm != null &&
            weightKg != null && activityLevel != null
}
