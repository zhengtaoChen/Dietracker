package com.diettracker.data.local.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.diettracker.data.local.db.entity.CalorieGoalEntity
import com.diettracker.domain.model.GoalType
import com.diettracker.testutil.TestDb
import java.time.LocalDate
import java.util.UUID
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
class CalorieGoalDaoTest {

    private lateinit var db: com.diettracker.data.local.db.AppDatabase
    private lateinit var dao: CalorieGoalDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = TestDb.inMemory(context)
        dao = db.calorieGoalDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `取某日生效目标为该日及之前最新一条`() = runBlocking {
        dao.upsert(goal(LocalDate.of(2026, 1, 1), 1800f))
        dao.upsert(goal(LocalDate.of(2026, 1, 15), 1600f))

        val onJan10 = dao.getEffectiveOn(LocalDate.of(2026, 1, 10))
        assertEquals(1800f, onJan10?.calories)

        val onJan20 = dao.getEffectiveOn(LocalDate.of(2026, 1, 20))
        assertEquals(1600f, onJan20?.calories)
    }

    @Test
    fun `目标生效日期晚于查询日期时返回 null`() = runBlocking {
        dao.upsert(goal(LocalDate.of(2026, 2, 1), 1800f))

        assertNull(dao.getEffectiveOn(LocalDate.of(2026, 1, 1)))
    }

    private fun goal(effectiveDate: LocalDate, calories: Float): CalorieGoalEntity =
        CalorieGoalEntity(
            id = UUID.randomUUID().toString(),
            effectiveDate = effectiveDate,
            calories = calories,
            goalType = GoalType.MANUAL,
            estimateParams = null,
            createdAt = 0L,
        )
}
