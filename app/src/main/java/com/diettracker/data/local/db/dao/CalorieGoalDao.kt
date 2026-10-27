package com.diettracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.diettracker.data.local.db.entity.CalorieGoalEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieGoalDao {

    @Upsert
    suspend fun upsert(goal: CalorieGoalEntity)

    /** 取某日生效的目标：生效日期 <= date 中最新一条。 */
    @Query("SELECT * FROM calorie_goals WHERE effectiveDate <= :date ORDER BY effectiveDate DESC LIMIT 1")
    suspend fun getEffectiveOn(date: LocalDate): CalorieGoalEntity?

    @Query("SELECT * FROM calorie_goals WHERE effectiveDate <= :date ORDER BY effectiveDate DESC LIMIT 1")
    fun observeEffectiveOn(date: LocalDate): Flow<CalorieGoalEntity?>

    @Query("SELECT * FROM calorie_goals ORDER BY effectiveDate DESC")
    fun observeAll(): Flow<List<CalorieGoalEntity>>
}
