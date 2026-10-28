package com.diettracker.ui.feature.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diettracker.domain.model.ActivityLevel
import com.diettracker.domain.model.GoalType
import com.diettracker.domain.model.Sex
import com.diettracker.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSetupScreen(
    onBack: () -> Unit = {},
    viewModel: GoalSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("每日热量目标") },
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
            when (state.method) {
                null -> MethodChooser(
                    onAuto = { viewModel.selectMethod(GoalSetupViewModel.Method.AUTO) },
                    onManual = { viewModel.selectMethod(GoalSetupViewModel.Method.MANUAL) },
                    onSkip = onBack,
                )
                GoalSetupViewModel.Method.AUTO -> AutoForm(state, viewModel)
                GoalSetupViewModel.Method.MANUAL -> ManualForm(state, viewModel)
            }
        }
    }
}

@Composable
private fun MethodChooser(onAuto: () -> Unit, onManual: () -> Unit, onSkip: () -> Unit) {
    Text("如何设置你的每日热量目标？", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    ChoiceCard(
        title = "自动估算",
        subtitle = "填写年龄、身高、体重与活动水平，根据公式计算参考值",
        onClick = onAuto,
    )
    Spacer(Modifier.height(12.dp))
    ChoiceCard(
        title = "手动设置",
        subtitle = "直接输入一个目标热量",
        onClick = onManual,
    )
    Spacer(Modifier.height(12.dp))
    ChoiceCard(
        title = "暂时跳过",
        subtitle = "之后可在设置中随时设置",
        onClick = onSkip,
    )
}

@Composable
private fun ChoiceCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AutoForm(state: GoalSetupViewModel.UiState, viewModel: GoalSetupViewModel) {
    Text("自动估算", style = MaterialTheme.typography.titleMedium)

    Spacer(Modifier.height(16.dp))
    Text("性别", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Sex.entries.forEach { sex ->
            FilterChip(
                selected = state.sex == sex,
                onClick = { viewModel.selectSex(sex) },
                label = { Text(sex.label) },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    NumberField("年龄（岁）", state.ageText, viewModel::setAge)
    Spacer(Modifier.height(12.dp))
    NumberField("身高（cm）", state.heightText, viewModel::setHeight)
    Spacer(Modifier.height(12.dp))
    NumberField("体重（kg）", state.weightText, viewModel::setWeight)

    Spacer(Modifier.height(16.dp))
    Text("活动水平", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActivityLevel.entries.forEach { level ->
            FilterChip(
                selected = state.activityLevel == level,
                onClick = { viewModel.selectActivity(level) },
                label = { Text(level.label) },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("目标", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalType.entries.filter { it != GoalType.MANUAL }.forEach { type ->
            FilterChip(
                selected = state.goalType == type,
                onClick = { viewModel.selectGoalType(type) },
                label = { Text(Formatters.goalTypeLabel(type)) },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = viewModel::computeSuggestion, modifier = Modifier.fillMaxWidth()) {
        Text("计算建议热量")
    }

    Spacer(Modifier.height(16.dp))
    NumberField("目标热量（kcal）", state.targetCaloriesText, viewModel::setTargetCalories)
    Spacer(Modifier.height(4.dp))
    Text(
        "估算结果仅作参考，不作为医学建议。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    state.error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }

    Spacer(Modifier.height(16.dp))
    Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("保存目标") }
}

@Composable
private fun ManualForm(state: GoalSetupViewModel.UiState, viewModel: GoalSetupViewModel) {
    Text("手动设置", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    NumberField("目标热量（kcal）", state.targetCaloriesText, viewModel::setTargetCalories)

    state.error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }

    Spacer(Modifier.height(16.dp))
    Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("保存目标") }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
