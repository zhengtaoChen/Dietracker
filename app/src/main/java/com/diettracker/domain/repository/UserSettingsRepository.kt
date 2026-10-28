package com.diettracker.domain.repository

import com.diettracker.domain.model.PersonalInfo
import kotlinx.coroutines.flow.Flow

/**
 * 用户个人资料与非敏感设置（DataStore 存储，PRD §3/§7）。
 * 个人资料用于自动估算热量目标，可随时修改或留空。
 */
interface UserSettingsRepository {

    val personalInfo: Flow<PersonalInfo>

    suspend fun savePersonalInfo(info: PersonalInfo)
}
