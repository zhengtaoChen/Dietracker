package com.diettracker.domain.repository

import com.diettracker.data.local.db.entity.DayCompletenessEntity
import com.diettracker.domain.model.DayCompletenessStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface DayCompletenessRepository {

    fun observeByDate(date: LocalDate): Flow<DayCompletenessEntity?>

    suspend fun getByDate(date: LocalDate): DayCompletenessEntity?

    suspend fun setStatus(date: LocalDate, status: DayCompletenessStatus)
}
