package com.diettracker.ui.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.RecordSource
import com.diettracker.domain.model.RecordStatus
import com.diettracker.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private fun parseNonNegative(value: String): Float? =
    value.trim().toFloatOrNull()?.takeIf { it >= 0f }

private fun parsePositive(value: String): Float? =
    value.trim().toFloatOrNull()?.takeIf { it > 0f }

private fun Float.toInputString(): String =
    if (this % 1f == 0f) toInt().toString() else toString()

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
) {
    /** 是否有任何已填内容（用于草稿门槛）。 */
    val hasContent: Boolean
        get() = listOf(name, calories, protein, carbs, fat, note).any { it.isNotBlank() }

    val isCaloriesValid: Boolean
        get() = parsePositive(calories) != null

    val isNameValid: Boolean
        get() = name.isNotBlank()

    /** 每个营养字段：为空视为合法（保持 null），非空须为非负数字。 */
    val isMacrosValid: Boolean
        get() = listOf(protein, carbs, fat).all { it.isBlank() || it.toFloatOrNull()?.let { v -> v >= 0f } == true }

    val canConfirm: Boolean
        get() = isNameValid && isCaloriesValid && isMacrosValid
}

sealed interface RecordEvent {
    data object Saved : RecordEvent
    data object DraftSaved : RecordEvent
    data class Error(val message: String) : RecordEvent
}

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val mealRepository: MealRepository,
) : ViewModel() {

    enum class Step { ENTRIES, FORM, CONFIRM }

    private val _step = MutableStateFlow(Step.ENTRIES)
    val step: StateFlow<Step> = _step.asStateFlow()

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

    /** 正在编辑的既有记录（草稿）的 UUID；null 表示新建记录。 */
    private var editingId: String? = null

    /** 正在编辑记录的原始 createdAt，正式保存/重存时保留。 */
    private var editingCreatedAt: Long? = null

    fun openForm() {
        _form.value = MealFormState()
        _formError.value = null
        _isDirty.value = false
        editingId = null
        editingCreatedAt = null
        _step.value = Step.FORM
    }

    fun openDraft(id: String) {
        viewModelScope.launch {
            val entity = mealRepository.getMeal(id) ?: return@launch
            editingId = id
            editingCreatedAt = entity.createdAt
            _form.value = entity.toFormState()
            _formError.value = null
            _isDirty.value = false
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
        _step.value = Step.ENTRIES
    }

    fun proceedToConfirm() {
        if (_form.value.canConfirm) {
            _formError.value = null
            _step.value = Step.CONFIRM
        } else {
            _formError.value = when {
                !_form.value.isNameValid -> "请输入餐次名称"
                !_form.value.isCaloriesValid -> "请输入有效的总热量（大于 0 的 kcal）"
                else -> "营养数值无效，请填写非负数字或留空"
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

    fun save() {
        if (_isSaving.value) return
        val entity = buildEntity(RecordStatus.SAVED) ?: return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                mealRepository.saveMeal(entity, emptyList())
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
        val entity = buildEntity(RecordStatus.DRAFT) ?: return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                mealRepository.saveMeal(entity, emptyList())
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
        _step.value = Step.ENTRIES
    }

    private fun MealRecordEntity.toFormState(): MealFormState = MealFormState(
        name = name.orEmpty(),
        calories = totalCaloriesOverride?.toInputString().orEmpty(),
        protein = proteinOverride?.toInputString().orEmpty(),
        carbs = carbsOverride?.toInputString().orEmpty(),
        fat = fatOverride?.toInputString().orEmpty(),
        date = date,
        time = time ?: LocalTime.now(),
        mealType = mealType,
        note = note.orEmpty(),
    )

    private fun buildEntity(status: RecordStatus): MealRecordEntity? {
        val state = _form.value
        val name = state.name.trim().ifBlank { null }
        val calories = parsePositive(state.calories)
        if (status == RecordStatus.SAVED && (name == null || calories == null)) return null

        val now = System.currentTimeMillis()
        return MealRecordEntity(
            id = editingId ?: UUID.randomUUID().toString(),
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
        )
    }
}
