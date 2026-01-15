package com.recipe.manager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.recipe.manager.data.entity.CategoryWithCount
import com.recipe.manager.data.entity.RecipeWithCategory
import com.recipe.manager.ui.components.*
import com.recipe.manager.ui.navigation.Screen
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.viewmodel.CategoryViewModel
import com.recipe.manager.ui.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    categoryViewModel: CategoryViewModel,
    recipeViewModel: RecipeViewModel
) {
    val categoryState by categoryViewModel.uiState.collectAsState()
    val recipeState by recipeViewModel.listUiState.collectAsState()
    
    Scaffold(
        topBar = {
            RecipeTopBar(
                title = "菜谱管理",
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Favorites.route) }) {
                        Icon(Icons.Default.Favorite, contentDescription = "收藏")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.RecipeEdit.createRoute()) },
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增菜谱", tint = OnPrimary)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            // 快捷功能区
            item {
                QuickActions(navController)
            }
            
            // 分类区域
            item {
                CategorySection(
                    categories = categoryState.categories,
                    onCategoryClick = { categoryId ->
                        navController.navigate(Screen.RecipeList.createRoute(categoryId))
                    },
                    onManageClick = { navController.navigate(Screen.CategoryList.route) }
                )
            }
            
            // 热门菜谱
            item {
                Text(
                    text = "热门菜谱",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            if (recipeState.recipes.isEmpty()) {
                item {
                    EmptyState(
                        icon = {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MediumGray
                            )
                        },
                        message = "还没有菜谱，快去添加吧",
                        actionText = "添加菜谱",
                        onAction = { navController.navigate(Screen.RecipeEdit.createRoute()) }
                    )
                }
            } else {
                items(recipeState.recipes.take(10)) { recipe ->
                    RecipeListItem(
                        recipe = recipe,
                        onClick = { 
                            navController.navigate(Screen.RecipeDetail.createRoute(recipe.recipe.id)) 
                        }
                    )
                }
                
                if (recipeState.recipes.size > 10) {
                    item {
                        TextButton(
                            onClick = { navController.navigate(Screen.RecipeList.createRoute()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("查看全部菜谱", color = Primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionItem(
            icon = Icons.Default.Category,
            label = "分类管理",
            onClick = { navController.navigate(Screen.CategoryList.route) }
        )
        QuickActionItem(
            icon = Icons.Default.MenuBook,
            label = "全部菜谱",
            onClick = { navController.navigate(Screen.RecipeList.createRoute()) }
        )
        QuickActionItem(
            icon = Icons.Default.Backup,
            label = "备份恢复",
            onClick = { navController.navigate(Screen.Backup.route) }
        )
    }
}

@Composable
private fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Primary, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CategorySection(
    categories: List<CategoryWithCount>,
    onCategoryClick: (Long) -> Unit,
    onManageClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "菜谱分类", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onManageClick) {
                Text("管理", color = Primary)
            }
        }
        
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无分类", color = MediumGray)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        category = category,
                        onClick = { onCategoryClick(category.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: CategoryWithCount,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge,
                color = Primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "(${category.recipeCount})",
                style = MaterialTheme.typography.labelSmall,
                color = MediumGray
            )
        }
    }
}

@Composable
fun RecipeListItem(
    recipe: RecipeWithCategory,
    onClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                SelectableCheckbox(
                    selected = isSelected,
                    onSelectedChange = { onSelectionChange?.invoke(it) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            RecipeImage(
                imagePath = recipe.recipe.coverImagePath,
                modifier = Modifier.size(72.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    recipe.category?.let {
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MediumGray
                    )
                    Text(
                        text = " ${recipe.recipe.clickCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray
                    )
                }
            }
            
            if (recipe.recipe.isFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "已收藏",
                    tint = Error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
