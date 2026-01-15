package com.recipe.manager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 步骤实体
 */
@Entity(
    tableName = "steps",
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
data class Step(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: Long,
    val description: String,
    val imagePath: String? = null,
    val stepNumber: Int,
    val sortOrder: Int = 0
)
