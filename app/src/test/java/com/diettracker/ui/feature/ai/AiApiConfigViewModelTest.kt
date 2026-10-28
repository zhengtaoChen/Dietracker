package com.diettracker.ui.feature.ai

import com.diettracker.domain.model.AiApiConfig
import com.diettracker.domain.model.ConnectionTestStatus
import com.diettracker.domain.repository.AiApiConfigRepository
import com.diettracker.domain.repository.AiConnectionTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiApiConfigViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val fakeRepo = FakeRepo()
    private val fakeTester = FakeTester()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `保存配置写入仓库并标记已有密钥`() = runBlocking {
        val vm = AiApiConfigViewModel(fakeRepo, fakeTester)
        vm.setBaseUrl("https://api.example.com/v1")
        vm.setTextModel("text-model")
        vm.setVisionModel("vision-model")
        vm.setApiKeyText("sk-test")

        val job = launch(Dispatchers.Unconfined) { vm.messages.collect {} }
        vm.save()
        job.cancel()

        assertEquals("https://api.example.com/v1", fakeRepo.savedBaseUrl)
        assertEquals("text-model", fakeRepo.savedTextModel)
        assertEquals("vision-model", fakeRepo.savedVisionModel)
        assertEquals("sk-test", fakeRepo.savedApiKey)
        assertTrue(vm.state.value.hasApiKey)
    }

    @Test
    fun `非 https Base URL 拒绝保存`() {
        val vm = AiApiConfigViewModel(fakeRepo, fakeTester)
        vm.setBaseUrl("http://insecure.example.com")
        vm.save()

        assertNotNull(vm.state.value.error)
        assertNull(fakeRepo.savedBaseUrl)
    }

    @Test
    fun `文字连接测试成功更新结果`() {
        fakeTester.result = ConnectionTestStatus.SUCCESS
        val vm = AiApiConfigViewModel(fakeRepo, fakeTester)
        vm.setBaseUrl("https://api.example.com/v1")
        vm.setTextModel("text-model")
        vm.setApiKeyText("sk-test")

        vm.testText()

        assertEquals(ConnectionTestStatus.SUCCESS, vm.state.value.textResult)
        assertEquals("text-model", fakeTester.lastModelId)
    }

    @Test
    fun `文字连接测试认证失败`() {
        fakeTester.result = ConnectionTestStatus.AUTH_FAILED
        val vm = AiApiConfigViewModel(fakeRepo, fakeTester)
        vm.setBaseUrl("https://api.example.com/v1")
        vm.setTextModel("text-model")
        vm.setApiKeyText("sk-test")

        vm.testText()

        assertEquals(ConnectionTestStatus.AUTH_FAILED, vm.state.value.textResult)
    }

    @Test
    fun `缺少模型 ID 不发起测试`() {
        val vm = AiApiConfigViewModel(fakeRepo, fakeTester)
        vm.setBaseUrl("https://api.example.com/v1")
        vm.setApiKeyText("sk-test")

        vm.testText()

        assertNotNull(vm.state.value.error)
        assertNull(fakeTester.lastModelId)
    }

    @Test
    fun `清除配置调用仓库并重置状态`() = runBlocking {
        fakeRepo.configFlow.value = AiApiConfig("https://api.example.com/v1", "text", "vision", true)
        val vm = AiApiConfigViewModel(fakeRepo, fakeTester)

        val job = launch(Dispatchers.Unconfined) { vm.messages.collect {} }
        vm.clearAll()
        job.cancel()

        assertTrue(fakeRepo.cleared)
        assertFalse(vm.state.value.hasApiKey)
        assertEquals("", vm.state.value.baseUrl)
        assertEquals("", vm.state.value.textModelId)
    }

    private class FakeRepo : AiApiConfigRepository {
        val configFlow = MutableStateFlow(AiApiConfig(null, null, null, false))
        override val config: Flow<AiApiConfig> = configFlow

        var savedBaseUrl: String? = null
        var savedTextModel: String? = null
        var savedVisionModel: String? = null
        var savedApiKey: String? = null
        var storedKey: String? = null
        var cleared = false

        override suspend fun saveConfig(baseUrl: String, textModelId: String, visionModelId: String) {
            savedBaseUrl = baseUrl
            savedTextModel = textModelId
            savedVisionModel = visionModelId
        }

        override suspend fun saveApiKey(apiKey: String) {
            savedApiKey = apiKey
            storedKey = apiKey
            configFlow.value = configFlow.value.copy(hasApiKey = true)
        }

        override suspend fun getApiKey(): String? = storedKey

        override suspend fun clearApiKey() {
            storedKey = null
            configFlow.value = configFlow.value.copy(hasApiKey = false)
        }

        override suspend fun clearAll() {
            cleared = true
            savedBaseUrl = null
            savedTextModel = null
            savedVisionModel = null
            storedKey = null
            configFlow.value = AiApiConfig(null, null, null, false)
        }
    }

    private class FakeTester : AiConnectionTester {
        var result: ConnectionTestStatus = ConnectionTestStatus.SUCCESS
        var lastModelId: String? = null

        override suspend fun testChatCompletions(
            baseUrl: String,
            apiKey: String,
            modelId: String,
        ): ConnectionTestStatus {
            lastModelId = modelId
            return result
        }
    }
}
