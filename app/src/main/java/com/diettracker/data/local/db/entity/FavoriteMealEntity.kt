package com.diettracker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diettracker.domain.model.MealType

/**
 * 收藏餐次 / 快捷复用。食物明细以 JSON 快照保存于 [itemsSnapshot]。
 */
@Entity(tableName = "favorite_meals")
data class FavoriteMealEntity(
    @PrimaryKey val id: String,
    val name: String,
    val mealType: MealType?,
    val itemsSnapshot: String,
    val totalCalories: Float?,
    val createdAt: Long,
)
