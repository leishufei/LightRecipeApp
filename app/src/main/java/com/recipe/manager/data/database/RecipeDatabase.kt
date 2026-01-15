package com.recipe.manager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.recipe.manager.data.dao.CategoryDao
import com.recipe.manager.data.dao.IngredientDao
import com.recipe.manager.data.dao.RecipeDao
import com.recipe.manager.data.dao.StepDao
import com.recipe.manager.data.entity.Category
import com.recipe.manager.data.entity.Ingredient
import com.recipe.manager.data.entity.Recipe
import com.recipe.manager.data.entity.Step

@Database(
    entities = [Recipe::class, Category::class, Step::class, Ingredient::class],
    version = 1,
    exportSchema = false
)
abstract class RecipeDatabase : RoomDatabase() {
    
    abstract fun recipeDao(): RecipeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun stepDao(): StepDao
    abstract fun ingredientDao(): IngredientDao
    
    companion object {
        @Volatile
        private var INSTANCE: RecipeDatabase? = null
        
        fun getDatabase(context: Context): RecipeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecipeDatabase::class.java,
                    "recipe_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
