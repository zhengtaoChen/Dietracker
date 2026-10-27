package com.diettracker.ui.feature.record

import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.data.local.db.entity.MealRecordWithItems
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.NutritionSummary
import com.diettracker.domain.model.RecordSource
import com.diettracker.domain.model.RecordStatus
import com.diettracker.domain.repository.MealRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RecordViewModel 单元测试：验证草稿恢复、正式保存/重存复用 UUID、保留 createdAt、失败保留表单。
 *
 * 使用 UnconfinedTestDispatcher 作为 Main 调度器，使 viewModelScope 协程同步执行，便于断言。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {

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
    fun `打开草稿恢复表单内容`() = runTest {
        val repo = FakeMealRepository().apply {
            meals["d1"] = entity(id = "d1", name = "牛肉面", calories = 850f, status = RecordStatus.DRAFT, createdAt = 100L)
        }
        val vm = RecordViewModel(repo)

        vm.openDraft("d1")

        assertEquals("牛肉面", vm.form.value.name)
        assertEquals("850", vm.form.value.calories)
        assertEquals(RecordViewModel.Step.FORM, vm.step.value)
        assertFalse(vm.isDirty.value)
    }

    @Test
    fun `保存新草稿写入DRAFT并重置`() = runTest {
        val repo = FakeMealRepository()
        val vm = RecordViewModel(repo)
        vm.openForm()
        vm.onNameChange("早餐")

        vm.saveDraft()

        assertEquals(1, repo.meals.size)
        val saved = repo.meals.values.first()
        assertEquals(RecordStatus.DRAFT, saved.status)
        assertEquals("早餐", saved.name)
        assertEquals(null, saved.totalCaloriesOverride)
        assertEquals(RecordViewModel.Step.ENTRIES, vm.step.value)
        assertFalse(vm.isDirty.value)
    }

    @Test
    fun `草稿转正式记录复用UUID并保留createdAt`() = runTest {
        val repo = FakeMealRepository().apply {
            meals["d1"] = entity(id = "d1", name = "午餐", calories = 850f, status = RecordStatus.DRAFT, createdAt = 100L)
        }
        val vm = RecordViewModel(repo)
        vm.openDraft("d1")
        vm.onCaloriesChange("900")

        vm.save()

        assertEquals(1, repo.meals.size)
        val saved = repo.meals["d1"]!!
        assertEquals("d1", saved.id)
        assertEquals(RecordStatus.SAVED, saved.status)
        assertEquals(100L, saved.createdAt)
        assertEquals(900f, saved.totalCaloriesOverride)
        assertEquals(RecordViewModel.Step.ENTRIES, vm.step.value)
    }

    @Test
    fun `重新保存草稿复用UUID并保留createdAt`() = runTest {
        val repo = FakeMealRepository().apply {
            meals["d1"] = entity(id = "d1", name = "午餐", calories = null, status = RecordStatus.DRAFT, createdAt = 100L)
        }
        val vm = RecordViewModel(repo)
        vm.openDraft("d1")
        vm.onNoteChange("加辣")

        vm.saveDraft()

        assertEquals(1, repo.meals.size)
        val saved = repo.meals["d1"]!!
        assertEquals("d1", saved.id)
        assertEquals(RecordStatus.DRAFT, saved.status)
        assertEquals(100L, saved.createdAt)
        assertEquals("加辣", saved.note)
    }

    @Test
    fun `保存失败保留表单并保留脏标记`() = runTest {
        val repo = FakeMealRepository().apply { saveError = RuntimeException("boom") }
        val vm = RecordViewModel(repo)
        vm.openForm()
        vm.onNameChange("午餐")
        vm.onCaloriesChange("850")
        vm.proceedToConfirm()

        vm.save()

        assertEquals(RecordViewModel.Step.CONFIRM, vm.step.value)
        assertEquals("午餐", vm.form.value.name)
        assertTrue(vm.isDirty.value)
        assertFalse(vm.isSaving.value)
        assertTrue(repo.meals.isEmpty())
    }

    @Test
    fun `删除草稿调用仓库删除`() = runTest {
        val repo = FakeMealRepository().apply {
            meals["d1"] = entity(id = "d1", name = "草稿", status = RecordStatus.DRAFT)
        }
        val vm = RecordViewModel(repo)

        vm.deleteDraft("d1")

        assertTrue(repo.deletedIds.contains("d1"))
    }

    private fun entity(
        id: String,
        name: String?,
        calories: Float? = null,
        status: RecordStatus = RecordStatus.DRAFT,
        createdAt: Long = 0L,
    ): MealRecordEntity = MealRecordEntity(
        id = id,
        date = LocalDate.of(2026, 1, 1),
        mealType = MealType.LUNCH,
        name = name,
        time = null,
        note = null,
        photoUri = null,
        totalCaloriesOverride = calories,
        proteinOverride = null,
        carbsOverride = null,
        fatOverride = null,
        status = status,
        source = RecordSource.MANUAL_QUICK,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private class FakeMealRepository : MealRepository {
        val meals = mutableMapOf<String, MealRecordEntity>()
        val deletedIds = mutableListOf<String>()
        var saveError: Exception? = null

        override fun observeSavedMeals(date: LocalDate): Flow<List<MealRecordEntity>> =
            MutableStateFlow(meals.values.filter { it.status == RecordStatus.SAVED && it.date == date })

        override fun observeMealsByDate(date: LocalDate): Flow<List<MealRecordEntity>> =
            MutableStateFlow(meals.values.filter { it.date == date })

        override fun observeDrafts(): Flow<List<MealRecordEntity>> =
            MutableStateFlow(meals.values.filter { it.status == RecordStatus.DRAFT })

        override fun observeNutritionSummary(date: LocalDate): Flow<NutritionSummary> =
            MutableStateFlow(NutritionSummary(null, null, null, null, false, false))

        override suspend fun getMeal(id: String): MealRecordEntity? = meals[id]

        override suspend fun getMealWithItems(id: String): MealRecordWithItems? = null

        override suspend fun saveMeal(meal: MealRecordEntity, items: List<FoodItemEntity>) {
            saveError?.let { throw it }
            meals[meal.id] = meal
        }

        override suspend fun deleteMeal(id: String) {
            deletedIds += id
            meals.remove(id)
        }

        override suspend fun hasSavedMeal(date: LocalDate, mealType: MealType): Boolean =
            meals.values.any { it.status == RecordStatus.SAVED && it.date == date && it.mealType == mealType }
    }
}
