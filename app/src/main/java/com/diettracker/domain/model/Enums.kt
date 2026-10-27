package com.diettracker.domain.model

/**
 * 餐次类型。Room 默认以枚举 name() 字符串落库；后续只允许追加常量，禁止重命名已有常量。
 */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
}

/**
 * 记录状态：DRAFT 不参与任何正式统计（PRD §13.1）。
 */
enum class RecordStatus {
    DRAFT,
    SAVED,
}

/**
 * 记录来源（PRD §4.1 三种入口 + 快捷复用 + 本地库）。
 */
enum class RecordSource {
    MANUAL_QUICK,
    MANUAL_DETAIL,
    AI_TEXT,
    AI_IMAGE,
    QUICK_COPY,
    LOCAL_DB,
}

/**
 * 食物明细的营养数据来源。
 */
enum class FoodSource {
    LOCAL_DB,
    CUSTOM,
    AI,
    MANUAL,
}

/**
 * 本地营养库中食物的生熟状态。
 */
enum class RawCooked {
    RAW,
    COOKED,
}

/**
 * 热量目标类型（PRD §7）。
 */
enum class GoalType {
    LOSE,
    MAINTAIN,
    GAIN,
    MANUAL,
}

/**
 * 「某一天」的记录完整度状态（PRD §9 / §10.3）。
 * 完整度是「按天」的属性，独立于单条餐次记录，故单独成表。
 */
enum class DayCompletenessStatus {
    UNCONFIRMED,
    COMPLETE,
    INCOMPLETE,
}
