package com.diettracker.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diettracker.domain.model.FoodSource

/**
 * 食物明细（一餐含多条）。
 *
 * 营养字段全部可空：缺失 ≠ 0（PRD §13.4）。
 * 直接落库解析后的营养值 = 使用时的快照；[foodDbId]/[foodDbVersion] 用于追溯来源，
 * 本地营养库更新不回溯修改历史（PRD §13.7）。
 */
@Entity(
    tableName = "food_items",
    foreignKeys = [
        ForeignKey(
            entity = MealRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealRecordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mealRecordId")],
)
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val mealRecordId: String,
    val sortOrder: Int,
    val name: String,
    val category: String?,
    /** 重量(g)或份量。 */
    val amount: Float?,
    val amountUnit: String?,
    val calories: Float?,
    val protein: Float?,
    val carbs: Float?,
    val fat: Float?,
    val source: FoodSource,
    val foodDbId: String?,
    val foodDbVersion: Int?,
)
