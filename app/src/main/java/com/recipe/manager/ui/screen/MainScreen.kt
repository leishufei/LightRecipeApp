package com.recipe.manager.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.recipe.manager.data.entity.RecipeWithCategory
import com.recipe.manager.ui.components.RecipeImage
import com.recipe.manager.ui.navigation.Screen
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.viewmodel.CategoryViewModel
import com.recipe.manager.ui.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    navController: NavController,
    categoryViewModel: CategoryViewModel,
    recipeViewModel: RecipeViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> RecipeMainContent(
                    navController = navController,
                    categoryViewModel = categoryViewModel,
                    recipeViewModel = recipeViewModel
                )
                1 -> ProfileScreen(navController = navController)
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.height(64.dp)
    ) {
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.Restaurant, 
                    contentDescription = "菜谱",
                    modifier = Modifier.size(24.dp)
                ) 
            },
            label = { 
                Text(
                    "菜谱",
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                ) 
            },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF00C853),
                selectedTextColor = Color(0xFF00C853),
                unselectedIconColor = Color(0xFF9E9E9E),
                unselectedTextColor = Color(0xFF9E9E9E),
                indicatorColor = Color(0xFFE8F5E9)
            )
        )
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.Person, 
                    contentDescription = "我的",
                    modifier = Modifier.size(24.dp)
                ) 
            },
            label = { 
                Text(
                    "我的",
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                ) 
            },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF00C853),
                selectedTextColor = Color(0xFF00C853),
                unselectedIconColor = Color(0xFF9E9E9E),
                unselectedTextColor = Color(0xFF9E9E9E),
                indicatorColor = Color(0xFFE8F5E9)
            )
        )
    }
}

