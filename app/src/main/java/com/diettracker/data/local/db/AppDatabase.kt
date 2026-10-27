package com.diettracker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.diettracker.data.local.db.dao.CalorieGoalDao
import com.diettracker.data.local.db.dao.DayCompletenessDao
import com.diettracker.data.local.db.dao.FavoriteMealDao
import com.diettracker.data.local.db.dao.FoodDbDao
import com.diettracker.data.local.db.dao.FoodItemDao
import com.diettracker.data.local.db.dao.MealRecordDao
import com.diettracker.data.local.db.dao.WeightRecordDao
import com.diettracker.data.local.db.entity.CalorieGoalEntity
import com.diettracker.data.local.db.entity.DayCompletenessEntity
import com.diettracker.data.local.db.entity.FavoriteMealEntity
import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.data.local.db.entity.WeightRecordEntity

@Database(
    entities = [
        MealRecordEntity::class,
        FoodItemEntity::class,
        FoodDbEntity::class,
        WeightRecordEntity::class,
        CalorieGoalEntity::class,
        FavoriteMealEntity::class,
        DayCompletenessEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealRecordDao(): MealRecordDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun foodDbDao(): FoodDbDao
    abstract fun weightRecordDao(): WeightRecordDao
    abstract fun calorieGoalDao(): CalorieGoalDao
    abstract fun favoriteMealDao(): FavoriteMealDao
    abstract fun dayCompletenessDao(): DayCompletenessDao
}
