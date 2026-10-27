package com.diettracker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * 体重记录。按天唯一（date 为主键），每天一条可编辑记录，不做插值或预测（PRD §10.4）。
 *
 * 说明：以日期为主键而非独立 UUID，是因为「每日期一条」是体重记录的自然键；
 * `@Upsert` 按主键去重，正好实现「同一天覆盖、不同天新增」。
 */
@Entity(tableName = "weight_records")
data class WeightRecordEntity(
    @PrimaryKey val date: LocalDate,
    val weightKg: Float,
    val note: String?,
    val updatedAt: Long,
)
