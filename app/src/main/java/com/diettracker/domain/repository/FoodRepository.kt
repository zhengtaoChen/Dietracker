package com.diettracker.domain.repository

import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.domain.model.NutritionValues
import kotlinx.coroutines.flow.Flow

interface FoodRepository {

    fun search(query: String, limit: Int = 50): Flow<List<FoodDbEntity>>

    suspend fun getFood(id: String): FoodDbEntity?

    suspend fun saveCustomFood(food: FoodDbEntity)

    /** 由本地营养库按克数折算；食物不存在时返回 null。 */
    suspend fun getNutrition(foodId: String, grams: Float): NutritionValues?

    suspend fun foodCount(): Int
}
