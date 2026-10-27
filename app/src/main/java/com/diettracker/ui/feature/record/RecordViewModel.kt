package com.diettracker.ui.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.data.local.db.entity.FoodItemEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.FoodSource
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.NutritionCalculator
import com.diettracker.domain.model.NutritionValues
import com.diettracker.domain.model.RawCooked
import com.diettracker.domain.model.RecordSource
import com.diettracker.domain.model.RecordStatus
import com.diettracker.domain.repository.FoodRepository
import com.diettracker.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private fun parseNonNegative(value: String): Float? =
    value.trim().toFloatOrNull()?.takeIf { it >= 0f }

private fun parsePositive(value: String): Float? =
    value.trim().toFloatOrNull()?.takeIf { it > 0f }

private fun Float.toInputString(): String =
    if (this % 1f == 0f) toInt().toString() else toString()

private fun sumOrNull(values: List<Float>): Float? =
    if (values.isEmpty()) null else values.sum()

/**
 * 详细模式的一条食物明细草稿。营养字段可空：缺失 ≠ 0（PRD §13.4）。
 *
 * 对 LOCAL_DB 条目，[calories]/[protein]/[carbs]/[fat] 为「按克折算后的快照」，
 * 连同 [foodDbId]/[foodDbVersion] 一起落库，保证营养库更新不回溯修改历史（PRD §13.7）。
 */
data class FoodItemDraft(
    val key: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String? = null,
    /** 重量(g)原始文本，校验时解析。 */
    val grams: String = "",
    val amountUnit: String = "g",
    val calories: Float? = null,
    val protein: Float? = null,
    val carbs: Float? = null,
    val fat: Float? = null,
    val source: FoodSource = FoodSource.LOCAL_DB,
    val foodDbId: String? = null,
    val foodDbVersion: Int? = null,
)

/** 食物编辑器（添加/修改单条明细时的临时状态）。[food] 为空表示手动录入（非本地库）。 */
data class FoodEditorState(
    val editingKey: String? = null,
    val food: FoodDbEntity? = null,
    val name: String = "",
    val grams: String = "",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
)

/** 详细模式明细合计（已知值求和，缺失项不计入；无已知值则为 null）。 */
data class DetailSummary(
    val calories: Float? = null,
    val protein: Float? = null,
    val carbs: Float? = null,
    val fat: Float? = null,
)

/** 快速录入表单状态。数值字段以原始文本保存，校验时再解析，便于输入框绑定。 */
data class MealFormState(
    val name: String = "",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val mealType: MealType = MealType.LUNCH,
    val note: String = "",
    val items: List<FoodItemDraft> = emptyList(),
) {
    /** 是否有任何已填内容（用于草稿门槛）。 */
    val hasContent: Boolean
        get() = listOf(name, calories, protein, carbs, fat, note).any { it.isNotBlank() } || items.isNotEmpty()

    val isCaloriesValid: Boolean
        get() = parsePositive(calories) != null

    val isNameValid: Boolean
        get() = name.isNotBlank()

    /** 每个营养字段：为空视为合法（保持 null），非空须为非负数字。 */
    val isMacrosValid: Boolean
        get() = listOf(protein, carbs, fat).all { it.isBlank() || it.toFloatOrNull()?.let { v -> v >= 0f } == true }
}

sealed interface RecordEvent {
    data object Saved : RecordEvent
    data object DraftSaved : RecordEvent
    data class Error(val message: String) : RecordEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {

    enum class Step { ENTRIES, FORM, CONFIRM, FOOD_SEARCH, FOOD_EDIT }

    /** 文字记录的两种输入模式（PRD §4.2）。 */
    enum class InputMode { QUICK, DETAIL }

    private val _step = MutableStateFlow(Step.ENTRIES)
    val step: StateFlow<Step> = _step.asStateFlow()

    private val _mode = MutableStateFlow(InputMode.QUICK)
    val mode: StateFlow<InputMode> = _mode.asStateFlow()