@Composable
private fun RecipeMainContent(
    navController: NavController,
    categoryViewModel: CategoryViewModel,
    recipeViewModel: RecipeViewModel
) {
    val categoryState by categoryViewModel.uiState.collectAsState()
    val recipeState by recipeViewModel.listUiState.collectAsState()
    
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 按分类分组所有菜谱
    val groupedRecipes = remember(recipeState.recipes, categoryState.categories) {
        categoryState.categories.map { category ->
            category to recipeState.recipes.filter { it.recipe.categoryId == category.id }
        }
    }
    
    // 计算每个分类在列表中的起始索引
    val categoryStartIndices = remember(groupedRecipes) {
        val indices = mutableMapOf<Long, Int>()
        var currentIndex = 0
        groupedRecipes.forEach { (category, recipes) ->
            if (recipes.isNotEmpty()) {
                indices[category.id] = currentIndex
                currentIndex += 1 + recipes.size // 1个标题 + N个菜谱
            }
        }
        indices
    }
    
    // 监听滚动位置，自动切换分类
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visibleIndex = listState.firstVisibleItemIndex
        var accumulatedIndex = 0
        
        for ((category, recipes) in groupedRecipes) {
            if (recipes.isEmpty()) continue
            
            val categoryItemCount = 1 + recipes.size // 标题 + 菜谱
            if (visibleIndex >= accumulatedIndex && visibleIndex < accumulatedIndex + categoryItemCount) {
                if (selectedCategoryId != category.id) {
                    selectedCategoryId = category.id
                }
                break
            }
            accumulatedIndex += categoryItemCount
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部区域
        TopHeaderSection(
            navController = navController,
            recipeCount = recipeState.recipes.size
        )
        
        // 中间内容区域
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // 左侧分类列表
            CategorySideBar(
                categories = categoryState.categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { categoryId ->
                    selectedCategoryId = categoryId
                    // 滚动到对应分类的起始位置
                    categoryStartIndices[categoryId]?.let { startIndex ->
                        scope.launch {
                            listState.animateScrollToItem(startIndex)
                        }
                    }
                },
                onManageCategories = {
                    navController.navigate(Screen.CategoryList.route)
                },
                modifier = Modifier.width(80.dp)
            )
            
            // 右侧菜谱列表（带浮动按钮）
            Box(modifier = Modifier.weight(1f)) {
                RecipeContentArea(
                    groupedRecipes = groupedRecipes,
                    listState = listState,
                    onRecipeClick = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    },
                    onEditClick = { recipeId ->
                        navController.navigate(Screen.RecipeEdit.createRoute(recipeId))
                    },
                    onDeleteClick = { recipeId ->
                        recipeViewModel.deleteRecipe(recipeId)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // 右下角浮动添加按钮
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.RecipeEdit.createRoute()) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加菜谱",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopHeaderSection(
    navController: NavController,
    recipeCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00C853),
                        Color(0xFF00E676)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：头像和厨房名称
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "我的厨房",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "共 $recipeCount 道菜谱",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            
            // 右侧：搜索按钮
            IconButton(
                onClick = { navController.navigate(Screen.Search.route) },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CategorySideBar(
    categories: List<com.recipe.manager.data.entity.CategoryWithCount>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    onManageCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.White)
            .padding(top = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { category ->
                CategorySideItem(
                    name = category.name,
                    count = category.recipeCount,
                    isSelected = selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) }
                )
            }
        }
        
        // 分类管理按钮
        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onManageCategories)
                .background(Color.White)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "分类管理",
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "管理",
                    fontSize = 11.sp,
                    color = Color(0xFF00C853),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CategorySideItem(
    name: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color(0xFFE8F5E9) else Color.Transparent
            )
            .padding(vertical = 16.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF00C853) else Color(0xFF616161),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$count",
                    fontSize = 11.sp,
                    color = if (isSelected) Color(0xFF00C853) else Color(0xFF9E9E9E),
                    fontWeight = FontWeight.Normal
                )
            }
        }
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .background(
                        Color(0xFF00C853),
                        RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                    )
                    .align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun RecipeContentArea(
    groupedRecipes: List<Pair<com.recipe.manager.data.entity.CategoryWithCount, List<RecipeWithCategory>>>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRecipeClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        groupedRecipes.forEach { (category, recipes) ->
            if (recipes.isEmpty()) return@forEach
            
            // 分类标题
            item(key = "category_${category.id}") {
                Text(
                    text = "${category.name} (${recipes.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
            
            // 该分类下的所有菜谱
            items(
                items = recipes,
                key = { recipe -> "recipe_${recipe.recipe.id}" }
            ) { recipe ->
                RecipeCardItem(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.recipe.id) },
                    onEditClick = { onEditClick(recipe.recipe.id) },
                    onDeleteClick = { onDeleteClick(recipe.recipe.id) }
                )
            }
        }
        
        // 如果没有任何菜谱，显示空状态
        if (groupedRecipes.all { it.second.isEmpty() }) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暂无菜谱", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "点击右下角按钮添加菜谱",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCardItem(
    recipe: RecipeWithCategory,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 菜谱图片
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                RecipeImage(
                    imagePath = recipe.recipe.coverImagePath,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 菜谱信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recipe.recipe.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 操作按钮（图标形式）
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE8F5E9), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFFEBEE), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // 顶部个人信息区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00C853),
                            Color(0xFF00E676)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "我的厨房",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 功能列表
        ProfileMenuItem(
            icon = Icons.Default.Favorite,
            title = "我的收藏",
            subtitle = "查看收藏的菜谱",
            onClick = { navController.navigate(Screen.Favorites.route) }
        )
        
        ProfileMenuItem(
            icon = Icons.Default.Category,
            title = "分类管理",
            subtitle = "管理菜谱分类",
            onClick = { navController.navigate(Screen.CategoryList.route) }
        )
        
        ProfileMenuItem(
            icon = Icons.Default.Backup,
            title = "备份与恢复",
            subtitle = "数据备份和恢复",
            onClick = { navController.navigate(Screen.Backup.route) }
        )
        
        ProfileMenuItem(
            icon = Icons.Default.Settings,
            title = "设置",
            subtitle = "应用设置",
            onClick = { /* TODO */ }
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
