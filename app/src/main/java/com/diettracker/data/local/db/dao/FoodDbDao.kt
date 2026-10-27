package com.diettracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.diettracker.data.local.db.entity.FoodDbEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDbDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<FoodDbEntity>)

    @Upsert
    suspend fun upsert(food: FoodDbEntity)

    @Query("SELECT * FROM food_db WHERE id = :id")
    suspend fun getById(id: String): FoodDbEntity?

    @Query(
        "SELECT * FROM food_db " +
            "WHERE name LIKE '%' || :query || '%' " +
            "OR aliases LIKE '%' || :query || '%' " +
            "OR category LIKE '%' || :query || '%' " +
            "ORDER BY isFavorite DESC, name ASC LIMIT :limit",
    )
    fun search(query: String, limit: Int = 50): Flow<List<FoodDbEntity>>

    @Query("SELECT COUNT(*) FROM food_db")
    suspend fun count(): Int
}
