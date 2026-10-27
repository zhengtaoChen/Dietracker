package com.diettracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.data.local.db.entity.MealRecordWithItems
import com.diettracker.domain.model.MealType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface MealRecordDao {

    @Upsert
    suspend fun upsert(record: MealRecordEntity)

    @Query("DELETE FROM meal_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM meal_records WHERE id = :id")
    suspend fun getById(id: String): MealRecordEntity?

    /** 全部已引用照片的相对路径（含草稿），用于无主文件清理。 */
    @Query("SELECT photoUri FROM meal_records WHERE photoUri IS NOT NULL")
    suspend fun getAllPhotoUris(): List<String>

    /** 仅已保存记录（统计用），按时间排序。 */
    @Query("SELECT * FROM meal_records WHERE date = :date AND status = 'SAVED' ORDER BY time ASC, createdAt ASC")
    fun observeSavedByDate(date: LocalDate): Flow<List<MealRecordEntity>>

    /** 含草稿的全部记录。 */
    @Query("SELECT * FROM meal_records WHERE date = :date ORDER BY time ASC, createdAt ASC")
    fun observeAllByDate(date: LocalDate): Flow<List<MealRecordEntity>>

    @Query("SELECT * FROM meal_records WHERE date = :date ORDER BY time ASC, createdAt ASC")
    suspend fun getAllByDate(date: LocalDate): List<MealRecordEntity>

    @Query("SELECT * FROM meal_records WHERE status = 'DRAFT' ORDER BY updatedAt DESC")
    fun observeDrafts(): Flow<List<MealRecordEntity>>

    /** 某日某餐次是否已有已保存记录（提醒跳过判断用，PRD §11）。 */
    @Query("SELECT * FROM meal_records WHERE date = :date AND mealType = :mealType AND status = 'SAVED' LIMIT 1")
    suspend fun findSavedByMealType(date: LocalDate, mealType: MealType): MealRecordEntity?

    @Transaction
    @Query("SELECT * FROM meal_records WHERE date = :date AND status = 'SAVED' ORDER BY time ASC, createdAt ASC")
    fun observeSavedWithItems(date: LocalDate): Flow<List<MealRecordWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_records WHERE id = :id")
    suspend fun getWithItems(id: String): MealRecordWithItems?
}
