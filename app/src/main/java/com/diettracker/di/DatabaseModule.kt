package com.diettracker.di

import android.content.Context
import androidx.room.Room
import com.diettracker.data.local.db.AppDatabase
import com.diettracker.data.local.db.dao.CalorieGoalDao
import com.diettracker.data.local.db.dao.DayCompletenessDao
import com.diettracker.data.local.db.dao.FavoriteMealDao
import com.diettracker.data.local.db.dao.FoodDbDao
import com.diettracker.data.local.db.dao.FoodItemDao
import com.diettracker.data.local.db.dao.MealRecordDao
import com.diettracker.data.local.db.dao.WeightRecordDao
import com.diettracker.data.local.db.seed.FoodSeedCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        callback: FoodSeedCallback,
    ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
        .addCallback(callback)
        .build()

    @Provides
    fun provideMealRecordDao(db: AppDatabase): MealRecordDao = db.mealRecordDao()

    @Provides
    fun provideFoodItemDao(db: AppDatabase): FoodItemDao = db.foodItemDao()

    @Provides
    fun provideFoodDbDao(db: AppDatabase): FoodDbDao = db.foodDbDao()

    @Provides
    fun provideWeightRecordDao(db: AppDatabase): WeightRecordDao = db.weightRecordDao()

    @Provides
    fun provideCalorieGoalDao(db: AppDatabase): CalorieGoalDao = db.calorieGoalDao()

    @Provides
    fun provideFavoriteMealDao(db: AppDatabase): FavoriteMealDao = db.favoriteMealDao()

    @Provides
    fun provideDayCompletenessDao(db: AppDatabase): DayCompletenessDao = db.dayCompletenessDao()

    private const val DATABASE_NAME = "diettracker.db"
}
