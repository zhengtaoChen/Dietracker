package com.diettracker.domain.repository

import com.diettracker.data.local.db.entity.FavoriteMealEntity
import kotlinx.coroutines.flow.Flow

interface FavoriteMealRepository {

    fun observeAll(): Flow<List<FavoriteMealEntity>>

    suspend fun getById(id: String): FavoriteMealEntity?

    suspend fun saveFavorite(favorite: FavoriteMealEntity)

    suspend fun deleteFavorite(favorite: FavoriteMealEntity)
}
