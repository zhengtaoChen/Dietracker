package com.diettracker.domain.repository

import com.diettracker.data.local.db.entity.CalorieGoalEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface GoalRepository {

    fun observeEffectiveGoal(date: LocalDate): Flow<CalorieGoalEntity?>

    fun observeAll(): Flow<List<CalorieGoalEntity>>

    suspend fun getEffectiveGoal(date: LocalDate): CalorieGoalEntity?

    suspend fun saveGoal(goal: CalorieGoalEntity)
}
