package com.diettracker.ui.feature.record

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.FoodSource
import com.diettracker.domain.model.MealType
import com.diettracker.domain.model.NutritionValues
import com.diettracker.domain.model.RawCooked
import com.diettracker.ui.components.PhotoThumbnail
import com.diettracker.ui.util.Formatters
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onNavigateHome: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onDirtyChange: (Boolean) -> Unit = {},
    pendingNavigation: String? = null,
    onDiscardResolved: (Boolean) -> Unit = {},
    editMealId: String? = null,
    onEditMealHandled: () -> Unit = {},
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    val drafts by viewModel.drafts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val rawCookedFilter by viewModel.rawCookedFilter.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val editorError by viewModel.editorError.collectAsStateWithLifecycle()
    val editorPreview by viewModel.editorPreview.collectAsStateWithLifecycle()
    val detailSummary by viewModel.detailSummary.collectAsStateWithLifecycle()

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showPhotoEntryDialog by remember { mutableStateOf(false) }
    var draftPendingDelete by remember { mutableStateOf<MealRecordEntity?>(null) }

    val context = LocalContext.current
    var captureTarget by remember { mutableStateOf<File?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = captureTarget
        captureTarget = null
        if (success && file != null) viewModel.onPhotoCaptured("photos/${file.name}")
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onPhotoPicked)
    }
    val launchCamera = {
        val file = File(context.filesDir, "photos").apply { mkdirs() }
            .let { dir -> File(dir, "${UUID.randomUUID()}.jpg") }
        file.createNewFile()
        captureTarget = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        takePicture.launch(uri)
    }
    val launchGallery = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(isDirty) { onDirtyChange(isDirty) }

    LaunchedEffect(editMealId) {
        editMealId?.let {
            viewModel.openMeal(it)
            onEditMealHandled()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecordEvent.Saved -> {
                    onNavigateHome()
                    onShowMessage("已保存")
                }
                is RecordEvent.DraftSaved -> onShowMessage("草稿已保存")
                is RecordEvent.Error -> onShowMessage(event.message)
            }
        }
    }

    // 三种返回方式（顶部返回 / 系统返回 / 底部导航）统一走放弃确认。
    val requestLeave = {
        if (isDirty) showDiscardDialog = true else viewModel.backToEntries()
    }

    BackHandler(enabled = step == RecordViewModel.Step.FORM) { requestLeave() }
    BackHandler(enabled = step == RecordViewModel.Step.CONFIRM) { viewModel.backToForm() }
    BackHandler(enabled = step == RecordViewModel.Step.FOOD_SEARCH) { viewModel.closeSearch() }
    BackHandler(enabled = step == RecordViewModel.Step.FOOD_EDIT) { viewModel.cancelEditor() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (step) {
            RecordViewModel.Step.ENTRIES -> RecordEntries(
                drafts = drafts,
                onTextClick = { showModeDialog = true },
                onPhotoClick = { showPhotoEntryDialog = true },
                onDraftClick = viewModel::openMeal,
                onDraftDelete = { draftPendingDelete = it },
            )
            RecordViewModel.Step.FORM -> if (mode == RecordViewModel.InputMode.QUICK) {
                QuickRecordForm(
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
                    onCapturePhoto = launchCamera,
                    onPickPhoto = launchGallery,
                    onRemovePhoto = viewModel::removePhoto,
                    onBack = requestLeave,
                    onProceed = viewModel::proceedToConfirm,
                    onSaveDraft = viewModel::saveDraft,
                )
            } else {
                DetailRecordForm(
                    form = form,
                    detailSummary = detailSummary,
                    error = formError,
                    onNameChange = viewModel::onNameChange,
                    onCaloriesChange = viewModel::onCaloriesChange,
                    onNoteChange = viewModel::onNoteChange,
                    onDateChange = viewModel::onDateChange,
                    onTimeChange = viewModel::onTimeChange,
                    onMealTypeChange = viewModel::onMealTypeChange,
                    onAddFood = viewModel::openFoodSearch,
                    onEditItem = viewModel::editItem,
                    onDeleteItem = viewModel::deleteItem,
                    onCapturePhoto = launchCamera,
                    onPickPhoto = launchGallery,
                    onRemovePhoto = viewModel::removePhoto,
                    onBack = requestLeave,
                    onProceed = viewModel::proceedToConfirm,
                    onSaveDraft = viewModel::saveDraft,
                )
            }
            RecordViewModel.Step.CONFIRM -> ConfirmRecord(
                mode = mode,
                form = form,
                detailSummary = detailSummary,
                isSaving = isSaving,
                onBack = viewModel::backToForm,
                onSave = viewModel::save,
                onSaveDraft = viewModel::saveDraft,
            )
            RecordViewModel.Step.FOOD_SEARCH -> FoodSearchScreen(
                query = searchQuery,
                filter = rawCookedFilter,
                results = searchResults,
                onQueryChange = viewModel::onSearchQueryChange,
                onFilterChange = viewModel::onRawCookedFilterChange,
                onSelect = viewModel::selectFood,
                onManual = viewModel::startManualFood,
                onBack = viewModel::closeSearch,
            )
            RecordViewModel.Step.FOOD_EDIT -> FoodEditorScreen(
                editor = editor,
                error = editorError,
                preview = editorPreview,
                onNameChange = viewModel::onEditorNameChange,
                onGramsChange = viewModel::onEditorGramsChange,
                onCaloriesChange = viewModel::onEditorCaloriesChange,
                onProteinChange = viewModel::onEditorProteinChange,
                onCarbsChange = viewModel::onEditorCarbsChange,
                onFatChange = viewModel::onEditorFatChange,
                onAdd = viewModel::addFoodItem,
                onCancel = viewModel::cancelEditor,
            )
        }
    }

    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("选择输入方式") },
            text = { Text("快速输入适合只填名称与总热量；详细输入可逐条添加食物并自动计算营养。") },
            confirmButton = {
                TextButton(onClick = {
                    showModeDialog = false
                    viewModel.openDetailForm()
                }) { Text("详细输入") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showModeDialog = false
                    viewModel.openQuickForm()
                }) { Text("快速输入") }
            },
        )
    }

    if (showPhotoEntryDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoEntryDialog = false },
            title = { Text("拍照记录") },
            text = { Text("拍摄或选择一张照片，随后可手动补充名称与热量。") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoEntryDialog = false
                    viewModel.openQuickForm()
                    launchCamera()
                }) { Text("拍照") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoEntryDialog = false
                    viewModel.openQuickForm()
                    launchGallery()
                }) { Text("从相册选择") }
            },
        )
    }

    val discardVisible = showDiscardDialog || pendingNavigation != null
    if (discardVisible) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
                if (pendingNavigation != null) onDiscardResolved(false)
            },
            title = { Text("放弃修改？") },
            text = { Text("当前填写的内容尚未保存，离开后将丢失。") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.backToEntries()
                    if (pendingNavigation != null) onDiscardResolved(true)
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    if (pendingNavigation != null) onDiscardResolved(false)
                }) { Text("继续编辑") }
            },
        )
    }

    draftPendingDelete?.let { draft ->
        AlertDialog(
            onDismissRequest = { draftPendingDelete = null },
            title = { Text("删除草稿？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    draftPendingDelete = null
                    viewModel.deleteDraft(draft.id)
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { draftPendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun RecordEntries(
    drafts: List<MealRecordEntity>,
    onTextClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onDraftClick: (String) -> Unit,
    onDraftDelete: (MealRecordEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
            subtitle = "快速输入或详细逐条添加食物并计算营养",
            enabled = true,
            onClick = onTextClick,
        )
        Spacer(Modifier.height(12.dp))
        EntryCard(
            title = "拍照记录",
            subtitle = "拍摄或选择照片，再手动补充名称与热量",
            enabled = true,
            onClick = onPhotoClick,
        )
        Spacer(Modifier.height(12.dp))
        EntryCard(
            title = "快捷记录",
            subtitle = "从收藏或历史餐次快速复用（后续阶段实现）",
            enabled = false,
            onClick = {},
        )

        if (drafts.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "未完成草稿（${drafts.size}）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            drafts.forEach { draft ->
                DraftListItem(
                    draft = draft,
                    onClick = { onDraftClick(draft.id) },
                    onDelete = { onDraftDelete(draft) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DraftListItem(
    draft: MealRecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.name ?: Formatters.mealTypeLabel(draft.mealType),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        Formatters.formatDate(draft.date),
                        Formatters.mealTypeLabel(draft.mealType),
                        Formatters.formatUpdatedAt(draft.updatedAt),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = Formatters.formatKcal(draft.totalCaloriesOverride),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除草稿")
            }
        }
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
    onCapturePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onBack: () -> Unit,
    onProceed: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderRow(
            title = "文字记录 · 快速",
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

            MealTypeChips(selected = form.mealType, onSelect = onMealTypeChange)
            Spacer(Modifier.height(12.dp))

            DateAndTimeRow(
                date = form.date,
                time = form.time,
                onShowDatePicker = { showDatePicker = true },
                onShowTimePicker = { showTimePicker = true },
            )
            Spacer(Modifier.height(16.dp))

            PhotoSection(
                photoUri = form.photoUri,
                onCapture = onCapturePhoto,
                onPick = onPickPhoto,
                onRemove = onRemovePhoto,
            )
            Spacer(Modifier.height(16.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailRecordForm(
    form: MealFormState,
    detailSummary: DetailSummary,
    error: String?,
    onNameChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onAddFood: () -> Unit,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onCapturePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onBack: () -> Unit,
    onProceed: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderRow(
            title = "文字记录 · 详细",
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
                label = { Text("餐次名称（可选）") },
                placeholder = { Text("例如：午餐") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            MealTypeChips(selected = form.mealType, onSelect = onMealTypeChange)
            Spacer(Modifier.height(12.dp))

            DateAndTimeRow(
                date = form.date,
                time = form.time,
                onShowDatePicker = { showDatePicker = true },
                onShowTimePicker = { showTimePicker = true },
            )
            Spacer(Modifier.height(16.dp))

            PhotoSection(
                photoUri = form.photoUri,
                onCapture = onCapturePhoto,
                onPick = onPickPhoto,
                onRemove = onRemovePhoto,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "食物明细",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedButton(onClick = onAddFood) { Text("添加食物") }
            }
            Spacer(Modifier.height(4.dp))

            if (form.items.isEmpty()) {
                Text(
                    "尚未添加食物，点击「添加食物」从本地库选择或手动录入。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                form.items.forEach { item ->
                    ItemCard(
                        item = item,
                        onEdit = { onEditItem(item.key) },
                        onDelete = { onDeleteItem(item.key) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            // 明细合计 + 手动覆盖（覆盖优先参与统计，不与明细重复相加）。
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "明细合计",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    DetailRow("热量", Formatters.formatKcal(detailSummary.calories))
                    DetailRow("蛋白质", Formatters.formatGrams(detailSummary.protein))
                    DetailRow("碳水", Formatters.formatGrams(detailSummary.carbs))
                    DetailRow("脂肪", Formatters.formatGrams(detailSummary.fat))
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = form.calories,
                onValueChange = onCaloriesChange,
                label = { Text("总热量覆盖（可选，kcal）") },
                supportingText = { Text("填写后优先采用该值参与统计，不叠加明细热量") },
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
private fun ItemCard(
    item: FoodItemDraft,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = listOfNotNull(
                            item.category,
                            item.grams.takeIf { it.isNotBlank() }?.let { "$it g" },
                            foodSourceLabel(item.source),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑食物")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除食物")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutritionCell("热量", Formatters.formatKcal(item.calories))
                NutritionCell("蛋白质", Formatters.formatGrams(item.protein))
                NutritionCell("碳水", Formatters.formatGrams(item.carbs))
                NutritionCell("脂肪", Formatters.formatGrams(item.fat))
            }
        }
    }
}

@Composable
private fun NutritionCell(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoodSearchScreen(
    query: String,
    filter: RawCooked?,
    results: List<FoodDbEntity>,
    onQueryChange: (String) -> Unit,
    onFilterChange: (RawCooked?) -> Unit,
    onSelect: (FoodDbEntity) -> Unit,
    onManual: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderRow(title = "搜索食物", onBack = onBack, action = {})
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("搜索名称 / 别名 / 分类") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("全部") },
                )
                FilterChip(
                    selected = filter == RawCooked.RAW,
                    onClick = { onFilterChange(RawCooked.RAW) },
                    label = { Text("生") },
                )
                FilterChip(
                    selected = filter == RawCooked.COOKED,
                    onClick = { onFilterChange(RawCooked.COOKED) },
                    label = { Text("熟") },
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (results.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "未找到匹配食物",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results, key = { it.id }) { food ->
                    FoodResultRow(food = food, onClick = { onSelect(food) })
                }
            }
        }

        OutlinedButton(
            onClick = onManual,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) { Text("手动新增食物") }
    }
}

@Composable
private fun FoodResultRow(food: FoodDbEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOf(food.category, rawCookedLabel(food.rawCooked)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Formatters.formatKcal(food.kcalPer100g),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "每 100g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FoodEditorScreen(
    editor: FoodEditorState,
    error: String?,
    preview: NutritionValues?,
    onNameChange: (String) -> Unit,
    onGramsChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
) {
    val isLibrary = editor.food != null
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderRow(
            title = if (isLibrary) "食物明细" else "手动新增食物",
            onBack = onCancel,
            action = { TextButton(onClick = onAdd) { Text("添加") } },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (isLibrary) {
                val food = editor.food!!
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(food.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = listOf(food.category, rawCookedLabel(food.rawCooked)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "每 100g：${Formatters.formatKcal(food.kcalPer100g)} · " +
                                "蛋白质 ${Formatters.formatGrams(food.proteinPer100g)} · " +
                                "碳水 ${Formatters.formatGrams(food.carbsPer100g)} · " +
                                "脂肪 ${Formatters.formatGrams(food.fatPer100g)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = editor.grams,
                    onValueChange = onGramsChange,
                    label = { Text("重量（g）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (preview != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("按重量计算", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DetailRow("热量", Formatters.formatKcal(preview.calories))
                            DetailRow("蛋白质", Formatters.formatGrams(preview.protein))
                            DetailRow("碳水", Formatters.formatGrams(preview.carbs))
                            DetailRow("脂肪", Formatters.formatGrams(preview.fat))
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    label = { Text("食物名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = editor.grams,
                    onValueChange = onGramsChange,
                    label = { Text("重量（g，可选）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "营养数值（可选，未填写视为未知）",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editor.calories,
                        onValueChange = onCaloriesChange,
                        label = { Text("热量(kcal)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = editor.protein,
                        onValueChange = onProteinChange,
                        label = { Text("蛋白质(g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editor.carbs,
                        onValueChange = onCarbsChange,
                        label = { Text("碳水(g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = editor.fat,
                        onValueChange = onFatChange,
                        label = { Text("脂肪(g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ConfirmRecord(
    mode: RecordViewModel.InputMode,
    form: MealFormState,
    detailSummary: DetailSummary,
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
                    if (form.photoUri != null) {
                        PhotoThumbnail(
                            relativePath = form.photoUri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentDescription = "餐次照片",
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    if (mode == RecordViewModel.InputMode.QUICK) {
                        DetailRow("名称", form.name.trim().ifBlank { "—" })
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
                    } else {
                        DetailRow("名称", form.name.trim().ifBlank { "—" })
                        DetailRow("餐次类型", Formatters.mealTypeLabel(form.mealType))
                        DetailRow("日期", Formatters.formatDate(form.date))
                        DetailRow("时间", Formatters.formatTime(form.time))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            "食物明细",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        form.items.forEach { item ->
                            DetailRow(item.name, Formatters.formatKcal(item.calories))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("明细热量合计", Formatters.formatKcal(detailSummary.calories))
                        DetailRow("蛋白质", Formatters.formatGrams(detailSummary.protein))
                        DetailRow("碳水", Formatters.formatGrams(detailSummary.carbs))
                        DetailRow("脂肪", Formatters.formatGrams(detailSummary.fat))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow(
                            "总热量（覆盖）",
                            if (form.calories.isNotBlank()) {
                                "${Formatters.formatKcal(form.calories.toFloatOrNull())}（优先）"
                            } else {
                                "—（采用明细合计）"
                            },
                        )
                        if (form.note.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            DetailRow("备注", form.note.trim())
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (isSaving) "保存中…" else "保存") }
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
private fun MealTypeChips(selected: MealType, onSelect: (MealType) -> Unit) {
    Column {
        Text("餐次类型", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealType.entries.forEach { type ->
                FilterChip(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    label = { Text(Formatters.mealTypeLabel(type)) },
                )
            }
        }
    }
}

@Composable
private fun DateAndTimeRow(
    date: LocalDate,
    time: LocalTime,
    onShowDatePicker: () -> Unit,
    onShowTimePicker: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onShowDatePicker, modifier = Modifier.weight(1f)) {
            Text(Formatters.formatDate(date))
        }
        OutlinedButton(onClick = onShowTimePicker, modifier = Modifier.weight(1f)) {
            Text(Formatters.formatTime(time))
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

@Composable
private fun PhotoSection(
    photoUri: String?,
    onCapture: () -> Unit,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    var showChooser by remember { mutableStateOf(false) }
    Column {
        Text(
            "照片（可选）",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        if (photoUri != null) {
            PhotoThumbnail(
                relativePath = photoUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentDescription = "餐次照片",
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showChooser = true }) { Text("更换") }
                OutlinedButton(onClick = onRemove) { Text("移除") }
            }
        } else {
            OutlinedButton(onClick = { showChooser = true }, modifier = Modifier.fillMaxWidth()) {
                Text("添加照片")
            }
        }
    }

    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text("选择照片") },
            text = { Text("拍照或从相册选择一张照片。") },
            confirmButton = {
                TextButton(onClick = {
                    showChooser = false
                    onCapture()
                }) { Text("拍照") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChooser = false
                    onPick()
                }) { Text("从相册选择") }
            },
        )
    }
}

private fun rawCookedLabel(raw: RawCooked): String = if (raw == RawCooked.RAW) "生" else "熟"

private fun foodSourceLabel(source: FoodSource): String = when (source) {
    FoodSource.LOCAL_DB -> "本地库"
    FoodSource.MANUAL -> "手动"
    FoodSource.CUSTOM -> "自定义"
    FoodSource.AI -> "AI"
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
