package com.recipe.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.recipe.manager.data.entity.Category
import com.recipe.manager.data.entity.CategoryWithCount
import com.recipe.manager.data.repository.CategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CategorySortOrder {
    BY_CREATED_TIME,
    BY_RECIPE_COUNT
}

data class CategoryUiState(
    val categories: List<CategoryWithCount> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOrder: CategorySortOrder = CategorySortOrder.BY_CREATED_TIME,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

class CategoryViewModel(private val repository: CategoryRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(CategorySortOrder.BY_CREATED_TIME)
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _sortOrder.flatMapLatest { sortOrder ->
                when (sortOrder) {
                    CategorySortOrder.BY_CREATED_TIME -> repository.getCategoriesWithCount()
                    CategorySortOrder.BY_RECIPE_COUNT -> repository.getCategoriesOrderByRecipeCount()
                }
            }.collect { categories ->
                _uiState.update { 
                    it.copy(categories = categories, isLoading = false, error = null) 
                }
            }
        }
    }
    
    fun setSortOrder(order: CategorySortOrder) {
        _sortOrder.value = order
        _uiState.update { it.copy(sortOrder = order) }
    }
    
    fun addCategory(name: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getCategoryByName(name.trim())
            if (existing != null) {
                onResult(false, "分类名称已存在")
                return@launch
            }
            val category = Category(name = name.trim())
            repository.insertCategory(category)
            onResult(true, "添加成功")
        }
    }
    
    fun updateCategory(id: Long, name: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getCategoryByName(name.trim())
            if (existing != null && existing.id != id) {
                onResult(false, "分类名称已存在")
                return@launch
            }
            val category = repository.getCategoryById(id)
            if (category != null) {
                repository.updateCategory(category.copy(name = name.trim()))
                onResult(true, "更新成功")
            } else {
                onResult(false, "分类不存在")
            }
        }
    }
    
    fun deleteCategory(category: CategoryWithCount, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cat = repository.getCategoryById(category.id)
            if (cat != null) {
                repository.deleteCategory(cat)
                onResult(true, "删除成功")
            }
        }
    }
    
    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSelected = if (state.selectedIds.contains(id)) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(
                selectedIds = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }
    
    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }
    
    fun deleteSelected(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            if (ids.isNotEmpty()) {
                repository.deleteCategoriesByIds(ids)
                clearSelection()
                onResult(true, "批量删除成功")
            }
        }
    }
    
    class Factory(private val repository: CategoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoryViewModel(repository) as T
        }
    }
}
