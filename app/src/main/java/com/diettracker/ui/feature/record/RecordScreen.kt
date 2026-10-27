package com.diettracker.ui.feature.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diettracker.domain.model.MealType
import com.diettracker.ui.util.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onNavigateHome: () -> Unit = {},
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecordEvent.Saved -> {
                    snackbarHostState.showSnackbar("已保存")
                    onNavigateHome()
                }
                is RecordEvent.DraftSaved -> snackbarHostState.showSnackbar("草稿已保存")
                is RecordEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    BackHandler(enabled = step == RecordViewModel.Step.FORM) {
        if (form.hasContent) showDiscardDialog = true else viewModel.backToEntries()
    }
    BackHandler(enabled = step == RecordViewModel.Step.CONFIRM) {
        viewModel.backToForm()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (step) {
                RecordViewModel.Step.ENTRIES -> RecordEntries(onTextClick = viewModel::openForm)
                RecordViewModel.Step.FORM -> QuickRecordForm(
                    form = form,
                    error = formError,
                    onNameChange = viewModel::onNameChange,
                    onCaloriesChange = viewModel::onCaloriesChange,
                    onProteinChange = viewModel::onProteinChange,
                    onCarbsChange = viewModel::onCarbsChange,
                    onFatChange = viewModel::onFatChange,
                    onNoteChange = viewModel::onNoteChange,
                    onDateChange = viewModel::onDateChange,
                    onTimeChange = viewModel::onTimeChange,
                    onMealTypeChange = viewModel::onMealTypeChange,
                    onBack = viewModel::backToEntries,
                    onProceed = viewModel::proceedToConfirm,
                    onSaveDraft = viewModel::saveDraft,
                )
                RecordViewModel.Step.CONFIRM -> ConfirmRecord(
                    form = form,
                    isSaving = isSaving,
                    onBack = viewModel::backToForm,
                    onSave = viewModel::save,
                    onSaveDraft = viewModel::saveDraft,
                )
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃修改？") },
            text = { Text("当前填写的内容尚未保存，离开后将丢失。") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.backToEntries()
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("继续编辑") }
            },
        )
    }
}

@Composable
private fun RecordEntries(onTextClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("记录饮食", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "选择一种方式，记录你的一餐",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        EntryCard(
            title = "文字记录",
            subtitle = "手动填写餐次名称与热量，可补充营养素",
            enabled = true,
            onClick = onTextClick,
        )
        Spacer(Modifier.height(12.dp))
        EntryCard(
            title = "拍照记录",
            subtitle = "拍摄或选择照片识别（阶段 2B 实现）",
            enabled = false,
            onClick = {},
        )
        Spacer(Modifier.height(12.dp))
        EntryCard(
            title = "快捷记录",
            subtitle = "从收藏或历史餐次快速复用（阶段 2B 实现）",
            enabled = false,
            onClick = {},
        )
    }
}

@Composable
private fun EntryCard(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickRecordForm(
    form: MealFormState,
    error: String?,
    onNameChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onBack: () -> Unit,
    onProceed: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderRow(
            title = "文字记录",
            onBack = onBack,
            action = { TextButton(onClick = onProceed) { Text("下一步") } },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChange,
                label = { Text("餐次名称") },
                placeholder = { Text("例如：午餐、牛肉面") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.calories,
                onValueChange = onCaloriesChange,
                label = { Text("总热量（kcal）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            Text("餐次类型", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealType.entries.forEach { type ->
                    FilterChip(
                        selected = form.mealType == type,
                        onClick = { onMealTypeChange(type) },
                        label = { Text(Formatters.mealTypeLabel(type)) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(Formatters.formatDate(form.date))
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(Formatters.formatTime(form.time))
                }
            }
            Spacer(Modifier.height(12.dp))

            Text(
                "营养素（可选，未填写视为未知）",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.protein,
                    onValueChange = onProteinChange,
                    label = { Text("蛋白质(g)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.carbs,
                    onValueChange = onCarbsChange,
                    label = { Text("碳水(g)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = form.fat,
                onValueChange = onFatChange,
                label = { Text("脂肪(g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = form.note,
                onValueChange = onNoteChange,
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSaveDraft,
                enabled = form.hasContent,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存为草稿") }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.date.toUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateChange(it.toLocalDate()) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = form.time.hour,
            initialMinute = form.time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

@Composable
private fun ConfirmRecord(
    form: MealFormState,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderRow(
            title = "确认记录",
            onBack = onBack,
            action = {},
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow("名称", form.name.trim())
                    DetailRow("餐次类型", Formatters.mealTypeLabel(form.mealType))
                    DetailRow("日期", Formatters.formatDate(form.date))
                    DetailRow("时间", Formatters.formatTime(form.time))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow("总热量", Formatters.formatKcal(form.calories.toFloatOrNull()))
                    DetailRow("蛋白质", Formatters.formatGrams(form.protein.toFloatOrNull()))
                    DetailRow("碳水", Formatters.formatGrams(form.carbs.toFloatOrNull()))
                    DetailRow("脂肪", Formatters.formatGrams(form.fat.toFloatOrNull()))
                    if (form.note.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("备注", form.note.trim())
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSaveDraft,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存为草稿") }
        }
    }
}

@Composable
private fun HeaderRow(
    title: String,
    onBack: () -> Unit,
    action: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        action()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
