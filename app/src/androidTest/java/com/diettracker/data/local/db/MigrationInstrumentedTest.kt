package com.diettracker.data.local.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 在真实 Android SQLite 上验证 v1 → v2 迁移（[Migrations.MIGRATION_1_2]）。
 *
 * 直接构造 v1 表结构、写入历史数据，再以 v2 打开并执行真实迁移对象，
 * 断言新增三列存在且历史数据未被破坏。避免依赖 room-testing（其
 * kotlinx-serialization 版本与本项目存在冲突）。
 */
@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {

    private val v1CreateMealRecords = """
        CREATE TABLE IF NOT EXISTS `meal_records` (
            `id` TEXT NOT NULL,
            `date` TEXT NOT NULL,
            `mealType` TEXT NOT NULL,
            `name` TEXT,
            `time` TEXT,
            `note` TEXT,
            `photoUri` TEXT,
            `totalCaloriesOverride` REAL,
            `status` TEXT NOT NULL,
            `source` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
    """.trimIndent()

    @Test
    fun migrate1To2_addsMacroOverrideColumns_preservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-verify.db"
        context.deleteDatabase(dbName)

        // 1. 以 v1 结构建库并写入一条历史记录
        val v1Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(v1CreateMealRecords)
                        db.execSQL(
                            "INSERT INTO `meal_records` " +
                                "(`id`,`date`,`mealType`,`name`,`status`,`source`,`createdAt`,`updatedAt`) " +
                                "VALUES ('id-1','2026-10-27','LUNCH','牛肉面','SAVED','MANUAL_QUICK',1,2)",
                        )
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                })
                .build(),
        )
        v1Helper.writableDatabase.close()
        v1Helper.close()

        // 2. 以 v2 打开并执行真实迁移对象
        val v2Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) {
                        Migrations.MIGRATION_1_2.migrate(db)
                    }
                })
                .build(),
        )
        val db = v2Helper.readableDatabase

        // 3. 断言新增三列存在且历史数据保留
        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info(`meal_records`)").use { cursor ->
            while (cursor.moveToNext()) {
                columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }
        }
        assertTrue("缺少 proteinOverride 列", "proteinOverride" in columns)
        assertTrue("缺少 carbsOverride 列", "carbsOverride" in columns)
        assertTrue("缺少 fatOverride 列", "fatOverride" in columns)

        db.query("SELECT `name`,`status` FROM `meal_records` WHERE `id`='id-1'").use { cursor ->
            assertTrue("迁移后历史记录丢失", cursor.moveToFirst())
            assertEquals("牛肉面", cursor.getString(0))
            assertEquals("SAVED", cursor.getString(1))
        }

        v2Helper.close()
        context.deleteDatabase(dbName)
    }
}
