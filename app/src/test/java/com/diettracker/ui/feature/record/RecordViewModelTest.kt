package com.diettracker.ui.feature.record

import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.data.local.db.entity.MealRecordWithItems
import com.diettracker.domain.model.FoodSource
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.NutritionCalculator
import com.diettracker.domain.model.NutritionSummary
import com.diettracker.domain.model.NutritionValues
import com.diettracker.domain.model.RawCooked
import com.diettracker.domain.model.RecordSource
import com.diettracker.domain.model.RecordStatus
import com.diettracker.domain.repository.FoodRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RecordViewModel 单元测试：验证草稿恢复、正式保存/重存复用 UUID、保留 createdAt、失败保留表单，
 * 以及详细模式的明细增删、克数折算、营养快照、手动覆盖与缺失值。
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

    // ---- 阶段 2A 回归：快速模式 ----

    @Test
    fun `打开草稿恢复表单内容`() = runTest {
        val repo = FakeMealRepository().apply {
            meals["d1"] = entity(id = "d1", name = "牛肉面", calories = 850f, status = RecordStatus.DRAFT, createdAt = 100L)
        }
        val vm = RecordViewModel(repo, FakeFoodRepository())

        vm.openMeal("d1")

        assertEquals("牛肉面", vm.form.value.name)
        assertEquals("850", vm.form.value.calories)
        assertEquals(RecordViewModel.Step.FORM, vm.step.value)
        assertFalse(vm.isDirty.value)
    }

    @Test
    fun `保存新草稿写入DRAFT并重置`() = runTest {
        val repo = FakeMealRepository()
        val vm = RecordViewModel(repo, FakeFoodRepository())
        vm.openQuickForm()
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
        val vm = RecordViewModel(repo, FakeFoodRepository())
        vm.openMeal("d1")
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
        val vm = RecordViewModel(repo, FakeFoodRepository())
        vm.openMeal("d1")
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
        val vm = RecordViewModel(repo, FakeFoodRepository())
        vm.openQuickForm()
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
        val vm = RecordViewModel(repo, FakeFoodRepository())

        vm.deleteDraft("d1")

        assertTrue(repo.deletedIds.contains("d1"))
    }

    // ---- 阶段 2B：详细模式 ----

    @Test
    fun `详细模式手动新增食物明细`() = runTest {
        val vm = RecordViewModel(FakeMealRepository(), FakeFoodRepository())
        vm.openDetailForm()
        vm.startManualFood()
        vm.onEditorNameChange("牛肉")
        vm.onEditorCaloriesChange("200")
        vm.addFoodItem()

        assertEquals(1, vm.form.value.items.size)
        val item = vm.form.value.items[0]
        assertEquals("牛肉", item.name)
        assertEquals(200f, item.calories)
        assertEquals(FoodSource.MANUAL, item.source)
        assertEquals(RecordViewModel.Step.FORM, vm.step.value)
    }

    @Test
    fun `详细模式本地库食物按克折算并保存快照`() = runTest {
        val foodRepo = FakeFoodRepository().apply {
            foods["f1"] = food(id = "f1", name = "米饭", kcal = 130f, protein = 2.7f, carbs = 28.2f, fat = 0.3f, version = 3)
        }
        val vm = RecordViewModel(FakeMealRepository(), foodRepo)
        vm.openDetailForm()
        vm.openFoodSearch()
        vm.selectFood(foodRepo.foods["f1"]!!)
        vm.onEditorGramsChange("150")
        vm.addFoodItem()

        val item = vm.form.value.items[0]
        assertEquals(195f, item.calories!!, 0.001f)
        assertEquals(4.05f, item.protein!!, 0.001f)
        assertEquals(42.3f, item.carbs!!, 0.001f)
        assertEquals(0.45f, item.fat!!, 0.001f)
        assertEquals("f1", item.foodDbId)
        assertEquals(3, item.foodDbVersion)
        assertEquals(FoodSource.LOCAL_DB, item.source)
    }

    @Test
    fun `删除食物后明细合计重新计算`() = runTest {
        val vm = RecordViewModel(FakeMealRepository(), FakeFoodRepository())
        vm.openDetailForm()
        // 第一条：热量 100
        vm.startManualFood()
        vm.onEditorNameChange("米饭")
        vm.onEditorCaloriesChange("100")
        vm.addFoodItem()
        // 第二条：热量 200
        vm.startManualFood()
        vm.onEditorNameChange("鸡胸肉")
        vm.onEditorCaloriesChange("200")
        vm.addFoodItem()

        assertEquals(300f, vm.detailSummary.value.calories!!, 0.001f)

        vm.deleteItem(vm.form.value.items[0].key)

        assertEquals(1, vm.form.value.items.size)
        assertEquals(200f, vm.detailSummary.value.calories!!, 0.001f)
    }

    @Test
    fun `详细模式手动覆盖总热量优先且保存明细`() = runTest {
        val repo = FakeMealRepository()
        val vm = RecordViewModel(repo, FakeFoodRepository())
        vm.openDetailForm()
        vm.startManualFood()
        vm.onEditorNameChange("牛肉面")
        vm.onEditorCaloriesChange("300")
        vm.addFoodItem()
        vm.onCaloriesChange("500")
        vm.proceedToConfirm()
        vm.save()

        assertEquals(1, repo.meals.size)
        val saved = repo.meals.values.first()
        assertEquals(RecordSource.MANUAL_DETAIL, saved.source)
        assertEquals(500f, saved.totalCaloriesOverride)
        assertEquals(1, repo.itemsByMeal[saved.id]?.size)
    }

    @Test
    fun `详细模式缺失营养保持null而非0`() = runTest {
        val repo = FakeMealRepository()
        val vm = RecordViewModel(repo, FakeFoodRepository())
        vm.openDetailForm()
        vm.startManualFood()
        vm.onEditorNameChange("某食物")
        vm.onEditorCaloriesChange("120")
        vm.addFoodItem()
        vm.proceedToConfirm()
        vm.save()

        val saved = repo.meals.values.first()
        val item = repo.itemsByMeal[saved.id]!!.first()
        assertEquals(120f, item.calories)
        assertNull(item.protein)
        assertNull(item.carbs)
        assertNull(item.fat)
    }

    @Test
    fun `详细草稿恢复含明细且进入详细模式`() = runTest {
        val repo = FakeMealRepository().apply {
            meals["d1"] = entity(id = "d1", name = null, calories = null, status = RecordStatus.DRAFT)
            itemsByMeal["d1"] = listOf(
                FoodItemEntity(
                    id = "i1", mealRecordId = "d1", sortOrder = 0, name = "米饭",
                    category = "主食", amount = 100f, amountUnit = "g",
                    calories = 130f, protein = 2.7f, carbs = 28.2f, fat = 0.3f,
                    source = FoodSource.LOCAL_DB, foodDbId = "f1", foodDbVersion = 1,
                ),
            )
        }
        val vm = RecordViewModel(repo, FakeFoodRepository())

        vm.openMeal("d1")

        assertEquals(RecordViewModel.InputMode.DETAIL, vm.mode.value)
        assertEquals(1, vm.form.value.items.size)
        assertEquals("米饭", vm.form.value.items[0].name)
        assertEquals(130f, vm.form.value.items[0].calories)
    }

    @Test
    fun `详细模式无食物时不能进入确认页`() = runTest {
        val vm = RecordViewModel(FakeMealRepository(), FakeFoodRepository())
        vm.openDetailForm()
        vm.proceedToConfirm()

        assertNotNull(vm.formError.value)
        assertEquals(RecordViewModel.Step.FORM, vm.step.value)
    }

    @Test
    fun `本地库食物克数非法时不能添加`() = runTest {
        val foodRepo = FakeFoodRepository().apply {
            foods["f1"] = food(id = "f1", name = "米饭", kcal = 130f)
        }
        val vm = RecordViewModel(FakeMealRepository(), foodRepo)
        vm.openDetailForm()
        vm.selectFood(foodRepo.foods["f1"]!!)
        vm.onEditorGramsChange("0")
        vm.addFoodItem()

        assertNotNull(vm.editorError.value)
        assertEquals(0, vm.form.value.items.size)
    }

    // ---- 阶段 2B 修复：编辑已保存餐次 ----

    @Test
    fun `编辑已保存餐次复用UUID并保留createdAt`() = runTest {
        val repo = FakeMealRepository().apply {
            meals["m1"] = entity(id = "m1", name = "午餐", calories = 850f, status = RecordStatus.SAVED, createdAt = 100L)
        }
        val vm = RecordViewModel(repo, FakeFoodRepository())
        vm.openMeal("m1")
        vm.onCaloriesChange("750")

        vm.save()

        assertEquals(1, repo.meals.size)
        val saved = repo.meals["m1"]!!
        assertEquals("m1", saved.id)
        assertEquals(RecordStatus.SAVED, saved.status)
        assertEquals(100L, saved.createdAt)
        assertEquals(750f, saved.totalCaloriesOverride)
        assertEquals(RecordViewModel.Step.ENTRIES, vm.step.value)
    }

    @Test
    fun `编辑详细餐次修改食物重量后重算营养`() = runTest {
        val foodRepo = FakeFoodRepository().apply {
            foods["f1"] = food(id = "f1", name = "米饭", kcal = 130f, protein = 2.7f, carbs = 28.2f, fat = 0.3f, version = 1)
        }
        val repo = FakeMealRepository().apply {
            meals["m1"] = entity(id = "m1", name = null, calories = null, status = RecordStatus.SAVED, createdAt = 100L)
                .copy(source = RecordSource.MANUAL_DETAIL)
            itemsByMeal["m1"] = listOf(
                FoodItemEntity(
                    id = "i1", mealRecordId = "m1", sortOrder = 0, name = "米饭",
                    category = "主食", amount = 100f, amountUnit = "g",
                    calories = 130f, protein = 2.7f, carbs = 28.2f, fat = 0.3f,
                    source = FoodSource.LOCAL_DB, foodDbId = "f1", foodDbVersion = 1,
                ),
            )
        }
        val vm = RecordViewModel(repo, foodRepo)
        vm.openMeal("m1")

        assertEquals(RecordViewModel.InputMode.DETAIL, vm.mode.value)
        assertEquals(130f, vm.detailSummary.value.calories!!, 0.001f)

        vm.editItem(vm.form.value.items[0].key)
        vm.onEditorGramsChange("200")
        vm.addFoodItem()

        assertEquals(260f, vm.detailSummary.value.calories!!, 0.001f)

        vm.proceedToConfirm()
        vm.save()

        val savedItem = repo.itemsByMeal["m1"]!!.first()
        assertEquals(260f, savedItem.calories!!, 0.001f)
        assertEquals(5.4f, savedItem.protein!!, 0.001f)
    }

    @Test
    fun `取消编辑不修改原数据`() = runTest {
        val repo = FakeMealRepository().apply {
            meals["m1"] = entity(id = "m1", name = "午餐", calories = 850f, status = RecordStatus.SAVED, createdAt = 100L)
        }
        val vm = RecordViewModel(repo, FakeFoodRepository())
        vm.openMeal("m1")
        vm.onCaloriesChange("999")

        vm.backToEntries()

        val unchanged = repo.meals["m1"]!!
        assertEquals(850f, unchanged.totalCaloriesOverride)
        assertEquals(100L, unchanged.createdAt)
        assertEquals(RecordStatus.SAVED, unchanged.status)
    }

    // ---- 工厂 ----

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

    private fun food(
        id: String,
        name: String,
        kcal: Float,
        protein: Float = 0f,
        carbs: Float = 0f,
        fat: Float = 0f,
        version: Int = 1,
    ): FoodDbEntity = FoodDbEntity(
        id = id,
        name = name,
        category = "测试",
        aliases = "",
        kcalPer100g = kcal,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
        rawCooked = RawCooked.RAW,
        source = "test",
        sourceId = null,
        version = version,
        isCustom = false,
        isFavorite = false,
        createdAt = 0L,
    )

    private class FakeMealRepository : MealRepository {
        val meals = mutableMapOf<String, MealRecordEntity>()
        val itemsByMeal = mutableMapOf<String, List<FoodItemEntity>>()
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

        override suspend fun getMealWithItems(id: String): MealRecordWithItems? =
            meals[id]?.let { MealRecordWithItems(it, itemsByMeal[id] ?: emptyList()) }

        override suspend fun saveMeal(meal: MealRecordEntity, items: List<FoodItemEntity>) {
            saveError?.let { throw it }
            meals[meal.id] = meal
            itemsByMeal[meal.id] = items
        }

        override suspend fun deleteMeal(id: String) {
            deletedIds += id
            meals.remove(id)
            itemsByMeal.remove(id)
        }

        override suspend fun hasSavedMeal(date: LocalDate, mealType: MealType): Boolean =
            meals.values.any { it.status == RecordStatus.SAVED && it.date == date && it.mealType == mealType }
    }

    private class FakeFoodRepository : FoodRepository {
        val foods = mutableMapOf<String, FoodDbEntity>()

        override fun search(query: String, limit: Int): Flow<List<FoodDbEntity>> =
            MutableStateFlow(
                foods.values.filter {
                    it.name.contains(query) || it.aliases.contains(query) || it.category.contains(query)
                },
            )

        override suspend fun getFood(id: String): FoodDbEntity? = foods[id]

        override suspend fun saveCustomFood(food: FoodDbEntity) {
            foods[food.id] = food
        }

        override suspend fun getNutrition(foodId: String, grams: Float): NutritionValues? =
            foods[foodId]?.let { NutritionCalculator.computeFromFood(it, grams) }

        override suspend fun foodCount(): Int = foods.size
    }
}
