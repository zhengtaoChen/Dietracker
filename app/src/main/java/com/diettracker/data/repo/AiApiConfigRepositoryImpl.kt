package com.diettracker.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.diettracker.data.crypto.ApiKeyCrypto
import com.diettracker.domain.model.AiApiConfig
import com.diettracker.domain.repository.AiApiConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * AI API 配置仓储实现。
 *
 * 非敏感字段（Base URL、文字/视觉模型 ID）落 DataStore（ai_config）；
 * API Key 经 [ApiKeyCrypto] 加密后落 `secure_api_config` SharedPreferences，
 * 该文件已在 dataExtractionRules/fullBackupContent 中排除备份。
 */
@Singleton
class AiApiConfigRepositoryImpl @Inject constructor(
    @Named("ai_config") private val dataStore: DataStore<Preferences>,
    @ApplicationContext context: Context,
    private val crypto: ApiKeyCrypto,
) : AiApiConfigRepository {

    private val securePrefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    private val hasApiKey = MutableStateFlow(securePrefs.contains(KEY_API_KEY_CIPHERTEXT))

    override val config: Flow<AiApiConfig> = combine(
        dataStore.data.map { prefs ->
            Triple(
                prefs[Keys.BASE_URL],
                prefs[Keys.TEXT_MODEL],
                prefs[Keys.VISION_MODEL],
            )
        },
        hasApiKey,
    ) { (baseUrl, textModel, visionModel), hasKey ->
        AiApiConfig(
            baseUrl = baseUrl,
            textModelId = textModel,
            visionModelId = visionModel,
            hasApiKey = hasKey,
        )
    }

    override suspend fun saveConfig(baseUrl: String, textModelId: String, visionModelId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = baseUrl
            prefs[Keys.TEXT_MODEL] = textModelId
            prefs[Keys.VISION_MODEL] = visionModelId
        }
    }

    override suspend fun saveApiKey(apiKey: String) {
        val ciphertext = crypto.encrypt(apiKey) ?: return
        securePrefs.edit().putString(KEY_API_KEY_CIPHERTEXT, ciphertext).apply()
        hasApiKey.value = true
    }

    override suspend fun getApiKey(): String? =
        securePrefs.getString(KEY_API_KEY_CIPHERTEXT, null)?.let(crypto::decrypt)

    override suspend fun clearApiKey() {
        securePrefs.edit().remove(KEY_API_KEY_CIPHERTEXT).apply()
        hasApiKey.value = false
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
        clearApiKey()
    }

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val TEXT_MODEL = stringPreferencesKey("text_model_id")
        val VISION_MODEL = stringPreferencesKey("vision_model_id")
    }

    private companion object {
        const val SECURE_PREFS_NAME = "secure_api_config"
        const val KEY_API_KEY_CIPHERTEXT = "api_key_ciphertext"
    }
}
