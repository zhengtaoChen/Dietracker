package com.diettracker.data.repo

import com.diettracker.data.local.db.dao.DayCompletenessDao
import com.diettracker.data.local.db.entity.DayCompletenessEntity
import com.diettracker.domain.model.DayCompletenessStatus
import com.diettracker.domain.repository.DayCompletenessRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DayCompletenessRepositoryImpl @Inject constructor(
    private val dayCompletenessDao: DayCompletenessDao,
) : DayCompletenessRepository {

    override fun observeByDate(date: LocalDate): Flow<DayCompletenessEntity?> =
        dayCompletenessDao.observeByDate(date)

    override suspend fun getByDate(date: LocalDate): DayCompletenessEntity? =
        dayCompletenessDao.getByDate(date)

    override suspend fun setStatus(date: LocalDate, status: DayCompletenessStatus) {
        dayCompletenessDao.upsert(
            DayCompletenessEntity(
                date = date,
                status = status,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
