package com.diettracker.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.RecordSource
import com.diettracker.domain.model.RecordStatus
import java.time.LocalDate
import java.time.LocalTime

/**
 * 餐次/饮食记录（核心表）。
 *
 * 关键设计：
 * - [totalCaloriesOverride] 为空则取明细合计，二者互斥取一（PRD §13.2/13.3）。
 * - [status] 区分草稿与正式记录，草稿不参与统计（PRD §13.1）。
 * - 「记录完整度」是「按天」属性，已从本表拆出到 DayCompletenessEntity。
 */
@Entity(
    tableName = "meal_records",
    indices = [Index("date")],
)
data class MealRecordEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val mealType: MealType,
    /** 餐次名称（快速模式仅需名称 + 总热量即可保存）。 */
    val name: String?,
    val time: LocalTime?,
    val note: String?,
    val photoUri: String?,
    val totalCaloriesOverride: Float?,
    val status: RecordStatus,
    val source: RecordSource,
    val createdAt: Long,
    val updatedAt: Long,
)
