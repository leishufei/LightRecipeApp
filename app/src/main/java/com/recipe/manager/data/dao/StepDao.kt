package com.recipe.manager.data.dao

import androidx.room.*
import com.recipe.manager.data.entity.Step
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    
    @Query("SELECT * FROM steps WHERE recipeId = :recipeId ORDER BY stepNumber ASC")
    fun getStepsByRecipe(recipeId: Long): Flow<List<Step>>
    
    @Query("SELECT * FROM steps WHERE recipeId = :recipeId ORDER BY stepNumber ASC")
    suspend fun getStepsByRecipeSync(recipeId: Long): List<Step>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: Step): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<Step>)
    
    @Update
    suspend fun update(step: Step)
    
    @Delete
    suspend fun delete(step: Step)
    
    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteByRecipeId(recipeId: Long)
}
