package com.diettracker.data.repo

import com.diettracker.data.local.db.dao.CalorieGoalDao
import com.diettracker.data.local.db.entity.CalorieGoalEntity
import com.diettracker.domain.model.GoalType
import com.diettracker.domain.repository.GoalRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val calorieGoalDao: CalorieGoalDao,
) : GoalRepository {

    override fun observeEffectiveGoal(date: LocalDate): Flow<CalorieGoalEntity?> =
        calorieGoalDao.observeEffectiveOn(date)

    override fun observeAll(): Flow<List<CalorieGoalEntity>> =
        calorieGoalDao.observeAll()

    override suspend fun getEffectiveGoal(date: LocalDate): CalorieGoalEntity? =
        calorieGoalDao.getEffectiveOn(date)

    override suspend fun saveGoal(
        date: LocalDate,
        calories: Float,
        goalType: GoalType,
        estimateParams: String?,
    ) {
        calorieGoalDao.upsert(
            CalorieGoalEntity(
                id = date.toString(),
                effectiveDate = date,
                calories = calories,
                goalType = goalType,
                estimateParams = estimateParams,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}
