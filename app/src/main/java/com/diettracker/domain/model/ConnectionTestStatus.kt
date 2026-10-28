package com.diettracker.domain.model

/**
 * 连接测试结果分类（PRD §5 / 阶段 3A 验收）。
 * 需区分认证失败、模型不存在、额度不足、限流、超时与网络错误。
 */
enum class ConnectionTestStatus {
    SUCCESS,
    AUTH_FAILED,
    MODEL_NOT_FOUND,
    QUOTA_EXCEEDED,
    RATE_LIMITED,
    TIMEOUT,
    NETWORK_ERROR,
    UNKNOWN_ERROR,
}

/**
 * 将 HTTP 状态码 + 错误响应体映射为连接测试结果。
 * 纯函数，便于单元测试；网络层抛出的 SocketTimeout/IO 异常在调用方单独归为
 * [ConnectionTestStatus.TIMEOUT]/[ConnectionTestStatus.NETWORK_ERROR]。
 */
object ConnectionClassifier {

    private const val CHAT_COMPLETIONS_PATH = "chat/completions"

    /** 由 Base URL 拼出 Chat Completions 端点，去除尾部多余斜杠。 */
    fun chatCompletionsUrl(baseUrl: String): String =
        baseUrl.trimEnd('/') + "/" + CHAT_COMPLETIONS_PATH

    fun classify(httpCode: Int, errorBody: String?): ConnectionTestStatus {
        val body = errorBody.orEmpty().lowercase()
        return when {
            httpCode in 200..299 -> ConnectionTestStatus.SUCCESS
            httpCode == 401 || httpCode == 403 -> ConnectionTestStatus.AUTH_FAILED
            httpCode == 404 -> ConnectionTestStatus.MODEL_NOT_FOUND
            httpCode == 429 -> classifyRateLimited(body)
            httpCode == 400 || httpCode == 422 -> classifyBadRequest(body)
            httpCode == 408 || httpCode == 504 -> ConnectionTestStatus.TIMEOUT
            else -> ConnectionTestStatus.UNKNOWN_ERROR
        }
    }

    private fun classifyRateLimited(body: String): ConnectionTestStatus =
        if ("insufficient_quota" in body || "insufficient quota" in body || "quota" in body) {
            ConnectionTestStatus.QUOTA_EXCEEDED
        } else {
            ConnectionTestStatus.RATE_LIMITED
        }

    private fun classifyBadRequest(body: String): ConnectionTestStatus =
        if ("model_not_found" in body || "model not found" in body || "deploymentnotfound" in body) {
            ConnectionTestStatus.MODEL_NOT_FOUND
        } else {
            ConnectionTestStatus.UNKNOWN_ERROR
        }
}
