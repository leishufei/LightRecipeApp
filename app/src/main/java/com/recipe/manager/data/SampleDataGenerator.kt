package com.recipe.manager.data

import android.content.Context
import com.recipe.manager.data.entity.Category
import com.recipe.manager.data.entity.Ingredient
import com.recipe.manager.data.entity.Recipe
import com.recipe.manager.data.entity.Step
import com.recipe.manager.data.repository.CategoryRepository
import com.recipe.manager.data.repository.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 测试数据生成器
 * 
 * 用于在应用初始化时生成测试数据
 * 包含10个分类，每个分类10个菜谱，共100个菜谱
 * 
 * 使用方法：
 * 在 RecipeApplication.onCreate() 中调用：
 * SampleDataGenerator.generateIfNeeded(this, categoryRepository, recipeRepository)
 * 
 * 移除方法：
 * 1. 删除此文件
 * 2. 删除 RecipeApplication.kt 中的调用代码
 */
object SampleDataGenerator {
    
    // 分类名称
    private val categoryNames = listOf(
        "家常菜", "川菜", "粤菜", "湘菜", "鲁菜",
        "素菜", "汤羹", "甜点", "小吃", "凉菜"
    )
    
    // 菜谱名称前缀
    private val recipeNamePrefixes = listOf(
        "红烧", "清蒸", "爆炒", "干煸", "水煮",
        "麻辣", "香煎", "糖醋", "宫保", "鱼香"
    )
    
    // 菜谱名称主体
    private val recipeNameMains = listOf(
        "鸡肉", "猪肉", "牛肉", "鱼", "虾",
        "豆腐", "茄子", "土豆", "青菜", "蘑菇"
    )
    
    // 用料名称
    private val ingredientNames = listOf(
        "猪肉", "鸡肉", "牛肉", "鱼肉", "虾仁",
        "豆腐", "茄子", "土豆", "青菜", "蘑菇",
        "葱", "姜", "蒜", "辣椒", "花椒",
        "酱油", "料酒", "盐", "糖", "醋",
        "油", "淀粉", "鸡蛋", "香菜", "芝麻"
    )
    
    // 用量单位
    private val amounts = listOf(
        "100g", "200g", "300g", "适量", "少许",
        "1个", "2个", "3个", "1勺", "2勺",
        "1块", "2块", "半斤", "一把", "若干"
    )
    
    // 步骤描述模板
    private val stepTemplates = listOf(
        "将主料洗净切块，备用",
        "锅中倒油，烧热后放入葱姜蒜爆香",
        "加入主料翻炒至变色",
        "加入调料，继续翻炒均匀",
        "加入适量清水，大火烧开",
        "转小火慢炖15-20分钟",
        "收汁，撒上葱花即可出锅",
        "装盘，趁热享用"
    )
    
    /**
     * 检查并生成测试数据
     * 只在数据库为空时生成
     */
    fun generateIfNeeded(
        context: Context,
        categoryRepository: CategoryRepository,
        recipeRepository: RecipeRepository
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 检查是否已有数据
                val existingCategories = categoryRepository.getAllCategories().first()
                if (existingCategories.isNotEmpty()) {
                    Timber.d("数据库已有数据，跳过生成测试数据")
                    return@launch
                }
                
                Timber.d("开始生成测试数据...")
                generateSampleData(categoryRepository, recipeRepository)
                Timber.d("测试数据生成完成！")
            } catch (e: Exception) {
                Timber.e(e, "生成测试数据失败")
            }
        }
    }
    
    /**
     * 生成测试数据
     */
    private suspend fun generateSampleData(
        categoryRepository: CategoryRepository,
        recipeRepository: RecipeRepository
    ) {
        val currentTime = System.currentTimeMillis()
        
        // 1. 创建10个分类
        val categoryIds = mutableListOf<Long>()
        categoryNames.forEach { name ->
            val category = Category(
                name = name,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            val categoryId = categoryRepository.insertCategory(category)
            categoryIds.add(categoryId)
            Timber.d("创建分类: $name (ID: $categoryId)")
        }
        
        // 2. 为每个分类创建10个菜谱
        var recipeCount = 0
        categoryIds.forEachIndexed { categoryIndex, categoryId ->
            repeat(10) { recipeIndex ->
                val recipe = generateRecipe(categoryId, categoryIndex, recipeIndex, currentTime)
                val ingredients = generateIngredients()
                val steps = generateSteps()
                
                recipeRepository.insertRecipe(recipe, ingredients, steps)
                recipeCount++
                
                if (recipeCount % 10 == 0) {
                    Timber.d("已生成 $recipeCount 个菜谱...")
                }
            }
        }
        
        Timber.d("共生成 ${categoryIds.size} 个分类，$recipeCount 个菜谱")
    }
    
    /**
     * 生成单个菜谱
     */
    private fun generateRecipe(
        categoryId: Long,
        categoryIndex: Int,
        recipeIndex: Int,
        currentTime: Long
    ): Recipe {
        // 生成菜谱名称
        val prefix = recipeNamePrefixes[recipeIndex % recipeNamePrefixes.size]
        val main = recipeNameMains[(categoryIndex + recipeIndex) % recipeNameMains.size]
        val name = "$prefix$main"
        
        // 随机决定是否收藏（20%概率）
        val isFavorite = (categoryIndex + recipeIndex) % 5 == 0
        
        return Recipe(
            name = name,
            categoryId = categoryId,
            coverImagePath = null, // 测试数据不包含图片
            isFavorite = isFavorite,
            clickCount = (recipeIndex * 3) % 50, // 模拟点击次数
            createdAt = currentTime - (recipeIndex * 1000L), // 错开创建时间
            updatedAt = currentTime - (recipeIndex * 1000L)
        )
    }
    
    /**
     * 生成用料列表
     */
    private fun generateIngredients(): List<Ingredient> {
        val count = (3..6).random() // 每个菜谱3-6个用料
        return (0 until count).map { index ->
            val name = ingredientNames[(index * 3) % ingredientNames.size]
            val amount = amounts[index % amounts.size]
            Ingredient(
                name = name,
                amount = amount,
                recipeId = 0 // 会在插入时自动设置
            )
        }
    }
    
    /**
     * 生成步骤列表
     */
    private fun generateSteps(): List<Step> {
        val count = (4..8).random() // 每个菜谱4-8个步骤
        return (0 until count).map { index ->
            val description = if (index < stepTemplates.size) {
                stepTemplates[index]
            } else {
                "继续烹饪，注意火候和时间"
            }
            Step(
                description = description,
                imagePath = null, // 测试数据不包含图片
                stepNumber = index + 1,
                recipeId = 0 // 会在插入时自动设置
            )
        }
    }
}
