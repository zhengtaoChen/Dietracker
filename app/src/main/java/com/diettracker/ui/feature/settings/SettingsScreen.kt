package com.diettracker.ui.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diettracker.domain.model.PersonalInfo
import com.diettracker.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onEditGoal: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val info by viewModel.personalInfo.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
            SectionTitle("每日热量目标")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = goal?.let { "${Formatters.formatKcal(it.calories)} · ${Formatters.goalTypeLabel(it.goalType)}" }
                            ?: "未设置",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onEditGoal) { Text("修改目标") }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("个人资料")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = personalInfoSummary(info),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "用于自动估算热量目标，可随时修改。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onEditGoal) { Text("编辑") }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
}

private fun personalInfoSummary(info: PersonalInfo): String {
    val parts = listOfNotNull(
        info.sex?.label,
        info.ageYears?.let { "$it 岁" },
        info.heightCm?.let { "${trim(it)} cm" },
        info.weightKg?.let { "${trim(it)} kg" },
        info.activityLevel?.label,
    )
    return if (parts.isEmpty()) "未填写" else parts.joinToString(" · ")
}

private fun trim(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()
