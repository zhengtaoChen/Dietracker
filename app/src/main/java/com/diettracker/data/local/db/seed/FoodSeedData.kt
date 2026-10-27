package com.diettracker.data.local.db.seed

import com.diettracker.data.local.db.entity.FoodDbEntity
import com.diettracker.domain.model.RawCooked

/**
 * 内置示例食物种子数据。
 *
 * 数值为每 100g 的 USDA FoodData Central 常见参考值（公有领域，可合法使用）。
 * 本阶段仅提供 12 条示例，用于打通「搜索 → 按克折算营养」流程；正式生产前需
 * 对照 USDA FDC 官方原始条目逐条核验，并补齐精确的外部条目 ID（sourceId），
 * 再按 PRD 建议扩充至 150–300 种。
 *
 * 诚实性约束（PRD §5.1「来源与版本」）：
 * - [sourceId] 一律为 null——现阶段未核验官方 FDC 条目 ID，不能用占位字符串
 *   冒充真实的外部条目标识。
 * - 数值为近似参考值；「北豆腐（硬）」为中式豆制品，无单一 USDA FDC 条目直接对应，
 *   其数值为常见参考值（见技术文档「阶段 2B」来源核验说明），不视为精确测量值。
 */
object FoodSeedData {

    const val SOURCE = "USDA FoodData Central 常见参考值（示例，待核对官方 FDC 条目 ID）"
    const val VERSION = 1

    fun all(): List<FoodDbEntity> = listOf(
        food("seed_rice_cooked", "米饭（熟，白米）", "主食", "米饭|白米饭|大米饭|熟米饭", 130f, 2.7f, 28.2f, 0.3f, RawCooked.COOKED),
        food("seed_oats_dry", "燕麦片（干）", "主食", "燕麦|燕麦片|麦片", 389f, 16.9f, 66.3f, 6.9f, RawCooked.RAW),
        food("seed_potato_raw", "土豆（生）", "蔬菜", "土豆|马铃薯|洋芋", 77f, 2.0f, 17.5f, 0.1f, RawCooked.RAW),
        food("seed_broccoli_raw", "西兰花（生）", "蔬菜", "西兰花|绿花菜|花椰菜", 34f, 2.8f, 6.6f, 0.4f, RawCooked.RAW),
        food("seed_apple_raw", "苹果（生，带皮）", "水果", "苹果", 52f, 0.3f, 13.8f, 0.2f, RawCooked.RAW),
        food("seed_banana_raw", "香蕉（生）", "水果", "香蕉", 89f, 1.1f, 22.8f, 0.3f, RawCooked.RAW),
        food("seed_chicken_breast_raw", "鸡胸肉（生，去皮）", "肉类", "鸡胸肉|鸡胸|鸡脯肉", 120f, 22.5f, 0f, 2.6f, RawCooked.RAW),
        food("seed_salmon_raw", "三文鱼（生）", "肉类", "三文鱼|鲑鱼", 208f, 20.4f, 0f, 13.4f, RawCooked.RAW),
        food("seed_egg_raw", "鸡蛋（生，全蛋）", "蛋奶", "鸡蛋|全蛋", 143f, 12.6f, 0.7f, 9.5f, RawCooked.RAW),
        food("seed_milk_whole", "全脂牛奶", "蛋奶", "牛奶|全脂奶", 61f, 3.2f, 4.8f, 3.3f, RawCooked.RAW),
        food("seed_tofu_firm", "北豆腐（硬）", "豆制品", "豆腐|北豆腐|老豆腐", 76f, 8.1f, 1.9f, 4.8f, RawCooked.RAW),
        food("seed_olive_oil", "橄榄油", "油脂", "橄榄油", 884f, 0f, 0f, 100f, RawCooked.RAW),
    )

    private fun food(
        id: String,
        name: String,
        category: String,
        aliases: String,
        kcal: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        rawCooked: RawCooked,
    ): FoodDbEntity = FoodDbEntity(
        id = id,
        name = name,
        category = category,
        aliases = aliases,
        kcalPer100g = kcal,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
        rawCooked = rawCooked,
        source = SOURCE,
        sourceId = null,
        version = VERSION,
        isCustom = false,
        isFavorite = false,
        createdAt = 0L,
    )
}
