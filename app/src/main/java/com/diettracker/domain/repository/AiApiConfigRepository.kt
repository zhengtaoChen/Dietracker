package com.diettracker.domain.repository

import com.diettracker.domain.model.AiApiConfig
import kotlinx.coroutines.flow.Flow

/**
 * AI API 配置仓储（PRD §5 / 阶段 3A）。
 *
 * 非敏感字段（Base URL、文字/视觉模型 ID）落 DataStore；
 * API Key 经 Android Keystore 加密后落排除备份的 SharedPreferences，不落 DataStore。
 */
interface AiApiConfigRepository {

    val config: Flow<AiApiConfig>

    suspend fun saveConfig(baseUrl: String, textModelId: String, visionModelId: String)

    suspend fun saveApiKey(apiKey: String)

    /** 按需解密读取 API Key，调用方用完即弃，避免长期持有明文。 */
    suspend fun getApiKey(): String?

    suspend fun clearApiKey()

    suspend fun clearAll()
}
