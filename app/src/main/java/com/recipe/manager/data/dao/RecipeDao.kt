package com.recipe.manager.data.dao

import androidx.room.*
import com.recipe.manager.data.entity.Recipe
import com.recipe.manager.data.entity.RecipeWithCategory
import com.recipe.manager.data.entity.RecipeWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<Recipe>>
    
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipesWithCategory(): Flow<List<RecipeWithCategory>>
    
    @Query("SELECT * FROM recipes WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getRecipesByCategory(categoryId: Long): Flow<List<Recipe>>
    
    @Transaction
    @Query("SELECT * FROM recipes WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getRecipesByCategoryWithDetails(categoryId: Long): Flow<List<RecipeWithCategory>>
    
    @Query("SELECT * FROM recipes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRecipes(): Flow<List<Recipe>>
    
    @Query("""
        SELECT DISTINCT r.* FROM recipes r 
        LEFT JOIN ingredients i ON r.id = i.recipeId 
        WHERE r.name LIKE '%' || :keyword || '%' 
           OR i.name LIKE '%' || :keyword || '%'
        ORDER BY r.createdAt DESC
    """)
    fun searchRecipes(keyword: String): Flow<List<Recipe>>
    
    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeWithDetails(id: Long): RecipeWithDetails?
    
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: Long): Recipe?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: Recipe): Long
    
    @Update
    suspend fun update(recipe: Recipe)
    
    @Delete
    suspend fun delete(recipe: Recipe)
    
    @Query("DELETE FROM recipes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    @Query("UPDATE recipes SET clickCount = clickCount + 1 WHERE id = :id")
    suspend fun incrementClickCount(id: Long)
    
    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
    
    // 排序查询
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY clickCount DESC")
    fun getRecipesOrderByClickCount(): Flow<List<RecipeWithCategory>>
    
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getRecipesOrderByCreatedAt(): Flow<List<RecipeWithCategory>>
    
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getRecipesOrderByName(): Flow<List<RecipeWithCategory>>
}
