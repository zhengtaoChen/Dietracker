package com.diettracker.data.repo

import com.diettracker.data.local.db.dao.FoodDbDao
import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.domain.model.NutritionCalculator
import com.diettracker.domain.model.NutritionValues
import com.diettracker.domain.repository.FoodRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val foodDbDao: FoodDbDao,
) : FoodRepository {

    override fun search(query: String, limit: Int): Flow<List<FoodDbEntity>> =
        foodDbDao.search(query, limit)

    override suspend fun getFood(id: String): FoodDbEntity? =
        foodDbDao.getById(id)

    override suspend fun saveCustomFood(food: FoodDbEntity) =
        foodDbDao.upsert(food)

    override suspend fun getNutrition(foodId: String, grams: Float): NutritionValues? {
        val food = foodDbDao.getById(foodId) ?: return null
        return NutritionCalculator.computeFromFood(food, grams)
    }

    override suspend fun foodCount(): Int = foodDbDao.count()
}
