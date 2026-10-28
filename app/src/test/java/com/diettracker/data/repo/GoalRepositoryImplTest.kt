package com.diettracker.data.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.diettracker.data.local.db.AppDatabase
import com.diettracker.domain.model.GoalType
import com.diettracker.testutil.TestDb
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoalRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: GoalRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = TestDb.inMemory(context)
        repo = GoalRepositoryImpl(db.calorieGoalDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `保存目标后当日生效`() = runBlocking {
        repo.saveGoal(LocalDate.of(2026, 1, 1), 1800f, GoalType.MANUAL, null)

        assertEquals(1800f, repo.getEffectiveGoal(LocalDate.of(2026, 1, 1))?.calories)
    }

    @Test
    fun `同一天重复保存覆盖而非新增`() = runBlocking {
        repo.saveGoal(LocalDate.of(2026, 1, 1), 1800f, GoalType.MANUAL, null)
        repo.saveGoal(LocalDate.of(2026, 1, 1), 1600f, GoalType.MANUAL, null)

        assertEquals(1600f, repo.getEffectiveGoal(LocalDate.of(2026, 1, 1))?.calories)
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun `历史日期保持修改前的目标`() = runBlocking {
        repo.saveGoal(LocalDate.of(2026, 1, 1), 1800f, GoalType.MANUAL, null)
        repo.saveGoal(LocalDate.of(2026, 1, 15), 1600f, GoalType.MANUAL, null)

        assertEquals(1800f, repo.getEffectiveGoal(LocalDate.of(2026, 1, 10))?.calories)
        assertEquals(1600f, repo.getEffectiveGoal(LocalDate.of(2026, 1, 20))?.calories)
    }

    @Test
    fun `目标生效日期晚于查询日期时返回 null`() = runBlocking {
        repo.saveGoal(LocalDate.of(2026, 2, 1), 1800f, GoalType.MANUAL, null)

        assertNull(repo.getEffectiveGoal(LocalDate.of(2026, 1, 1)))
    }
}
