package com.diettracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.diettracker.data.local.db.entity.WeightRecordEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightRecordDao {

    @Upsert
    suspend fun upsert(weight: WeightRecordEntity)

    @Delete
    suspend fun delete(weight: WeightRecordEntity)

    @Query("SELECT * FROM weight_records WHERE date = :date")
    suspend fun getByDate(date: LocalDate): WeightRecordEntity?

    @Query("SELECT * FROM weight_records WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): WeightRecordEntity?
}
