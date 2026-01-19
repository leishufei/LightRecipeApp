@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.recipe.manager.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.recipe.manager.data.entity.RecipeWithCategory
import com.recipe.manager.ui.components.*
import com.recipe.manager.ui.navigation.Screen
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.viewmodel.RecipeSortOrder
import com.recipe.manager.ui.viewmodel.RecipeViewMode
import com.recipe.manager.ui.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    navController: NavController,
    viewModel: RecipeViewModel,
    categoryId: Long? = null
) {
    val uiState by viewModel.listUiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    LaunchedEffect(categoryId) {
        viewModel.setCategoryFilter(categoryId)
        viewModel.clearSelection()
    }
    
    Scaffold(
        topBar = {
            RecipeTopBar(
                title = if (uiState.isSelectionMode) "已选择 ${uiState.selectedIds.size} 项" 
                       else if (categoryId != null) "分类菜谱" else "全部菜谱",
                onBackClick = {
                    if (uiState.isSelectionMode) {
                        viewModel.clearSelection()
                    } else {
                        navController.popBackStack()
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    } else {
                        // 视图切换
                        IconButton(onClick = {
                            viewModel.setViewMode(
                                if (uiState.viewMode == RecipeViewMode.LIST) 
                                    RecipeViewMode.GRID else RecipeViewMode.LIST
                            )
                        }) {
                            Icon(
                                if (uiState.viewMode == RecipeViewMode.LIST) 
                                    Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = "切换视图"
                            )
                        }
                        // 排序
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "排序")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("按点击次数") },
                                    onClick = {
                                        viewModel.setSortOrder(RecipeSortOrder.BY_CLICK_COUNT)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == RecipeSortOrder.BY_CLICK_COUNT) {
                                            Icon(Icons.Default.Check, null, tint = Primary)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("按创建时间") },
                                    onClick = {
                                        viewModel.setSortOrder(RecipeSortOrder.BY_CREATED_TIME)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == RecipeSortOrder.BY_CREATED_TIME) {
                                            Icon(Icons.Default.Check, null, tint = Primary)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("按名称排序") },
                                    onClick = {
                                        viewModel.setSortOrder(RecipeSortOrder.BY_NAME)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == RecipeSortOrder.BY_NAME) {
                                            Icon(Icons.Default.Check, null, tint = Primary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.RecipeEdit.createRoute()) },
                    containerColor = Primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新增菜谱", tint = OnPrimary)
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState()
        } else if (uiState.recipes.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MediumGray
                    )
                },
                message = "暂无菜谱",
                actionText = "添加菜谱",
                onAction = { navController.navigate(Screen.RecipeEdit.createRoute()) }
            )
        } else {
            when (uiState.viewMode) {
                RecipeViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(Background),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.recipes, key = { it.recipe.id }) { recipe ->
                            RecipeListItem(
                                recipe = recipe,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(recipe.recipe.id)
                                    } else {
                                        navController.navigate(Screen.RecipeDetail.createRoute(recipe.recipe.id))
                                    }
                                },
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = uiState.selectedIds.contains(recipe.recipe.id),
                                onSelectionChange = { viewModel.toggleSelection(recipe.recipe.id) }
                            )
                        }
                    }
                }
                RecipeViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(Background),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.recipes, key = { it.recipe.id }) { recipe ->
                            RecipeGridItem(
                                recipe = recipe,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(recipe.recipe.id)
                                    } else {
                                        navController.navigate(Screen.RecipeDetail.createRoute(recipe.recipe.id))
                                    }
                                },
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = uiState.selectedIds.contains(recipe.recipe.id)
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "批量删除",
            message = "确定要删除选中的 ${uiState.selectedIds.size} 个菜谱吗？删除后无法恢复。",
            onConfirm = {
                viewModel.deleteSelected { _, _ ->
                    showDeleteConfirm = false
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecipeGridItem(
    recipe: RecipeWithCategory,
    onClick: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.1f) else Surface
        ),
        onClick = onClick
    ) {
        Box {
            Column {
                RecipeImage(
                    imagePath = recipe.recipe.coverImagePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = recipe.recipe.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MediumGray
                        )
                        Text(
                            text = " ${recipe.recipe.clickCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MediumGray
                        )
                    }
                }
            }
            
            if (isSelectionMode) {
                SelectableCheckbox(
                    selected = isSelected,
                    onSelectedChange = { onClick() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
            
            if (recipe.recipe.isFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "已收藏",
                    tint = Error,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}
