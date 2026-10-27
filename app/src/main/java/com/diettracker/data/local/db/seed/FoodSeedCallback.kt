package com.diettracker.data.local.db.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.diettracker.data.local.db.AppDatabase
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 数据库首次创建时写入内置食物种子数据。
 *
 * 通过 [Provider] 懒获取 [AppDatabase]，避免与数据库构建形成依赖环。
 * 种子插入使用 `IGNORE` 策略，配合稳定 ID 幂等，重复执行不会覆盖已有数据。
 */
@Singleton
class FoodSeedCallback @Inject constructor(
    private val databaseProvider: Provider<AppDatabase>,
) : RoomDatabase.Callback() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch {
            databaseProvider.get().foodDbDao().insertAll(FoodSeedData.all())
        }
    }
}
