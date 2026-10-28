package com.diettracker.data.repo

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diettracker.domain.model.ActivityLevel
import com.diettracker.domain.model.PersonalInfo
import com.diettracker.domain.model.Sex
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 仪器化测试：验证个人资料经 DataStore 落盘后，重新创建实例（模拟重启）仍可读取。
 */
@RunWith(AndroidJUnit4::class)
class UserSettingsRepositoryInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun 个人资料重启后仍存在() = runBlocking {
        val file = File(context.filesDir, "test-settings-${UUID.randomUUID()}.preferences_pb")

        val job1 = SupervisorJob()
        val scope1 = CoroutineScope(job1 + Dispatchers.IO)
        val repo1 = UserSettingsRepositoryImpl(
            PreferenceDataStoreFactory.create(
                corruptionHandler = null,
                migrations = emptyList(),
                scope = scope1,
                produceFile = { file },
            ),
        )
        repo1.savePersonalInfo(PersonalInfo(Sex.FEMALE, 28, 160f, 55f, ActivityLevel.LIGHT))
        job1.cancel()
        job1.join()

        val job2 = SupervisorJob()
        val scope2 = CoroutineScope(job2 + Dispatchers.IO)
        val repo2 = UserSettingsRepositoryImpl(
            PreferenceDataStoreFactory.create(
                corruptionHandler = null,
                migrations = emptyList(),
                scope = scope2,
                produceFile = { file },
            ),
        )
        val info = repo2.personalInfo.first()
        assertEquals(Sex.FEMALE, info.sex)
        assertEquals(28, info.ageYears)
        assertEquals(160f, info.heightCm)
        assertEquals(55f, info.weightKg)
        assertEquals(ActivityLevel.LIGHT, info.activityLevel)
        job2.cancel()
    }
}
