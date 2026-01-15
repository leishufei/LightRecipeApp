package com.recipe.manager.data.dao

import androidx.room.*
import com.recipe.manager.data.entity.Category
import com.recipe.manager.data.entity.CategoryWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    fun getAllCategories(): Flow<List<Category>>
    
    @Query("""
        SELECT c.id, c.name, c.createdAt, c.updatedAt, 
               COUNT(r.id) as recipeCount 
        FROM categories c 
        LEFT JOIN recipes r ON c.id = r.categoryId 
        GROUP BY c.id 
        ORDER BY c.createdAt DESC
    """)
    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>>
    
    @Query("""
        SELECT c.id, c.name, c.createdAt, c.updatedAt, 
               COUNT(r.id) as recipeCount 
        FROM categories c 
        LEFT JOIN recipes r ON c.id = r.categoryId 
        GROUP BY c.id 
        ORDER BY recipeCount DESC
    """)
    fun getCategoriesOrderByRecipeCount(): Flow<List<CategoryWithCount>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?
    
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    
    @Update
    suspend fun update(category: Category)
    
    @Delete
    suspend fun delete(category: Category)
    
    @Query("DELETE FROM categories WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
