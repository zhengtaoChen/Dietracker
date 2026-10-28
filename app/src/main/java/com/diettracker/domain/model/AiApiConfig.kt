package com.diettracker.domain.model

/**
 * AI API 配置的对外快照（PRD §5）。
 *
 * 非敏感字段（Base URL、文字/视觉模型 ID）明文落 DataStore；
 * API Key 永不进入本对象，仅以 [hasApiKey] 表示「是否已保存密钥」，
 * 密钥本体按需经 Keystore 解密读取，避免在内存/日志中长期持有明文。
 */
data class AiApiConfig(
    val baseUrl: String?,
    val textModelId: String?,
    val visionModelId: String?,
    val hasApiKey: Boolean,
) {
    val isConfigured: Boolean
        get() = !baseUrl.isNullOrBlank() && hasApiKey
}
