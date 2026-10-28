package com.diettracker.domain.repository

import com.diettracker.data.local.db.entity.CalorieGoalEntity
import com.diettracker.domain.model.GoalType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface GoalRepository {

    fun observeEffectiveGoal(date: LocalDate): Flow<CalorieGoalEntity?>

    fun observeAll(): Flow<List<CalorieGoalEntity>>

    suspend fun getEffectiveGoal(date: LocalDate): CalorieGoalEntity?

    /**
     * 保存某生效日期的目标。同一日期重复保存会覆盖（每日期一条），
     * 历史日期按当时生效目标回显，不因后续修改而变化（PRD §13.6）。
     */
    suspend fun saveGoal(
        date: LocalDate,
        calories: Float,
        goalType: GoalType,
        estimateParams: String?,
    )
}
