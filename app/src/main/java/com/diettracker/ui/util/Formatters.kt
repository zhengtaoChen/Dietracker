package com.diettracker.ui.util

import com.diettracker.domain.model.MealType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 展示用的格式化工具。缺失营养统一显示「—」而非 0（PRD §13.4）。 */
object Formatters {

    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.SIMPLIFIED_CHINESE)

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatTime(time: LocalTime): String = time.format(timeFormatter)

    /** 草稿列表等场景展示的更新时间（本地时区）。 */
    fun formatUpdatedAt(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(dateTimeFormatter)

    fun mealTypeLabel(type: MealType): String = when (type) {
        MealType.BREAKFAST -> "早餐"
        MealType.LUNCH -> "午餐"
        MealType.DINNER -> "晚餐"
        MealType.SNACK -> "加餐"
    }

    fun formatKcal(value: Float?): String = value?.let { "${formatNumber(it)} kcal" } ?: "—"

    fun formatGrams(value: Float?): String = value?.let { "${formatNumber(it)} g" } ?: "—"

    private fun formatNumber(value: Float): String =
        if (value % 1f == 0f) value.toInt().toString() else value.toString()
}
