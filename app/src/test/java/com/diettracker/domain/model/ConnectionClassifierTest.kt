package com.diettracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionClassifierTest {

    @Test
    fun `2xx 判定成功`() {
        assertEquals(ConnectionTestStatus.SUCCESS, ConnectionClassifier.classify(200, null))
        assertEquals(ConnectionTestStatus.SUCCESS, ConnectionClassifier.classify(201, null))
    }

    @Test
    fun `401 与 403 判定认证失败`() {
        assertEquals(ConnectionTestStatus.AUTH_FAILED, ConnectionClassifier.classify(401, null))
        assertEquals(ConnectionTestStatus.AUTH_FAILED, ConnectionClassifier.classify(403, null))
    }

    @Test
    fun `404 判定模型不存在`() {
        assertEquals(ConnectionTestStatus.MODEL_NOT_FOUND, ConnectionClassifier.classify(404, null))
    }

    @Test
    fun `429 区分额度不足与限流`() {
        assertEquals(
            ConnectionTestStatus.QUOTA_EXCEEDED,
            ConnectionClassifier.classify(429, """{"error":{"type":"insufficient_quota"}}"""),
        )
        assertEquals(
            ConnectionTestStatus.RATE_LIMITED,
            ConnectionClassifier.classify(429, """{"error":{"type":"rate_limit_exceeded"}}"""),
        )
    }

    @Test
    fun `400 带 model_not_found 判定模型不存在`() {
        assertEquals(
            ConnectionTestStatus.MODEL_NOT_FOUND,
            ConnectionClassifier.classify(400, """{"error":{"message":"model not found"}}"""),
        )
        assertEquals(ConnectionTestStatus.UNKNOWN_ERROR, ConnectionClassifier.classify(400, null))
    }

    @Test
    fun `超时状态码判定超时`() {
        assertEquals(ConnectionTestStatus.TIMEOUT, ConnectionClassifier.classify(408, null))
        assertEquals(ConnectionTestStatus.TIMEOUT, ConnectionClassifier.classify(504, null))
    }

    @Test
    fun `其余状态码判定未知错误`() {
        assertEquals(ConnectionTestStatus.UNKNOWN_ERROR, ConnectionClassifier.classify(500, null))
        assertEquals(ConnectionTestStatus.UNKNOWN_ERROR, ConnectionClassifier.classify(418, null))
    }

    @Test
    fun `端点拼接去除尾部斜杠`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            ConnectionClassifier.chatCompletionsUrl("https://api.openai.com/v1"),
        )
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            ConnectionClassifier.chatCompletionsUrl("https://api.openai.com/v1/"),
        )
    }
}
