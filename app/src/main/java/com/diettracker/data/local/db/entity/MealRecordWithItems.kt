package com.diettracker.data.local.db.entity

import androidx.room.Embedded
import androidx.room.Relation

/** 一餐 + 其食物明细（Room @Relation 映射，供按餐聚合查询）。 */
data class MealRecordWithItems(
    @Embedded val meal: MealRecordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealRecordId",
    )
    val items: List<FoodItemEntity>,
)
