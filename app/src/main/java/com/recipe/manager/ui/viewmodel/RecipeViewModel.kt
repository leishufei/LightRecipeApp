package com.recipe.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.recipe.manager.data.entity.Ingredient
import com.recipe.manager.data.entity.Recipe
import com.recipe.manager.data.entity.RecipeWithCategory
import com.recipe.manager.data.entity.RecipeWithDetails
import com.recipe.manager.data.entity.Step
import com.recipe.manager.data.repository.RecipeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class RecipeSortOrder {
    BY_CLICK_COUNT,
    BY_CREATED_TIME,
    BY_NAME
}

enum class RecipeViewMode {
    LIST,
    GRID
}

data class RecipeListUiState(
    val recipes: List<RecipeWithCategory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOrder: RecipeSortOrder = RecipeSortOrder.BY_CLICK_COUNT,
    val viewMode: RecipeViewMode = RecipeViewMode.LIST,
    val selectedCategoryId: Long? = null,
    val searchKeyword: String = "",
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

data class RecipeDetailUiState(
    val recipeWithDetails: RecipeWithDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {
    
    private val _listUiState = MutableStateFlow(RecipeListUiState())
    val listUiState: StateFlow<RecipeListUiState> = _listUiState.asStateFlow()
    
    private val _detailUiState = MutableStateFlow(RecipeDetailUiState())
    val detailUiState: StateFlow<RecipeDetailUiState> = _detailUiState.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(RecipeSortOrder.BY_CLICK_COUNT)
    private val _categoryFilter = MutableStateFlow<Long?>(null)
    private val _searchKeyword = MutableStateFlow("")
    
    // 记录当前会话已计数的菜谱ID，防止重复计数
    private val clickedRecipeIds = mutableSetOf<Long>()
    
    init {
        loadRecipes()
    }
    
    private fun loadRecipes() {
        viewModelScope.launch {
            _listUiState.update { it.copy(isLoading = true) }
            combine(_sortOrder, _categoryFilter, _searchKeyword) { sort, category, keyword ->
                Triple(sort, category, keyword)
            }.flatMapLatest { (sort, category, keyword) ->
                when {
                    keyword.isNotBlank() -> {
                        repository.searchRecipes(keyword).map { recipes ->
                            recipes.map { RecipeWithCategory(it, null) }
                        }
                    }
                    category != null -> repository.getRecipesByCategory(category)
                    else -> when (sort) {
                        RecipeSortOrder.BY_CLICK_COUNT -> repository.getRecipesOrderByClickCount()
                        RecipeSortOrder.BY_CREATED_TIME -> repository.getRecipesOrderByCreatedAt()
                        RecipeSortOrder.BY_NAME -> repository.getRecipesOrderByName()
                    }
                }
            }.collect { recipes ->
                _listUiState.update { 
                    it.copy(recipes = recipes, isLoading = false, error = null) 
                }
            }
        }
    }
    
    fun setSortOrder(order: RecipeSortOrder) {
        _sortOrder.value = order
        _listUiState.update { it.copy(sortOrder = order) }
    }
    
    fun setViewMode(mode: RecipeViewMode) {
        _listUiState.update { it.copy(viewMode = mode) }
    }
    
    fun setCategoryFilter(categoryId: Long?) {
        _categoryFilter.value = categoryId
        _listUiState.update { it.copy(selectedCategoryId = categoryId) }
    }
    
    fun setSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
        _listUiState.update { it.copy(searchKeyword = keyword) }
    }
    
    fun loadRecipeDetail(id: Long) {
        viewModelScope.launch {
            _detailUiState.update { it.copy(isLoading = true) }
            val details = repository.getRecipeWithDetails(id)
            _detailUiState.update { 
                it.copy(recipeWithDetails = details, isLoading = false) 
            }
            // 点击计数（同一会话内只计一次）
            if (!clickedRecipeIds.contains(id)) {
                repository.incrementClickCount(id)
                clickedRecipeIds.add(id)
            }
        }
    }
    
    fun addRecipe(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        onResult: (Boolean, String, Long?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = repository.insertRecipe(recipe, ingredients, steps)
                onResult(true, "添加成功", id)
            } catch (e: Exception) {
                onResult(false, "添加失败: ${e.message}", null)
            }
        }
    }
    
    fun updateRecipe(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateRecipe(recipe, ingredients, steps)
                onResult(true, "更新成功")
            } catch (e: Exception) {
                onResult(false, "更新失败: ${e.message}")
            }
        }
    }
    
    fun deleteRecipe(recipe: Recipe, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(recipe)
                onResult(true, "删除成功")
            } catch (e: Exception) {
                onResult(false, "删除失败: ${e.message}")
            }
        }
    }
    
    fun toggleFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateFavorite(id, isFavorite)
            // 刷新详情
            loadRecipeDetail(id)
        }
    }
    
    fun toggleSelection(id: Long) {
        _listUiState.update { state ->
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
        _listUiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }
    
    fun deleteSelected(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val ids = _listUiState.value.selectedIds.toList()
            if (ids.isNotEmpty()) {
                repository.deleteRecipesByIds(ids)
                clearSelection()
                onResult(true, "批量删除成功")
            }
        }
    }
    
    class Factory(private val repository: RecipeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecipeViewModel(repository) as T
        }
    }
}
