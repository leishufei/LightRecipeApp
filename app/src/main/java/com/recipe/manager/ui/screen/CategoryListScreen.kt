package com.recipe.manager.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.recipe.manager.data.entity.CategoryWithCount
import com.recipe.manager.ui.components.*
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.viewmodel.CategorySortOrder
import com.recipe.manager.ui.viewmodel.CategoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    navController: NavController,
    viewModel: CategoryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryWithCount?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryWithCount?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.clearSelection()
    }
    
    Scaffold(
        topBar = {
            RecipeTopBar(
                title = if (uiState.isSelectionMode) "已选择 ${uiState.selectedIds.size} 项" else "分类管理",
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
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "排序")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("按创建时间") },
                                    onClick = {
                                        viewModel.setSortOrder(CategorySortOrder.BY_CREATED_TIME)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == CategorySortOrder.BY_CREATED_TIME) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Primary)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("按菜谱数量") },
                                    onClick = {
                                        viewModel.setSortOrder(CategorySortOrder.BY_RECIPE_COUNT)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == CategorySortOrder.BY_RECIPE_COUNT) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Primary)
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
                    onClick = { showAddDialog = true },
                    containerColor = Primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新增分类", tint = OnPrimary)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState()
        } else if (uiState.categories.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MediumGray
                    )
                },
                message = "还没有分类，点击下方按钮添加",
                actionText = "添加分类",
                onAction = { showAddDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Background),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.categories, key = { it.id }) { category ->
                    CategoryListItem(
                        category = category,
                        isSelectionMode = uiState.isSelectionMode,
                        isSelected = uiState.selectedIds.contains(category.id),
                        onLongClick = { viewModel.toggleSelection(category.id) },
                        onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.toggleSelection(category.id)
                            }
                        },
                        onEdit = { editingCategory = category },
                        onDelete = { deletingCategory = category }
                    )
                }
            }
        }
    }
    
    // 添加分类对话框
    if (showAddDialog) {
        InputDialog(
            title = "新增分类",
            placeholder = "请输入分类名称",
            onConfirm = { name ->
                viewModel.addCategory(name) { success, message ->
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }
    
    // 编辑分类对话框
    editingCategory?.let { category ->
        InputDialog(
            title = "编辑分类",
            initialValue = category.name,
            placeholder = "请输入分类名称",
            onConfirm = { name ->
                viewModel.updateCategory(category.id, name) { _, _ ->
                    editingCategory = null
                }
            },
            onDismiss = { editingCategory = null }
        )
    }
    
    // 删除确认对话框
    deletingCategory?.let { category ->
        val message = if (category.recipeCount > 0) {
            "该分类下有 ${category.recipeCount} 个菜谱，删除分类将同时删除这些菜谱，确定要删除吗？"
        } else {
            "确定要删除分类「${category.name}」吗？"
        }
        ConfirmDialog(
            title = "删除分类",
            message = message,
            confirmText = "删除",
            onConfirm = {
                viewModel.deleteCategory(category) { _, _ ->
                    deletingCategory = null
                }
            },
            onDismiss = { deletingCategory = null }
        )
    }
    
    // 批量删除确认
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "批量删除",
            message = "确定要删除选中的 ${uiState.selectedIds.size} 个分类吗？关联的菜谱也会被删除。",
            confirmText = "删除",
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
private fun CategoryListItem(
    category: CategoryWithCount,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.1f) else Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                SelectableCheckbox(
                    selected = isSelected,
                    onSelectedChange = { onClick() },
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${category.recipeCount} 个菜谱",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = dateFormat.format(Date(category.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray
                    )
                }
            }
            
            if (!isSelectionMode) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MediumGray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Error)
                }
            }
        }
    }
}
