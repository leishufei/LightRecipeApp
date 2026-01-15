package com.recipe.manager

import android.app.Application
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
    }
}
