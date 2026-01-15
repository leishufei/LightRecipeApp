package com.recipe.manager.data.repository

import com.recipe.manager.data.dao.IngredientDao
import com.recipe.manager.data.dao.RecipeDao
import com.recipe.manager.data.dao.StepDao
import com.recipe.manager.data.entity.Ingredient
import com.recipe.manager.data.entity.Recipe
import com.recipe.manager.data.entity.RecipeWithCategory
import com.recipe.manager.data.entity.RecipeWithDetails
import com.recipe.manager.data.entity.Step
import kotlinx.coroutines.flow.Flow

class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val ingredientDao: IngredientDao,
    private val stepDao: StepDao
) {
    
    fun getAllRecipesWithCategory(): Flow<List<RecipeWithCategory>> = 
        recipeDao.getAllRecipesWithCategory()
    
    fun getRecipesByCategory(categoryId: Long): Flow<List<RecipeWithCategory>> = 
        recipeDao.getRecipesByCategoryWithDetails(categoryId)
    
    fun getFavoriteRecipes(): Flow<List<Recipe>> = 
        recipeDao.getFavoriteRecipes()
    
    fun searchRecipes(keyword: String): Flow<List<Recipe>> = 
        recipeDao.searchRecipes(keyword)
    
    fun getRecipesOrderByClickCount(): Flow<List<RecipeWithCategory>> = 
        recipeDao.getRecipesOrderByClickCount()
    
    fun getRecipesOrderByCreatedAt(): Flow<List<RecipeWithCategory>> = 
        recipeDao.getRecipesOrderByCreatedAt()
    
    fun getRecipesOrderByName(): Flow<List<RecipeWithCategory>> = 
        recipeDao.getRecipesOrderByName()
    
    suspend fun getRecipeWithDetails(id: Long): RecipeWithDetails? = 
        recipeDao.getRecipeWithDetails(id)
    
    suspend fun getRecipeById(id: Long): Recipe? = 
        recipeDao.getRecipeById(id)
    
    suspend fun insertRecipe(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>
    ): Long {
        val recipeId = recipeDao.insert(recipe)
        val ingredientsWithRecipeId = ingredients.mapIndexed { index, ingredient ->
            ingredient.copy(recipeId = recipeId, sortOrder = index)
        }
        val stepsWithRecipeId = steps.mapIndexed { index, step ->
            step.copy(recipeId = recipeId, stepNumber = index + 1, sortOrder = index)
        }
        ingredientDao.insertAll(ingredientsWithRecipeId)
        stepDao.insertAll(stepsWithRecipeId)
        return recipeId
    }
    
    suspend fun updateRecipe(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>
    ) {
        recipeDao.update(recipe.copy(updatedAt = System.currentTimeMillis()))
        ingredientDao.deleteByRecipeId(recipe.id)
        stepDao.deleteByRecipeId(recipe.id)
        val ingredientsWithRecipeId = ingredients.mapIndexed { index, ingredient ->
            ingredient.copy(recipeId = recipe.id, sortOrder = index)
        }
        val stepsWithRecipeId = steps.mapIndexed { index, step ->
            step.copy(recipeId = recipe.id, stepNumber = index + 1, sortOrder = index)
        }
        ingredientDao.insertAll(ingredientsWithRecipeId)
        stepDao.insertAll(stepsWithRecipeId)
    }
    
    suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.delete(recipe)
    }
    
    suspend fun deleteRecipesByIds(ids: List<Long>) {
        recipeDao.deleteByIds(ids)
    }
    
    suspend fun incrementClickCount(id: Long) {
        recipeDao.incrementClickCount(id)
    }
    
    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        recipeDao.updateFavorite(id, isFavorite)
    }
}
