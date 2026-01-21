package com.recipe.manager.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 菜谱完整信息（包含用料和步骤）
 */
data class RecipeWithDetails(
    @Embedded val recipe: Recipe,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<Ingredient>,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val steps: List<Step>
)

/**
 * 菜谱带分类信息
 */
data class RecipeWithCategory(
    @Embedded val recipe: Recipe,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
)

/**
 * 分类带菜谱数量
 */
data class CategoryWithCount(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val recipeCount: Int
)
