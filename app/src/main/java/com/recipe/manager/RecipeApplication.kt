package com.recipe.manager

import android.app.Application
import com.recipe.manager.data.SampleDataGenerator
import com.recipe.manager.data.database.RecipeDatabase
import com.recipe.manager.data.repository.CategoryRepository
import com.recipe.manager.data.repository.RecipeRepository
import timber.log.Timber

class RecipeApplication : Application() {
    
    val database by lazy { RecipeDatabase.getDatabase(this) }
    
    val categoryRepository by lazy { 
        CategoryRepository(database.categoryDao()) 
    }
    
    val recipeRepository by lazy { 
        RecipeRepository(
            database.recipeDao(),
            database.ingredientDao(),
            database.stepDao()
        ) 
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Timber 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // ============================================
        // 测试数据生成器
        // 在首次启动时自动生成100个测试菜谱
        // 移除方法：删除下面这行代码和 SampleDataGenerator.kt 文件
        // ============================================
        SampleDataGenerator.generateIfNeeded(this, categoryRepository, recipeRepository)
    }
}
