package com.diettracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.diettracker.data.local.db.entity.FavoriteMealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMealDao {

    @Upsert
    suspend fun upsert(favorite: FavoriteMealEntity)

    @Delete
    suspend fun delete(favorite: FavoriteMealEntity)

    @Query("SELECT * FROM favorite_meals WHERE id = :id")
    suspend fun getById(id: String): FavoriteMealEntity?

    @Query("SELECT * FROM favorite_meals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteMealEntity>>
}
