package com.diettracker.data.network

import com.diettracker.domain.model.ConnectionClassifier
import com.diettracker.domain.model.ConnectionTestStatus
import com.diettracker.domain.repository.AiConnectionTester
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 连接测试实现：向 `POST {baseUrl}/chat/completions` 发送一次最小化请求
 * （OpenAI Chat Completions 格式），根据 HTTP 状态码/响应体/异常分类结果。
 * 使用内置 HttpURLConnection，避免为单一测试请求引入额外网络依赖。
 */
@Singleton
class AiConnectionTesterImpl @Inject constructor() : AiConnectionTester {

    override suspend fun testChatCompletions(
        baseUrl: String,
        apiKey: String,
        modelId: String,
    ): ConnectionTestStatus = withContext(Dispatchers.IO) {
        try {
            val connection = openConnection(baseUrl, apiKey)
            try {
                connection.outputStream.use { it.write(requestBody(modelId).toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val body = readBody(connection, code)
                ConnectionClassifier.classify(code, body)
            } finally {
                connection.disconnect()
            }
        } catch (_: SocketTimeoutException) {
            ConnectionTestStatus.TIMEOUT
        } catch (_: IOException) {
            ConnectionTestStatus.NETWORK_ERROR
        }
    }

    private fun openConnection(baseUrl: String, apiKey: String): HttpURLConnection =
        (URL(ConnectionClassifier.chatCompletionsUrl(baseUrl)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

    private fun readBody(connection: HttpURLConnection, code: Int): String? {
        val stream = if (code >= 400) connection.errorStream else connection.inputStream
        return stream?.bufferedReader()?.use { it.readText() }
    }

    private fun requestBody(modelId: String): String =
        """{"model":${jsonString(modelId)},"messages":[{"role":"user","content":"ping"}],"max_tokens":1}"""

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
