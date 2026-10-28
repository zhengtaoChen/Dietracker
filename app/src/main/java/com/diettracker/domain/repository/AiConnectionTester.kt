package com.diettracker.domain.repository

import com.diettracker.domain.model.ConnectionTestStatus

/**
 * AI 连接测试（PRD §5）。由用户主动触发，调用一次 Chat Completions 接口。
 */
interface AiConnectionTester {

    suspend fun testChatCompletions(
        baseUrl: String,
        apiKey: String,
        modelId: String,
    ): ConnectionTestStatus
}
