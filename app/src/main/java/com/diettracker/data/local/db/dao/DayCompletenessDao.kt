package com.diettracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.diettracker.data.local.db.entity.DayCompletenessEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface DayCompletenessDao {

    @Upsert
    suspend fun upsert(day: DayCompletenessEntity)

    @Query("DELETE FROM day_completeness WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("SELECT * FROM day_completeness WHERE date = :date")
    suspend fun getByDate(date: LocalDate): DayCompletenessEntity?

    @Query("SELECT * FROM day_completeness WHERE date = :date")
    fun observeByDate(date: LocalDate): Flow<DayCompletenessEntity?>
}
