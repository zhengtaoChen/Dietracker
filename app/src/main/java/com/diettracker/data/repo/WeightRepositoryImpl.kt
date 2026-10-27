package com.diettracker.data.repo

import com.diettracker.data.local.db.dao.WeightRecordDao
import com.diettracker.data.local.db.entity.WeightRecordEntity
import com.diettracker.domain.repository.WeightRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val weightRecordDao: WeightRecordDao,
) : WeightRepository {

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<WeightRecordEntity>> =
        weightRecordDao.observeRange(start, end)

    override suspend fun getByDate(date: LocalDate): WeightRecordEntity? =
        weightRecordDao.getByDate(date)

    override suspend fun getLatest(): WeightRecordEntity? =
        weightRecordDao.getLatest()

    override suspend fun saveWeight(weight: WeightRecordEntity) =
        weightRecordDao.upsert(weight)

    override suspend fun deleteWeight(weight: WeightRecordEntity) =
        weightRecordDao.delete(weight)
}
