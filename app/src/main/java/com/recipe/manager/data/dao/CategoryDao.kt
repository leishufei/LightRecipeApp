package com.recipe.manager.data.dao

import androidx.room.*
import com.recipe.manager.data.entity.Category
import com.recipe.manager.data.entity.CategoryWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllCategories(): Flow<List<Category>>
    
    @Query("""
        SELECT c.id, c.name, c.sortOrder, c.createdAt, c.updatedAt, 
               COUNT(r.id) as recipeCount 
        FROM categories c 
        LEFT JOIN recipes r ON c.id = r.categoryId 
        GROUP BY c.id 
        ORDER BY c.sortOrder ASC, c.createdAt ASC
    """)
    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>>
    
    @Query("""
        SELECT c.id, c.name, c.sortOrder, c.createdAt, c.updatedAt, 
               COUNT(r.id) as recipeCount 
        FROM categories c 
        LEFT JOIN recipes r ON c.id = r.categoryId 
        GROUP BY c.id 
        ORDER BY recipeCount DESC
    """)
    fun getCategoriesOrderByRecipeCount(): Flow<List<CategoryWithCount>>
    
    @Query("""
        SELECT c.id, c.name, c.sortOrder, c.createdAt, c.updatedAt, 
               COUNT(r.id) as recipeCount 
        FROM categories c 
        LEFT JOIN recipes r ON c.id = r.categoryId 
        GROUP BY c.id 
        ORDER BY c.sortOrder ASC, c.createdAt ASC
    """)
    fun getCategoriesOrderBySortOrder(): Flow<List<CategoryWithCount>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?
    
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?
    
    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories")
    suspend fun getMaxSortOrder(): Int
    
    @Query("SELECT * FROM categories ORDER BY createdAt ASC")
    suspend fun getAllCategoriesSync(): List<Category>
    
    @Transaction
    suspend fun initializeSortOrderIfNeeded() {
        val categories = getAllCategoriesSync()
        // 检查是否所有分类的 sortOrder 都是 0
        val allZero = categories.all { it.sortOrder == 0 }
        if (allZero && categories.isNotEmpty()) {
            categories.forEachIndexed { index, category ->
                update(category.copy(sortOrder = index + 1))
            }
        }
    }
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    
    @Update
    suspend fun update(category: Category)
    
    @Transaction
    suspend fun updateCategories(categories: List<Category>) {
        categories.forEach { update(it) }
    }
    
    @Delete
    suspend fun delete(category: Category)
    
    @Query("DELETE FROM categories WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
