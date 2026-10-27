package com.diettracker.data.repo

import com.diettracker.data.local.db.dao.FavoriteMealDao
import com.diettracker.data.local.db.entity.FavoriteMealEntity
import com.diettracker.domain.repository.FavoriteMealRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FavoriteMealRepositoryImpl @Inject constructor(
    private val favoriteMealDao: FavoriteMealDao,
) : FavoriteMealRepository {

    override fun observeAll(): Flow<List<FavoriteMealEntity>> =
        favoriteMealDao.observeAll()

    override suspend fun getById(id: String): FavoriteMealEntity? =
        favoriteMealDao.getById(id)

    override suspend fun saveFavorite(favorite: FavoriteMealEntity) =
        favoriteMealDao.upsert(favorite)

    override suspend fun deleteFavorite(favorite: FavoriteMealEntity) =
        favoriteMealDao.delete(favorite)
}
