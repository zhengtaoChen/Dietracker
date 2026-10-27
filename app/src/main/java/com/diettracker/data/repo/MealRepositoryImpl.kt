package com.diettracker.data.repo

import androidx.room.withTransaction
import com.diettracker.data.local.db.AppDatabase
import com.diettracker.data.local.db.dao.FoodItemDao
import com.diettracker.data.local.db.dao.MealRecordDao
import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.data.local.db.entity.MealRecordWithItems
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.NutritionCalculator
import com.diettracker.domain.model.NutritionSummary
import com.diettracker.domain.repository.MealRepository
import com.diettracker.domain.repository.PhotoRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MealRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val mealRecordDao: MealRecordDao,
    private val foodItemDao: FoodItemDao,
    private val photoRepository: PhotoRepository,
) : MealRepository {

    override fun observeSavedMeals(date: LocalDate): Flow<List<MealRecordEntity>> =
        mealRecordDao.observeSavedByDate(date)

    override fun observeMealsByDate(date: LocalDate): Flow<List<MealRecordEntity>> =
        mealRecordDao.observeAllByDate(date)

    override fun observeDrafts(): Flow<List<MealRecordEntity>> =
        mealRecordDao.observeDrafts()

    override fun observeNutritionSummary(date: LocalDate): Flow<NutritionSummary> =
        mealRecordDao.observeSavedWithItems(date)
            .map { NutritionCalculator.dailySummary(it) }

    override suspend fun getMeal(id: String): MealRecordEntity? =
        mealRecordDao.getById(id)

    override suspend fun getMealWithItems(id: String): MealRecordWithItems? =
        mealRecordDao.getWithItems(id)

    override suspend fun saveMeal(meal: MealRecordEntity, items: List<FoodItemEntity>) {
        database.withTransaction {
            mealRecordDao.upsert(meal)
            foodItemDao.deleteByMeal(meal.id)
            foodItemDao.insertAll(items)
        }
    }

    override suspend fun deleteMeal(id: String) {
        val meal = mealRecordDao.getById(id)
        database.withTransaction {
            foodItemDao.deleteByMeal(id)
            mealRecordDao.deleteById(id)
        }
        meal?.photoUri?.let { photoRepository.deletePhoto(it) }
    }

    override suspend fun deleteOrphanedPhotos(): Int {
        val referenced = mealRecordDao.getAllPhotoUris().toSet()
        return photoRepository.listPhotos().count { relativePath ->
            if (relativePath in referenced) false else {
                photoRepository.deletePhoto(relativePath)
                true
            }
        }
    }

    override suspend fun hasSavedMeal(date: LocalDate, mealType: MealType): Boolean =
        mealRecordDao.findSavedByMealType(date, mealType) != null
}
