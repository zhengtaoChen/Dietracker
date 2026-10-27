package com.diettracker.domain.repository

import com.diettracker.data.local.db.entity.WeightRecordEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface WeightRepository {

    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<WeightRecordEntity>>

    suspend fun getByDate(date: LocalDate): WeightRecordEntity?

    suspend fun getLatest(): WeightRecordEntity?

    suspend fun saveWeight(weight: WeightRecordEntity)

    suspend fun deleteWeight(weight: WeightRecordEntity)
}
