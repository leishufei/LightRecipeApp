package com.recipe.manager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
