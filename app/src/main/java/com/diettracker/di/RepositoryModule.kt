package com.diettracker.di

import com.diettracker.data.crypto.AndroidKeystoreApiKeyCrypto
import com.diettracker.data.crypto.ApiKeyCrypto
import com.diettracker.data.network.AiConnectionTesterImpl
import com.diettracker.data.repo.AiApiConfigRepositoryImpl
import com.diettracker.data.repo.DayCompletenessRepositoryImpl
import com.diettracker.data.repo.FavoriteMealRepositoryImpl
import com.diettracker.data.repo.FoodRepositoryImpl
import com.diettracker.data.repo.GoalRepositoryImpl
import com.diettracker.data.repo.MealRepositoryImpl
import com.diettracker.data.repo.PhotoRepositoryImpl
import com.diettracker.data.repo.UserSettingsRepositoryImpl
import com.diettracker.data.repo.WeightRepositoryImpl
import com.diettracker.domain.repository.AiApiConfigRepository
import com.diettracker.domain.repository.AiConnectionTester
import com.diettracker.domain.repository.DayCompletenessRepository
import com.diettracker.domain.repository.FavoriteMealRepository
import com.diettracker.domain.repository.FoodRepository
import com.diettracker.domain.repository.GoalRepository
import com.diettracker.domain.repository.MealRepository
import com.diettracker.domain.repository.PhotoRepository
import com.diettracker.domain.repository.UserSettingsRepository
import com.diettracker.domain.repository.WeightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository

    @Binds
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    abstract fun bindFavoriteMealRepository(impl: FavoriteMealRepositoryImpl): FavoriteMealRepository

    @Binds
    abstract fun bindDayCompletenessRepository(impl: DayCompletenessRepositoryImpl): DayCompletenessRepository

    @Binds
    abstract fun bindUserSettingsRepository(impl: UserSettingsRepositoryImpl): UserSettingsRepository

    @Binds
    abstract fun bindAiApiConfigRepository(impl: AiApiConfigRepositoryImpl): AiApiConfigRepository

    @Binds
    abstract fun bindAiConnectionTester(impl: AiConnectionTesterImpl): AiConnectionTester

    @Binds
    abstract fun bindApiKeyCrypto(impl: AndroidKeystoreApiKeyCrypto): ApiKeyCrypto
}
