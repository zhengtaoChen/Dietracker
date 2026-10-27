package com.diettracker.domain.repository

import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.data.local.db.entity.MealRecordWithItems
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.NutritionSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface MealRepository {

    /** 仅已保存记录（统计口径）。 */
    fun observeSavedMeals(date: LocalDate): Flow<List<MealRecordEntity>>

    /** 含草稿的全部记录。 */
    fun observeMealsByDate(date: LocalDate): Flow<List<MealRecordEntity>>

    fun observeDrafts(): Flow<List<MealRecordEntity>>

    /** 某日营养汇总（仅统计已保存记录，草稿被排除）。 */
    fun observeNutritionSummary(date: LocalDate): Flow<NutritionSummary>

    suspend fun getMeal(id: String): MealRecordEntity?

    suspend fun getMealWithItems(id: String): MealRecordWithItems?

    /** 保存餐次及其明细（事务内替换明细）。 */
    suspend fun saveMeal(meal: MealRecordEntity, items: List<FoodItemEntity>)

    suspend fun deleteMeal(id: String)

    /** 删除未被任何记录引用的照片文件，返回删除数量。 */
    suspend fun deleteOrphanedPhotos(): Int

    /** 某日某餐次是否已有已保存记录（提醒跳过判断用）。 */
    suspend fun hasSavedMeal(date: LocalDate, mealType: MealType): Boolean
}
