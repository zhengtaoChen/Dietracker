package com.diettracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.diettracker.data.local.db.entity.FoodItemEntity

@Dao
interface FoodItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodItemEntity>)

    @Upsert
    suspend fun upsert(item: FoodItemEntity)

    @Query("DELETE FROM food_items WHERE mealRecordId = :mealRecordId")
    suspend fun deleteByMeal(mealRecordId: String)

    @Query("SELECT * FROM food_items WHERE mealRecordId = :mealRecordId ORDER BY sortOrder ASC")
    suspend fun getByMeal(mealRecordId: String): List<FoodItemEntity>
}
