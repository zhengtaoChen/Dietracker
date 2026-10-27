package com.diettracker.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diettracker.domain.model.GoalType
import java.time.LocalDate

/**
 * 热量目标及历史。目标与 [effectiveDate] 绑定，历史统计按当日生效目标回显（PRD §13.6）。
 */
@Entity(
    tableName = "calorie_goals",
    indices = [Index("effectiveDate")],
)
data class CalorieGoalEntity(
    @PrimaryKey val id: String,
    val effectiveDate: LocalDate,
    val calories: Float,
    val goalType: GoalType,
    /** 自动估算参数快照（JSON），MANUAL 时可为空。 */
    val estimateParams: String?,
    val createdAt: Long,
)
