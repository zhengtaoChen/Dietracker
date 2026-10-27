package com.diettracker.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.NutritionSummary
import com.diettracker.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    mealRepository: MealRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    /** 今日已保存的餐次（草稿不参与统计，PRD §13.1）。 */
    val meals: StateFlow<List<MealRecordEntity>> = mealRepository
        .observeSavedMeals(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 今日营养汇总（热量 + 三大营养素，缺失为 null）。 */
    val summary: StateFlow<NutritionSummary?> = mealRepository
        .observeNutritionSummary(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val date: LocalDate get() = today
}
