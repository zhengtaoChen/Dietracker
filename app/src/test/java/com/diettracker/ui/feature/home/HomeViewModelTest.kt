package com.diettracker.ui.feature.home

import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.data.local.db.entity.MealRecordWithItems
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.NutritionSummary
import com.diettracker.domain.repository.MealRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `删除餐次调用仓库删除`() {
        val repo = FakeMealRepository()
        val vm = HomeViewModel(repo)

        vm.deleteMeal("m1")

        assertTrue(repo.deletedIds.contains("m1"))
    }

    @Test
    fun `删除失败发出错误消息`() {
        val repo = FakeMealRepository().apply { deleteError = RuntimeException("boom") }
        val vm = HomeViewModel(repo)
        val messages = mutableListOf<String>()
        runBlocking {
            val job = launch(Dispatchers.Unconfined) { vm.messages.collect { messages += it } }
            vm.deleteMeal("m1")
            job.cancel()
        }

        assertTrue(messages.isNotEmpty())
        assertTrue(messages.first().contains("删除失败"))
    }

    private class FakeMealRepository : MealRepository {
        val deletedIds = mutableListOf<String>()
        var deleteError: Exception? = null

        override fun observeSavedMeals(date: LocalDate): Flow<List<MealRecordEntity>> =
            MutableStateFlow(emptyList())

        override fun observeMealsByDate(date: LocalDate): Flow<List<MealRecordEntity>> =
            MutableStateFlow(emptyList())

        override fun observeDrafts(): Flow<List<MealRecordEntity>> =
            MutableStateFlow(emptyList())

        override fun observeNutritionSummary(date: LocalDate): Flow<NutritionSummary> =
            MutableStateFlow(NutritionSummary(null, null, null, null, false, false))

        override suspend fun getMeal(id: String): MealRecordEntity? = null

        override suspend fun getMealWithItems(id: String): MealRecordWithItems? = null

        override suspend fun saveMeal(meal: MealRecordEntity, items: List<FoodItemEntity>) = Unit

        override suspend fun deleteMeal(id: String) {
            deleteError?.let { throw it }
            deletedIds += id
        }

        override suspend fun deleteOrphanedPhotos(): Int = 0

        override suspend fun hasSavedMeal(date: LocalDate, mealType: MealType): Boolean = false
    }
}
