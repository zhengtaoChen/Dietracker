package com.diettracker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diettracker.domain.model.DayCompletenessStatus
import java.time.LocalDate

/**
 * 「某一天」的记录完整度标记。
 *
 * 完整度是「按天」的属性（PRD §9「用户可将某天标记为记录完整/可能不完整」、§10.3），
 * 与单条餐次记录无关，故从 MealRecordEntity 拆出独立成表，避免一餐一个完整度值的语义冲突。
 */
@Entity(tableName = "day_completeness")
data class DayCompletenessEntity(
    @PrimaryKey val date: LocalDate,
    val status: DayCompletenessStatus,
    val updatedAt: Long,
)
