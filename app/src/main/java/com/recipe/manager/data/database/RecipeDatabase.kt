package com.recipe.manager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 sortOrder 列
                database.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                // 为现有分类设置递增的 sortOrder（按 createdAt 排序）
                database.execSQL("""
                    UPDATE categories SET sortOrder = (
                        SELECT COUNT(*) FROM categories c2 WHERE c2.createdAt < categories.createdAt
                    ) + 1
                """)
            }
        }
        
        fun getDatabase(context: Context): RecipeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecipeDatabase::class.java,
                    "recipe_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
