package com.diettracker.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diettracker.data.local.db.entity.CalorieGoalEntity
import com.diettracker.data.local.db.entity.MealRecordEntity
import com.diettracker.domain.model.NutritionSummary
import com.diettracker.domain.model.computeHomeProgress
import com.diettracker.ui.components.PhotoThumbnail
import com.diettracker.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    onAddRecord: () -> Unit = {},
    onEditMeal: (String) -> Unit = {},
    onOpenGoalSetup: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val date by viewModel.date.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()

    var mealPendingDelete by remember { mutableStateOf<MealRecordEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { onShowMessage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Formatters.formatDate(date)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecord) {
                Icon(Icons.Filled.Add, contentDescription = "记录饮食")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            SummaryHeader(
                summary = summary,
                goal = goal,
                onOpenGoalSetup = onOpenGoalSetup,
            )
            Spacer(Modifier.height(16.dp))
            if (meals.isEmpty()) {
                EmptyState(onAddRecord = onAddRecord)
            } else {
                Text("今日餐次", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(meals, key = { it.id }) { meal ->
                        MealListItem(
                            meal = meal,
                            onEdit = { onEditMeal(meal.id) },
                            onDelete = { mealPendingDelete = meal },
                        )
                    }
                }
            }
        }
    }

    mealPendingDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { mealPendingDelete = null },
            title = { Text("删除餐次？") },
            text = { Text("将删除「${meal.name ?: Formatters.mealTypeLabel(meal.mealType)}」及其食物明细，删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    mealPendingDelete = null
                    viewModel.deleteMeal(meal.id)
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { mealPendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SummaryHeader(
    summary: NutritionSummary?,
    goal: CalorieGoalEntity?,
    onOpenGoalSetup: () -> Unit,
) {
    val progress = computeHomeProgress(summary, goal)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "今日摄入",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = Formatters.formatKcal(progress.consumedCalories),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(12.dp))

            if (goal == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "未设置每日目标",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onOpenGoalSetup) { Text("设置目标") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "目标 ${Formatters.formatKcal(goal.calories)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val remaining = progress.remainingCalories
                    when {
                        remaining == null -> Text("—", style = MaterialTheme.typography.bodyMedium)
                        remaining < 0f -> Text(
                            text = "超出 ${Formatters.formatKcal(-remaining)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        else -> Text(
                            text = "剩余 ${Formatters.formatKcal(remaining)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progressFraction ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progress.isOverGoal) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroCell("蛋白质", summary?.protein)
                MacroCell("碳水", summary?.carbs)
                MacroCell("脂肪", summary?.fat)
            }
        }
    }
}

@Composable
private fun MacroCell(label: String, value: Float?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = Formatters.formatGrams(value),
            style = MaterialTheme.typography.titleMedium,
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
private fun MealListItem(
    meal: MealRecordEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (meal.photoUri != null) {
                PhotoThumbnail(
                    relativePath = meal.photoUri,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentDescription = "餐次照片",
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name ?: Formatters.mealTypeLabel(meal.mealType),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        Formatters.mealTypeLabel(meal.mealType),
                        meal.time?.let(Formatters::formatTime),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = Formatters.formatKcal(meal.totalCaloriesOverride),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑餐次")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除餐次")
            }
        }
    }
}

@Composable
private fun EmptyState(onAddRecord: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "今天还没有饮食记录",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "点击右下角按钮，记录你的第一餐",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
