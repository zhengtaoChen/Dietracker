package com.diettracker.ui.feature.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diettracker.domain.model.ActivityLevel
import com.diettracker.domain.model.CalorieEstimator
import com.diettracker.domain.model.GoalType
import com.diettracker.domain.model.PersonalInfo
import com.diettracker.domain.model.Sex
import com.diettracker.domain.repository.GoalRepository
import com.diettracker.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GoalSetupViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {

    enum class Method { AUTO, MANUAL }

    data class UiState(
        val method: Method? = null,
        val sex: Sex? = null,
        val ageText: String = "",
        val heightText: String = "",
        val weightText: String = "",
        val activityLevel: ActivityLevel? = null,
        val goalType: GoalType = GoalType.MAINTAIN,
        val targetCaloriesText: String = "",
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _saved = MutableSharedFlow<Unit>()
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    init {
        viewModelScope.launch {
            val info = userSettingsRepository.personalInfo.first()
            _state.update {
                it.copy(
                    sex = info.sex,
                    ageText = info.ageYears?.toString() ?: "",
                    heightText = info.heightCm?.let(::formatNumber) ?: "",
                    weightText = info.weightKg?.let(::formatNumber) ?: "",
                    activityLevel = info.activityLevel,
                )
            }
        }
    }

    fun selectMethod(method: Method) = _state.update { it.copy(method = method, error = null) }

    fun selectSex(sex: Sex) = _state.update { it.copy(sex = sex, error = null) }

    fun setAge(text: String) = _state.update { it.copy(ageText = text, error = null) }

    fun setHeight(text: String) = _state.update { it.copy(heightText = text, error = null) }

    fun setWeight(text: String) = _state.update { it.copy(weightText = text, error = null) }

    fun selectActivity(level: ActivityLevel) =
        _state.update { it.copy(activityLevel = level, error = null) }

    fun selectGoalType(type: GoalType) = _state.update { it.copy(goalType = type, error = null) }

    fun setTargetCalories(text: String) =
        _state.update { it.copy(targetCaloriesText = text, error = null) }

    fun computeSuggestion() {
        val s = _state.value
        val parsed = parsedInfo(s)
        if (parsed == null) return
        val suggested = CalorieEstimator.suggestedCalories(
            parsed.sex, parsed.age, parsed.height, parsed.weight, parsed.activity, s.goalType,
        )
        _state.update { it.copy(targetCaloriesText = suggested.roundToInt().toString(), error = null) }
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            when (s.method) {
                Method.AUTO -> saveAuto(s)
                Method.MANUAL -> saveManual(s)
                null -> Unit
            }
        }
    }

    private suspend fun saveAuto(s: UiState) {
        val parsed = parsedInfo(s)
        val target = s.targetCaloriesText.toFloatOrNull()?.takeIf { it > 0f }
        if (parsed == null || target == null) {
            if (target == null) setError("请输入有效的目标热量")
            return
        }
        userSettingsRepository.savePersonalInfo(
            PersonalInfo(
                sex = parsed.sex,
                ageYears = parsed.age,
                heightCm = parsed.height,
                weightKg = parsed.weight,
                activityLevel = parsed.activity,
            ),
        )
        goalRepository.saveGoal(
            date = LocalDate.now(),
            calories = target,
            goalType = s.goalType,
            estimateParams = CalorieEstimator.estimateParamsJson(
                parsed.sex, parsed.age, parsed.height, parsed.weight, parsed.activity,
            ),
        )
        _saved.emit(Unit)
    }

    private suspend fun saveManual(s: UiState) {
        val target = s.targetCaloriesText.toFloatOrNull()?.takeIf { it > 0f }
        if (target == null) {
            setError("请输入有效的目标热量")
            return
        }
        goalRepository.saveGoal(LocalDate.now(), target, GoalType.MANUAL, null)
        _saved.emit(Unit)
    }

    /** 解析并校验个人资料；任一字段非法则置错误并返回 null。 */
    private fun parsedInfo(s: UiState): ParsedInfo? {
        val sex = s.sex ?: return setError("请选择性别")
        val age = s.ageText.toIntOrNull()?.takeIf { it in CalorieEstimator.AGE_MIN..CalorieEstimator.AGE_MAX }
            ?: return setError("年龄需在 ${CalorieEstimator.AGE_MIN}-${CalorieEstimator.AGE_MAX} 岁之间")
        val height = s.heightText.toFloatOrNull()?.takeIf { it in CalorieEstimator.HEIGHT_MIN..CalorieEstimator.HEIGHT_MAX }
            ?: return setError("身高需在 ${CalorieEstimator.HEIGHT_MIN.toInt()}-${CalorieEstimator.HEIGHT_MAX.toInt()} cm 之间")
        val weight = s.weightText.toFloatOrNull()?.takeIf { it in CalorieEstimator.WEIGHT_MIN..CalorieEstimator.WEIGHT_MAX }
            ?: return setError("体重需在 ${CalorieEstimator.WEIGHT_MIN.toInt()}-${CalorieEstimator.WEIGHT_MAX.toInt()} kg 之间")
        val activity = s.activityLevel ?: return setError("请选择活动水平")
        setError(null)
        return ParsedInfo(sex, age, height, weight, activity)
    }

    private fun setError(message: String?): Nothing? {
        _state.update { it.copy(error = message) }
        return null
    }

    private data class ParsedInfo(
        val sex: Sex,
        val age: Int,
        val height: Float,
        val weight: Float,
        val activity: ActivityLevel,
    )

    private fun formatNumber(value: Float): String =
        if (value % 1f == 0f) value.toInt().toString() else value.toString()
}
