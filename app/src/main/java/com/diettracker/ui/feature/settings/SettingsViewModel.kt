package com.diettracker.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diettracker.data.local.db.entity.CalorieGoalEntity
import com.diettracker.domain.model.PersonalInfo
import com.diettracker.domain.repository.GoalRepository
import com.diettracker.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    goalRepository: GoalRepository,
    userSettingsRepository: UserSettingsRepository,
) : ViewModel() {

    val goal: StateFlow<CalorieGoalEntity?> = goalRepository
        .observeEffectiveGoal(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val personalInfo: StateFlow<PersonalInfo> = userSettingsRepository.personalInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalInfo())
}