    private val _form = MutableStateFlow(MealFormState())
    val form: StateFlow<MealFormState> = _form.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** 当前表单相对打开时是否有未保存的修改（用于退出保护，区别于 hasContent）。 */
    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _events = MutableSharedFlow<RecordEvent>()
    val events: SharedFlow<RecordEvent> = _events.asSharedFlow()

    /** 未完成草稿（真实 Room 数据，按更新时间倒序）。 */
    val drafts: StateFlow<List<MealRecordEntity>> = mealRepository
        .observeDrafts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- 食物搜索 ----

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _rawCookedFilter = MutableStateFlow<RawCooked?>(null)
    val rawCookedFilter: StateFlow<RawCooked?> = _rawCookedFilter.asStateFlow()

    val searchResults: StateFlow<List<FoodDbEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else foodRepository.search(query)
        }
        .combine(_rawCookedFilter) { list, filter ->
            if (filter == null) list else list.filter { it.rawCooked == filter }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- 食物编辑器 ----

    private val _editor = MutableStateFlow(FoodEditorState())
    val editor: StateFlow<FoodEditorState> = _editor.asStateFlow()

    private val _editorError = MutableStateFlow<String?>(null)
    val editorError: StateFlow<String?> = _editorError.asStateFlow()

    /** 本地库食物按克数折算的实时预览（仅在食物非空且克数合法时非 null）。 */
    val editorPreview: StateFlow<NutritionValues?> = _editor
        .map { e ->
            val food = e.food ?: return@map null
            val grams = parsePositive(e.grams) ?: return@map null
            NutritionCalculator.computeFromFood(food, grams)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 详细模式明细营养合计（已知值求和，缺失为 null）。纯内存派生，使用 Eagerly 便于直接读取最新值。 */
    val detailSummary: StateFlow<DetailSummary> = _form
        .map { form ->
            DetailSummary(
                calories = sumOrNull(form.items.mapNotNull { it.calories }),
                protein = sumOrNull(form.items.mapNotNull { it.protein }),
                carbs = sumOrNull(form.items.mapNotNull { it.carbs }),
                fat = sumOrNull(form.items.mapNotNull { it.fat }),
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DetailSummary())

    /** 正在编辑的既有记录（草稿）的 UUID；null 表示新建记录。 */
    private var editingId: String? = null

    /** 正在编辑记录的原始 createdAt，正式保存/重存时保留。 */
    private var editingCreatedAt: Long? = null

    fun openQuickForm() = resetForm(InputMode.QUICK)

    fun openDetailForm() = resetForm(InputMode.DETAIL)

    private fun resetForm(mode: InputMode) {
        _mode.value = mode
        _form.value = MealFormState()
        _formError.value = null
        _isDirty.value = false
        editingId = null
        editingCreatedAt = null
        clearEditor()
        _step.value = Step.FORM
    }

    fun openMeal(id: String) {
        viewModelScope.launch {
            val withItems = mealRepository.getMealWithItems(id) ?: return@launch
            val entity = withItems.meal
            editingId = id
            editingCreatedAt = entity.createdAt
            _mode.value = if (withItems.items.isEmpty()) InputMode.QUICK else InputMode.DETAIL
            _form.value = entity.toFormState(withItems.items)
            _formError.value = null
            _isDirty.value = false
            clearEditor()
            _step.value = Step.FORM
        }
    }

    fun deleteDraft(id: String) {
        viewModelScope.launch {
            try {
                mealRepository.deleteMeal(id)
            } catch (e: Exception) {
                _events.tryEmit(RecordEvent.Error("删除草稿失败：${e.message ?: "未知错误"}"))
            }
        }
    }

    /** 放弃当前编辑并回到入口页（清空表单，避免残留内容）。 */
    fun backToEntries() {
        _form.value = MealFormState()
        _formError.value = null
        _isDirty.value = false
        editingId = null
        editingCreatedAt = null
        _mode.value = InputMode.QUICK
        clearEditor()
        _step.value = Step.ENTRIES
    }

    fun proceedToConfirm() {
        val error = validate()
        if (error == null) {
            _formError.value = null
            _step.value = Step.CONFIRM
        } else {
            _formError.value = error
        }
    }

    private fun validate(): String? = when (_mode.value) {
        InputMode.QUICK -> {
            val f = _form.value
            when {
                !f.isNameValid -> "请输入餐次名称"
                !f.isCaloriesValid -> "请输入有效的总热量（大于 0 的 kcal）"
                !f.isMacrosValid -> "营养数值无效，请填写非负数字或留空"
                else -> null
            }
        }
        InputMode.DETAIL -> {
            val f = _form.value
            when {
                f.items.isEmpty() -> "请至少添加一项食物"
                f.calories.isNotBlank() && parsePositive(f.calories) == null ->
                    "总热量覆盖值无效，请填写大于 0 的 kcal 或留空"
                else -> null
            }
        }
    }

    fun backToForm() {
        _step.value = Step.FORM
    }

    fun onNameChange(value: String) = update { it.copy(name = value) }
    fun onCaloriesChange(value: String) = update { it.copy(calories = value) }
    fun onProteinChange(value: String) = update { it.copy(protein = value) }
    fun onCarbsChange(value: String) = update { it.copy(carbs = value) }
    fun onFatChange(value: String) = update { it.copy(fat = value) }
    fun onNoteChange(value: String) = update { it.copy(note = value) }
    fun onDateChange(value: LocalDate) = update { it.copy(date = value) }
    fun onTimeChange(value: LocalTime) = update { it.copy(time = value) }
    fun onMealTypeChange(value: MealType) = update { it.copy(mealType = value) }

    private fun update(transform: (MealFormState) -> MealFormState) {
        _form.value = transform(_form.value)
        _formError.value = null
        _isDirty.value = true
    }

    // ---- 食物搜索 / 编辑 ----

    fun openFoodSearch() {
        _searchQuery.value = ""
        _rawCookedFilter.value = null
        clearEditor()
        _step.value = Step.FOOD_SEARCH
    }

    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
    }

    fun onRawCookedFilterChange(value: RawCooked?) {
        _rawCookedFilter.value = value
    }

    fun selectFood(food: FoodDbEntity) {
        _editor.value = FoodEditorState(food = food, name = food.name)
        _editorError.value = null
        _step.value = Step.FOOD_EDIT
    }

    fun startManualFood() {
        _editor.value = FoodEditorState()
        _editorError.value = null
        _step.value = Step.FOOD_EDIT
    }

    fun editItem(key: String) {
        val item = _form.value.items.find { it.key == key } ?: return
        viewModelScope.launch {
            val food = item.foodDbId?.let { foodRepository.getFood(it) }
            _editor.value = if (food != null) {
                FoodEditorState(editingKey = item.key, food = food, name = food.name, grams = item.grams)
            } else {
                FoodEditorState(
                    editingKey = item.key,
                    name = item.name,
                    grams = item.grams,
                    calories = item.calories?.toInputString().orEmpty(),
                    protein = item.protein?.toInputString().orEmpty(),
                    carbs = item.carbs?.toInputString().orEmpty(),
                    fat = item.fat?.toInputString().orEmpty(),
                )
            }
            _editorError.value = null
            _step.value = Step.FOOD_EDIT
        }
    }

    fun deleteItem(key: String) {
        _form.value = _form.value.copy(items = _form.value.items.filterNot { it.key == key })
        _formError.value = null
        _isDirty.value = true
    }

    fun onEditorNameChange(value: String) {
        _editor.value = _editor.value.copy(name = value)
        _editorError.value = null
    }

    fun onEditorGramsChange(value: String) {
        _editor.value = _editor.value.copy(grams = value)
        _editorError.value = null
    }

    fun onEditorCaloriesChange(value: String) {
        _editor.value = _editor.value.copy(calories = value)
        _editorError.value = null
    }

    fun onEditorProteinChange(value: String) {
        _editor.value = _editor.value.copy(protein = value)
        _editorError.value = null
    }

    fun onEditorCarbsChange(value: String) {
        _editor.value = _editor.value.copy(carbs = value)
        _editorError.value = null
    }

    fun onEditorFatChange(value: String) {
        _editor.value = _editor.value.copy(fat = value)
        _editorError.value = null
    }

    fun addFoodItem() {
        val e = _editor.value
        val draft = buildDraftFromEditor(e) ?: return
        val items = _form.value.items
        _form.value = _form.value.copy(
            items = if (e.editingKey != null) {
                items.map { if (it.key == e.editingKey) draft.copy(key = e.editingKey) else it }
            } else {
                items + draft
            },
        )
        _formError.value = null
        _isDirty.value = true
        clearEditor()
        _step.value = Step.FORM
    }

    private fun buildDraftFromEditor(e: FoodEditorState): FoodItemDraft? {
        val food = e.food
        return if (food != null) {
            val grams = parsePositive(e.grams)
            if (grams == null) {
                _editorError.value = "请输入有效的重量（克，大于 0）"
                return null
            }
            val n = NutritionCalculator.computeFromFood(food, grams)
            FoodItemDraft(
                name = food.name,
                category = food.category,
                grams = e.grams,
                calories = n.calories,
                protein = n.protein,
                carbs = n.carbs,
                fat = n.fat,
                source = FoodSource.LOCAL_DB,
                foodDbId = food.id,
                foodDbVersion = food.version,
            )
        } else {
            val name = e.name.trim()
            if (name.isBlank()) {
                _editorError.value = "请输入食物名称"
                return null
            }
            val grams = parsePositive(e.grams)
            if (e.grams.isNotBlank() && grams == null) {
                _editorError.value = "重量无效，请填写大于 0 的数字或留空"
                return null
            }
            val calories = parseNonNegative(e.calories)
            val protein = parseNonNegative(e.protein)
            val carbs = parseNonNegative(e.carbs)
            val fat = parseNonNegative(e.fat)
            val anyInvalidNutrition =
                (e.calories.isNotBlank() && calories == null) ||
                    (e.protein.isNotBlank() && protein == null) ||
                    (e.carbs.isNotBlank() && carbs == null) ||
                    (e.fat.isNotBlank() && fat == null)
            if (anyInvalidNutrition) {
                _editorError.value = "营养数值无效，请填写非负数字或留空"
                return null
            }
            if (grams == null && calories == null && protein == null && carbs == null && fat == null) {
                _editorError.value = "请至少填写重量或一项营养数值"
                return null
            }
            FoodItemDraft(
                name = name,
                category = null,
                grams = e.grams,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                source = FoodSource.MANUAL,
                foodDbId = null,
                foodDbVersion = null,
            )
        }
    }

    fun closeSearch() {
        clearEditor()
        _step.value = Step.FORM
    }

    fun cancelEditor() {
        clearEditor()
        _step.value = Step.FORM
    }

    private fun clearEditor() {
        _editor.value = FoodEditorState()
        _editorError.value = null
    }

    // ---- 保存 ----

    fun save() {
        if (_isSaving.value) return
        val built = buildMeal(RecordStatus.SAVED) ?: return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                mealRepository.saveMeal(built.meal, built.items)
                resetAfterSave()
                _events.tryEmit(RecordEvent.Saved)
            } catch (e: Exception) {
                _events.tryEmit(RecordEvent.Error("保存失败：${e.message ?: "未知错误"}"))
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun saveDraft() {
        if (_isSaving.value) return
        if (!_form.value.hasContent) return
        val built = buildMeal(RecordStatus.DRAFT) ?: return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                mealRepository.saveMeal(built.meal, built.items)
                resetAfterSave()
                _events.tryEmit(RecordEvent.DraftSaved)
            } catch (e: Exception) {
                _events.tryEmit(RecordEvent.Error("保存草稿失败：${e.message ?: "未知错误"}"))
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun resetAfterSave() {
        _form.value = MealFormState()
        _formError.value = null
        _isDirty.value = false
        editingId = null
        editingCreatedAt = null
        _mode.value = InputMode.QUICK
        clearEditor()
        _step.value = Step.ENTRIES
    }

    private data class BuiltMeal(val meal: MealRecordEntity, val items: List<FoodItemEntity>)

    private fun buildMeal(status: RecordStatus): BuiltMeal? {
        val state = _form.value
        val now = System.currentTimeMillis()
        val id = editingId ?: UUID.randomUUID().toString()

        return when (_mode.value) {
            InputMode.QUICK -> {
                val name = state.name.trim().ifBlank { null }
                val calories = parsePositive(state.calories)
                if (status == RecordStatus.SAVED && (name == null || calories == null)) return null
                BuiltMeal(
                    meal = MealRecordEntity(
                        id = id,
                        date = state.date,
                        mealType = state.mealType,
                        name = name,
                        time = state.time,
                        note = state.note.trim().ifBlank { null },
                        photoUri = null,
                        totalCaloriesOverride = calories,
                        proteinOverride = parseNonNegative(state.protein),
                        carbsOverride = parseNonNegative(state.carbs),
                        fatOverride = parseNonNegative(state.fat),
                        status = status,
                        source = RecordSource.MANUAL_QUICK,
                        createdAt = editingCreatedAt ?: now,
                        updatedAt = now,
                    ),
                    items = emptyList(),
                )
            }
            InputMode.DETAIL -> {
                if (status == RecordStatus.SAVED && state.items.isEmpty()) return null
                BuiltMeal(
                    meal = MealRecordEntity(
                        id = id,
                        date = state.date,
                        mealType = state.mealType,
                        name = state.name.trim().ifBlank { null },
                        time = state.time,
                        note = state.note.trim().ifBlank { null },
                        photoUri = null,
                        totalCaloriesOverride = parsePositive(state.calories),
                        proteinOverride = null,
                        carbsOverride = null,
                        fatOverride = null,
                        status = status,
                        source = RecordSource.MANUAL_DETAIL,
                        createdAt = editingCreatedAt ?: now,
                        updatedAt = now,
                    ),
                    items = state.items.mapIndexed { index, draft -> draft.toEntity(id, index) },
                )
            }
        }
    }

    private fun MealRecordEntity.toFormState(items: List<FoodItemEntity>): MealFormState = MealFormState(
        name = name.orEmpty(),
        calories = totalCaloriesOverride?.toInputString().orEmpty(),
        protein = proteinOverride?.toInputString().orEmpty(),
        carbs = carbsOverride?.toInputString().orEmpty(),
        fat = fatOverride?.toInputString().orEmpty(),
        date = date,
        time = time ?: LocalTime.now(),
        mealType = mealType,
        note = note.orEmpty(),
        items = items.map { it.toDraft() },
    )

    private fun FoodItemEntity.toDraft(): FoodItemDraft = FoodItemDraft(
        key = UUID.randomUUID().toString(),
        name = name,
        category = category,
        grams = amount?.toInputString().orEmpty(),
        amountUnit = amountUnit ?: "g",
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        source = source,
        foodDbId = foodDbId,
        foodDbVersion = foodDbVersion,
    )

    private fun FoodItemDraft.toEntity(mealRecordId: String, sortOrder: Int): FoodItemEntity = FoodItemEntity(
        id = UUID.randomUUID().toString(),
        mealRecordId = mealRecordId,
        sortOrder = sortOrder,
        name = name,
        category = category,
        amount = parsePositive(grams),
        amountUnit = amountUnit,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        source = source,
        foodDbId = foodDbId,
        foodDbVersion = foodDbVersion,
    )
}
