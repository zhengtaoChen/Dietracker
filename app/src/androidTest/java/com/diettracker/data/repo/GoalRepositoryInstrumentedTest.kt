package com.diettracker.data.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diettracker.data.local.db.AppDatabase
import com.diettracker.domain.model.GoalType
import com.diettracker.testutil.InstrumentedTestDb
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 仪器化测试：验证热量目标在真实 SQLite 上的持久化与历史生效（PRD §13.6）。
 */
@RunWith(AndroidJUnit4::class)
class GoalRepositoryInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "goal-repo-test.db"

    private lateinit var db: AppDatabase
    private lateinit var repo: GoalRepositoryImpl

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        db = InstrumentedTestDb.fileDb(context, dbName)
        repo = GoalRepositoryImpl(db.calorieGoalDao())
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun 目标重启后仍生效() = runBlocking {
        repo.saveGoal(LocalDate.of(2026, 1, 1), 1800f, GoalType.MANUAL, null)

        db.close()
        db = InstrumentedTestDb.fileDb(context, dbName)
        repo = GoalRepositoryImpl(db.calorieGoalDao())

        assertEquals(1800f, repo.getEffectiveGoal(LocalDate.of(2026, 1, 10))?.calories)
    }

    @Test
    fun 修改目标后历史保持正确() = runBlocking {
        repo.saveGoal(LocalDate.of(2026, 1, 1), 1800f, GoalType.MANUAL, null)
        repo.saveGoal(LocalDate.of(2026, 1, 15), 1600f, GoalType.LOSE, null)

        assertEquals(1800f, repo.getEffectiveGoal(LocalDate.of(2026, 1, 10))?.calories)
        assertEquals(1600f, repo.getEffectiveGoal(LocalDate.of(2026, 1, 20))?.calories)
    }
}
