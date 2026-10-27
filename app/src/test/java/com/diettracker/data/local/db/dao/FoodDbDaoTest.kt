package com.diettracker.data.local.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.domain.model.RawCooked
import com.diettracker.testutil.TestDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FoodDbDaoTest {

    private lateinit var db: com.diettracker.data.local.db.AppDatabase
    private lateinit var dao: FoodDbDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = TestDb.inMemory(context)
        dao = db.foodDbDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `按名称搜索命中`() = runBlocking {
        dao.insertAll(
            listOf(
                food("f1", "米饭", "主食", "白米饭|大米饭"),
                food("f2", "鸡胸肉", "肉类", "鸡胸|鸡脯肉"),
            ),
        )

        val result = dao.search("米饭").first()
        assertEquals(1, result.size)
        assertEquals("米饭", result[0].name)
    }

    @Test
    fun `按别名搜索命中`() = runBlocking {
        dao.insertAll(listOf(food("f1", "米饭", "主食", "白米饭|大米饭")))

        val result = dao.search("大米饭").first()
        assertEquals(1, result.size)
    }

    @Test
    fun `按分类搜索命中`() = runBlocking {
        dao.insertAll(
            listOf(
                food("f1", "米饭", "主食", ""),
                food("f2", "鸡胸肉", "肉类", ""),
            ),
        )

        val result = dao.search("主食").first()
        assertEquals(1, result.size)
        assertEquals("米饭", result[0].name)
    }

    @Test
    fun `insertAll 后 count 正确`() = runBlocking {
        dao.insertAll(
            listOf(food("f1", "米饭", "主食", ""), food("f2", "鸡胸肉", "肉类", "")),
        )
        assertEquals(2, dao.count())
    }

    @Test
    fun `同 id 重复 insert 使用 IGNORE 不覆盖`() = runBlocking {
        dao.insertAll(listOf(food("f1", "米饭", "主食", "", kcal = 130f)))
        dao.insertAll(listOf(food("f1", "改名", "主食", "", kcal = 999f)))

        assertEquals(1, dao.count())
        val loaded = dao.getById("f1")
        assertEquals("米饭", loaded?.name)
        assertTrue(loaded?.kcalPer100g == 130f)
    }

    private fun food(
        id: String,
        name: String,
        category: String,
        aliases: String,
        kcal: Float = 100f,
    ): FoodDbEntity = FoodDbEntity(
        id = id,
        name = name,
        category = category,
        aliases = aliases,
        kcalPer100g = kcal,
        proteinPer100g = 0f,
        carbsPer100g = 0f,
        fatPer100g = 0f,
        rawCooked = RawCooked.RAW,
        source = "test",
        sourceId = null,
        version = 1,
        isCustom = false,
        isFavorite = false,
        createdAt = 0L,
    )
}
