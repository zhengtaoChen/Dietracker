package com.diettracker.data.local.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room 类型转换器。仅 java.time 类型需要显式转换；
 * 枚举由 Room 默认以 name() 字符串落库，无需额外转换器。
 */
class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
}
