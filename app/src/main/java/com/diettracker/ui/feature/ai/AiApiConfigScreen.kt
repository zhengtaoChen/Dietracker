package com.diettracker.ui.feature.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diettracker.domain.model.ConnectionTestStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiApiConfigScreen(
    onBack: () -> Unit = {},
    viewModel: AiApiConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI API 配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::setBaseUrl,
                label = { Text("Base URL") },
                placeholder = { Text(AiApiConfigViewModel.DEFAULT_BASE_URL) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "需以 https:// 开头，默认 ${AiApiConfigViewModel.DEFAULT_BASE_URL}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.apiKeyText,
                onValueChange = viewModel::setApiKeyText,
                label = { Text("API Key") },
                placeholder = { Text(if (state.hasApiKey) "已保存（留空保持不变）" else "请输入 API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.textModelId,
                onValueChange = viewModel::setTextModel,
                label = { Text("文字模型 ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.visionModelId,
                onValueChange = viewModel::setVisionModel,
                label = { Text("视觉模型 ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("保存配置") }

            Spacer(Modifier.height(24.dp))
            Text(
                "连接测试",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            TestButton(
                label = "测试文字模型",
                testing = state.textTesting,
                result = state.textResult,
                onClick = viewModel::testText,
            )
            Spacer(Modifier.height(12.dp))
            TestButton(
                label = "测试视觉模型",
                testing = state.visionTesting,
                result = state.visionResult,
                onClick = viewModel::testVision,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "连接测试由你手动触发，将调用一次 API，可能产生少量费用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = { showClearDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("清除配置", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除 AI API 配置？") },
            text = { Text("将删除 Base URL、模型 ID 与已保存的 API Key，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearAll()
                }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TestButton(
    label: String,
    testing: Boolean,
    result: ConnectionTestStatus?,
    onClick: () -> Unit,
) {
    Column {
        OutlinedButton(onClick = onClick, enabled = !testing, modifier = Modifier.fillMaxWidth()) {
            if (testing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("测试中…")
            } else {
                Text(label)
            }
        }
        result?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                connectionTestMessage(it),
                style = MaterialTheme.typography.bodySmall,
                color = if (it == ConnectionTestStatus.SUCCESS) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

private fun connectionTestMessage(status: ConnectionTestStatus): String = when (status) {
    ConnectionTestStatus.SUCCESS -> "连接成功"
    ConnectionTestStatus.AUTH_FAILED -> "认证失败：请检查 API Key"
    ConnectionTestStatus.MODEL_NOT_FOUND -> "模型不存在：请检查模型 ID"
    ConnectionTestStatus.QUOTA_EXCEEDED -> "额度不足"
    ConnectionTestStatus.RATE_LIMITED -> "请求过于频繁（限流）"
    ConnectionTestStatus.TIMEOUT -> "连接超时"
    ConnectionTestStatus.NETWORK_ERROR -> "网络错误：请检查 Base URL 与网络连接"
    ConnectionTestStatus.UNKNOWN_ERROR -> "连接失败（未知错误）"
}
