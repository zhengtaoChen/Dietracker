package com.diettracker.testutil

import android.content.Context
import androidx.room.Room
import com.diettracker.data.local.db.AppDatabase

/**
 * 仪器化测试专用数据库构建器。
 *
 * 与单元测试的 [TestDb]（inMemory）不同，仪器化测试需要验证「真实 Android SQLite」
 * 上的行为，包括关闭数据库后重新打开的数据持久化，因此这里提供基于文件名的构建方式。
 * 注意：不使用 FoodSeedCallback，测试数据由各测试自行写入，避免种子数据干扰断言。
 */
object InstrumentedTestDb {

    fun fileDb(context: Context, name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()
}
