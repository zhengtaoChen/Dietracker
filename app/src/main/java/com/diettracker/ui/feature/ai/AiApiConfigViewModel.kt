package com.diettracker.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diettracker.domain.model.ConnectionTestStatus
import com.diettracker.domain.repository.AiApiConfigRepository
import com.diettracker.domain.repository.AiConnectionTester
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
class AiApiConfigViewModel @Inject constructor(
    private val repository: AiApiConfigRepository,
    private val tester: AiConnectionTester,
) : ViewModel() {

    data class UiState(
        val baseUrl: String = "",
        val textModelId: String = "",
        val visionModelId: String = "",
        val apiKeyText: String = "",
        val hasApiKey: Boolean = false,
        val textTesting: Boolean = false,
        val visionTesting: Boolean = false,
        val textResult: ConnectionTestStatus? = null,
        val visionResult: ConnectionTestStatus? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            val config = repository.config.first()
            _state.update {
                it.copy(
                    baseUrl = config.baseUrl ?: "",
                    textModelId = config.textModelId ?: "",
                    visionModelId = config.visionModelId ?: "",
                    hasApiKey = config.hasApiKey,
                )
            }
        }
    }

    fun setBaseUrl(text: String) = _state.update { it.copy(baseUrl = text, error = null) }

    fun setTextModel(text: String) = _state.update { it.copy(textModelId = text, error = null) }

    fun setVisionModel(text: String) = _state.update { it.copy(visionModelId = text, error = null) }

    fun setApiKeyText(text: String) = _state.update { it.copy(apiKeyText = text, error = null) }

    fun save() {
        val s = _state.value
        val baseUrl = normalizedBaseUrl(s.baseUrl)
        if (!baseUrl.startsWith("https://")) {
            setError("Base URL 必须以 https:// 开头")
            return
        }
        viewModelScope.launch {
            repository.saveConfig(baseUrl, s.textModelId.trim(), s.visionModelId.trim())
            val key = s.apiKeyText.trim()
            if (key.isNotEmpty()) repository.saveApiKey(key)
            _state.update {
                it.copy(apiKeyText = "", hasApiKey = it.hasApiKey || key.isNotEmpty(), error = null)
            }
            _messages.emit("AI API 配置已保存")
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            _state.update {
                it.copy(
                    baseUrl = "",
                    textModelId = "",
                    visionModelId = "",
                    apiKeyText = "",
                    hasApiKey = false,
                    textResult = null,
                    visionResult = null,
                    error = null,
                )
            }
            _messages.emit("已清除 AI API 配置")
        }
    }

    fun testText() = runConnectionTest(modelKind = "文字", model = _state.value.textModelId) { s, status ->
        s.copy(textTesting = false, textResult = status)
    }

    fun testVision() = runConnectionTest(modelKind = "视觉", model = _state.value.visionModelId) { s, status ->
        s.copy(visionTesting = false, visionResult = status)
    }

    private fun runConnectionTest(
        modelKind: String,
        model: String,
        finish: (UiState, ConnectionTestStatus) -> UiState,
    ) {
        val s = _state.value
        val alreadyRunning = if (modelKind == "文字") s.textTesting else s.visionTesting
        if (alreadyRunning) return

        val baseUrl = normalizedBaseUrl(s.baseUrl)
        if (!baseUrl.startsWith("https://")) {
            setError("Base URL 必须以 https:// 开头")
            return
        }
        val modelId = model.trim()
        if (modelId.isEmpty()) {
            setError("请先填写${modelKind}模型 ID")
            return
        }
        setError(null)

        viewModelScope.launch {
            val typedKey = s.apiKeyText.trim()
            val key = typedKey.ifEmpty { repository.getApiKey().orEmpty() }
            if (key.isEmpty()) {
                setError("请先填写 API Key")
                return@launch
            }

            _state.update { if (modelKind == "文字") it.copy(textTesting = true, textResult = null) else it.copy(visionTesting = true, visionResult = null) }
            val status = tester.testChatCompletions(baseUrl, key, modelId)
            _state.update { finish(it, status) }
        }
    }

    private fun setError(message: String?) = _state.update { it.copy(error = message) }

    private fun normalizedBaseUrl(raw: String): String =
        raw.trim().ifBlank { DEFAULT_BASE_URL }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
    }
}
