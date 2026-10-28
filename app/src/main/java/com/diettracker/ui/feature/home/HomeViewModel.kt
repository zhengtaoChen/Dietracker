package com.diettracker.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diettracker.data.local.db.entity.CalorieGoalEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.NutritionSummary
import com.diettracker.domain.repository.GoalRepository
import com.diettracker.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val currentDate = MutableStateFlow(LocalDate.now())

    /** 当前日期，跨午夜自动更新，避免首页停留前一天。 */
    val date: StateFlow<LocalDate> = currentDate

    /** 今日已保存的餐次（草稿不参与统计，PRD §13.1）。 */
    val meals: StateFlow<List<MealRecordEntity>> = currentDate
        .flatMapLatest { mealRepository.observeSavedMeals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 今日营养汇总（热量 + 三大营养素，缺失为 null）。 */
    val summary: StateFlow<NutritionSummary?> = currentDate
        .flatMapLatest { mealRepository.observeNutritionSummary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 今日生效的热量目标（无则为 null）。 */
    val goal: StateFlow<CalorieGoalEntity?> = currentDate
        .flatMapLatest { goalRepository.observeEffectiveGoal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                currentDate.value = LocalDate.now()
                delay(60_000)
            }
        }
    }

    fun deleteMeal(id: String) {
        viewModelScope.launch {
            try {
                mealRepository.deleteMeal(id)
            } catch (e: Exception) {
                _messages.emit("删除失败：${e.message ?: "未知错误"}")
            }
        }
    }
}
