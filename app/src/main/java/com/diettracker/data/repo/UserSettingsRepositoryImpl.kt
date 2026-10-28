package com.diettracker.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.diettracker.domain.model.ActivityLevel
import com.diettracker.domain.model.PersonalInfo
import com.diettracker.domain.model.Sex
import com.diettracker.domain.repository.UserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class UserSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserSettingsRepository {

    override val personalInfo: Flow<PersonalInfo> = dataStore.data.map { prefs ->
        PersonalInfo(
            sex = prefs[Keys.SEX]?.let { runCatching { Sex.valueOf(it) }.getOrNull() },
            ageYears = prefs[Keys.AGE],
            heightCm = prefs[Keys.HEIGHT],
            weightKg = prefs[Keys.WEIGHT],
            activityLevel = prefs[Keys.ACTIVITY]?.let {
                runCatching { ActivityLevel.valueOf(it) }.getOrNull()
            },
        )
    }

    override suspend fun savePersonalInfo(info: PersonalInfo) {
        dataStore.edit { prefs ->
            info.sex?.let { prefs[Keys.SEX] = it.name } ?: prefs.remove(Keys.SEX)
            info.ageYears?.let { prefs[Keys.AGE] = it } ?: prefs.remove(Keys.AGE)
            info.heightCm?.let { prefs[Keys.HEIGHT] = it } ?: prefs.remove(Keys.HEIGHT)
            info.weightKg?.let { prefs[Keys.WEIGHT] = it } ?: prefs.remove(Keys.WEIGHT)
            info.activityLevel?.let { prefs[Keys.ACTIVITY] = it.name } ?: prefs.remove(Keys.ACTIVITY)
        }
    }

    private object Keys {
        val SEX = stringPreferencesKey("sex")
        val AGE = intPreferencesKey("age_years")
        val HEIGHT = floatPreferencesKey("height_cm")
        val WEIGHT = floatPreferencesKey("weight_kg")
        val ACTIVITY = stringPreferencesKey("activity_level")
    }
}
