package com.recipe.manager.data.repository

import com.recipe.manager.data.dao.CategoryDao
import com.recipe.manager.data.entity.Category
import com.recipe.manager.data.entity.CategoryWithCount
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    
    fun getAllCategories(): Flow<List<Category>> = 
        categoryDao.getAllCategories()
    
    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>> = 
        categoryDao.getCategoriesWithCount()
    
    fun getCategoriesOrderByRecipeCount(): Flow<List<CategoryWithCount>> = 
        categoryDao.getCategoriesOrderByRecipeCount()
    
    fun getCategoriesOrderBySortOrder(): Flow<List<CategoryWithCount>> =
        categoryDao.getCategoriesOrderBySortOrder()
    
    suspend fun getCategoryById(id: Long): Category? = 
        categoryDao.getCategoryById(id)
    
    suspend fun getCategoryByName(name: String): Category? = 
        categoryDao.getCategoryByName(name)
    
    suspend fun getMaxSortOrder(): Int =
        categoryDao.getMaxSortOrder()
    
    suspend fun insertCategory(category: Category): Long = 
        categoryDao.insert(category)
    
    suspend fun updateCategory(category: Category) {
        categoryDao.update(category.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun initializeSortOrderIfNeeded() {
        categoryDao.initializeSortOrderIfNeeded()
    }
    
    suspend fun updateCategoriesOrder(categories: List<Category>) {
        categoryDao.updateCategories(categories)
    }
    
    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }
    
    suspend fun deleteCategoriesByIds(ids: List<Long>) {
        categoryDao.deleteByIds(ids)
    }
}
