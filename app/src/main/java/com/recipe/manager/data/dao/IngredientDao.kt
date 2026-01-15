package com.recipe.manager.data.dao

import androidx.room.*
import com.recipe.manager.data.entity.Ingredient
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    
    @Query("SELECT * FROM ingredients WHERE recipeId = :recipeId ORDER BY sortOrder ASC")
    fun getIngredientsByRecipe(recipeId: Long): Flow<List<Ingredient>>
    
    @Query("SELECT * FROM ingredients WHERE recipeId = :recipeId ORDER BY sortOrder ASC")
    suspend fun getIngredientsByRecipeSync(recipeId: Long): List<Ingredient>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: Ingredient): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<Ingredient>)
    
    @Update
    suspend fun update(ingredient: Ingredient)
    
    @Delete
    suspend fun delete(ingredient: Ingredient)
    
    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteByRecipeId(recipeId: Long)
}
