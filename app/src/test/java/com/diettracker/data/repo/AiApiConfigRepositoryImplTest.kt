package com.diettracker.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.diettracker.data.crypto.ApiKeyCrypto
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiApiConfigRepositoryImplTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = TestScope(dispatcher + Job())

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: AiApiConfigRepositoryImpl

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = scope,
            produceFile = { File.createTempFile("test-ai-config", ".preferences_pb") },
        )
        repo = AiApiConfigRepositoryImpl(dataStore, context, FakeCrypto())
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `保存配置后读取非敏感字段`() = scope.runTest {
        repo.saveConfig("https://api.example.com/v1", "text-model", "vision-model")

        val config = repo.config.first()
        assertEquals("https://api.example.com/v1", config.baseUrl)
        assertEquals("text-model", config.textModelId)
        assertEquals("vision-model", config.visionModelId)
        assertFalse(config.hasApiKey)
    }

    @Test
    fun `API Key 加密落盘且可解密读取`() = scope.runTest {
        repo.saveApiKey("sk-secret-123")

        assertEquals("sk-secret-123", repo.getApiKey())
        assertTrue(repo.config.first().hasApiKey)

        val stored = context.getSharedPreferences("secure_api_config", Context.MODE_PRIVATE)
            .getString("api_key_ciphertext", null)
        assertEquals("enc:sk-secret-123", stored)
    }

    @Test
    fun `清除 API Key 后不可读取`() = scope.runTest {
        repo.saveApiKey("sk-secret-123")
        repo.clearApiKey()

        assertNull(repo.getApiKey())
        assertFalse(repo.config.first().hasApiKey)
    }

    @Test
    fun `清除全部配置后字段清空`() = scope.runTest {
        repo.saveConfig("https://api.example.com/v1", "text-model", "vision-model")
        repo.saveApiKey("sk-secret-123")
        repo.clearAll()

        val config = repo.config.first()
        assertNull(config.baseUrl)
        assertNull(config.textModelId)
        assertNull(config.visionModelId)
        assertFalse(config.hasApiKey)
    }

    private class FakeCrypto : ApiKeyCrypto {
        override fun encrypt(plaintext: String): String? = "enc:$plaintext"

        override fun decrypt(encoded: String): String? = encoded.removePrefix("enc:")
    }
}
