package com.diettracker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diettracker.domain.model.RawCooked

/**
 * 本地营养数据库（内置种子 + 用户自定义）。
 *
 * 每 100g 参考营养值要求完整（非空）：这是「按克折算」的基准数据，缺失无法折算。
 * 「缺失 ≠ 0」规则针对的是 FoodItemEntity（用餐明细），而非本参考表。
 */
@Entity(tableName = "food_db")
data class FoodDbEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    /** 别名，`|` 分隔，供搜索匹配。 */
    val aliases: String,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val rawCooked: RawCooked,
    /** 数据来源（如 USDA FoodData Central）。 */
    val source: String,
    /** 来源条目标识（外部 ID 或本地可追溯键）。 */
    val sourceId: String?,
    val version: Int,
    val isCustom: Boolean,
    val isFavorite: Boolean,
    val createdAt: Long,
)
