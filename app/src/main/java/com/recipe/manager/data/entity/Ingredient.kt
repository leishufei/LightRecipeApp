package com.recipe.manager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用料实体
 */
@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId")]
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: Long,
    val name: String,
    val amount: String,
    val sortOrder: Int = 0
)
