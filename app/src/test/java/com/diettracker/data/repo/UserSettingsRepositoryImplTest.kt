package com.diettracker.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.diettracker.domain.model.ActivityLevel
import com.diettracker.domain.model.PersonalInfo
import com.diettracker.domain.model.Sex
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserSettingsRepositoryImplTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = TestScope(dispatcher + Job())

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: UserSettingsRepositoryImpl

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = scope,
            produceFile = { File.createTempFile("test-settings", ".preferences_pb") },
        )
        repo = UserSettingsRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `保存并读取完整个人资料`() = scope.runTest {
        repo.savePersonalInfo(PersonalInfo(Sex.MALE, 30, 175f, 70f, ActivityLevel.MODERATE))

        val info = repo.personalInfo.first()
        assertEquals(Sex.MALE, info.sex)
        assertEquals(30, info.ageYears)
        assertEquals(175f, info.heightCm)
        assertEquals(70f, info.weightKg)
        assertEquals(ActivityLevel.MODERATE, info.activityLevel)
    }

    @Test
    fun `未填写时返回空资料`() = scope.runTest {
        val info = repo.personalInfo.first()
        assertNull(info.sex)
        assertNull(info.ageYears)
        assertNull(info.heightCm)
        assertNull(info.weightKg)
        assertNull(info.activityLevel)
    }

    @Test
    fun `覆盖保存可清除已填写字段`() = scope.runTest {
        repo.savePersonalInfo(PersonalInfo(Sex.FEMALE, 28, 160f, 55f, ActivityLevel.LIGHT))
        repo.savePersonalInfo(PersonalInfo())

        val info = repo.personalInfo.first()
        assertNull(info.sex)
        assertNull(info.ageYears)
        assertNull(info.heightCm)
        assertNull(info.weightKg)
        assertNull(info.activityLevel)
    }
}
