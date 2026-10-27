package com.diettracker.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库迁移集合。
 *
 * 一律使用「追加式」迁移（ALTER TABLE ADD COLUMN），保留既有数据；
 * 禁止使用 fallbackToDestructiveMigration（PRD §12.4 要求升级不丢失真实用户数据）。
 */
object Migrations {

    /**
     * v1 → v2：为快速模式（PRD §4.2）补充餐次级别的手动营养字段。
     * 新增三列均为可空 REAL，历史记录这三列保持 NULL（缺失 ≠ 0）。
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `meal_records` ADD COLUMN `proteinOverride` REAL")
            db.execSQL("ALTER TABLE `meal_records` ADD COLUMN `carbsOverride` REAL")
            db.execSQL("ALTER TABLE `meal_records` ADD COLUMN `fatOverride` REAL")
        }
    }
}
