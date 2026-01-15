package com.recipe.manager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.recipe.manager.data.entity.Ingredient
import com.recipe.manager.data.entity.Step
import com.recipe.manager.ui.components.*
import com.recipe.manager.ui.navigation.Screen
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.viewmodel.RecipeViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    navController: NavController,
    viewModel: RecipeViewModel,
    recipeId: Long
) {
    val uiState by viewModel.detailUiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    LaunchedEffect(recipeId) {
        viewModel.loadRecipeDetail(recipeId)
    }
    
    val recipeWithDetails = uiState.recipeWithDetails
    
    Scaffold(
        topBar = {
            RecipeTopBar(
                title = recipeWithDetails?.recipe?.name ?: "菜谱详情",
                onBackClick = { navController.popBackStack() },
                actions = {
                    recipeWithDetails?.let { details ->
                        IconButton(onClick = {
                            viewModel.toggleFavorite(details.recipe.id, !details.recipe.isFavorite)
                        }) {
                            Icon(
                                if (details.recipe.isFavorite) Icons.Default.Favorite 
                                else Icons.Default.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = if (details.recipe.isFavorite) Error else OnPrimary
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate(Screen.RecipeEdit.createRoute(details.recipe.id))
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            recipeWithDetails?.let {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.CookingMode.createRoute(recipeId)) },
                    containerColor = Secondary,
                    contentColor = OnSecondary
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🍳 做饭模式")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState()
        } else if (recipeWithDetails == null) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MediumGray
                    )
                },
                message = "菜谱不存在"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Background)
            ) {
                // 封面图
                item {
                    RecipeDetailHeader(
                        imagePath = recipeWithDetails.recipe.coverImagePath,
                        clickCount = recipeWithDetails.recipe.clickCount,
                        createdAt = recipeWithDetails.recipe.createdAt
                    )
                }
                
                // 用料
                item {
                    SectionTitle(title = "用料", icon = Icons.Default.ShoppingCart)
                }
                
                item {
                    IngredientsCard(ingredients = recipeWithDetails.ingredients)
                }
                
                // 步骤
                item {
                    SectionTitle(title = "步骤", icon = Icons.Default.FormatListNumbered)
                }
                
                itemsIndexed(recipeWithDetails.steps.sortedBy { it.stepNumber }) { index, step ->
                    StepItem(step = step, index = index + 1)
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除菜谱",
            message = "确定要删除这个菜谱吗？",
            confirmText = "删除",
            onConfirm = {
                recipeWithDetails?.let {
                    viewModel.deleteRecipe(it.recipe) { success, _ ->
                        if (success) {
                            navController.popBackStack()
                        }
                    }
                }
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun RecipeDetailHeader(
    imagePath: String?,
    clickCount: Int,
    createdAt: Long
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
    Column {
        if (imagePath != null && File(imagePath).exists()) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "菜谱封面",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentScale = ContentScale.Crop
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MediumGray
                )
                Text(
                    text = " $clickCount 次浏览",
                    style = MaterialTheme.typography.labelMedium,
                    color = MediumGray
                )
            }
            Text(
                text = "创建于 ${dateFormat.format(Date(createdAt))}",
                style = MaterialTheme.typography.labelMedium,
                color = MediumGray
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Primary
        )
    }
}

@Composable
private fun IngredientsCard(ingredients: List<Ingredient>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ingredients.sortedBy { it.sortOrder }.forEachIndexed { index, ingredient ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ingredient.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = ingredient.amount,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Primary
                    )
                }
                if (index < ingredients.size - 1) {
                    Divider(color = Divider)
                }
            }
        }
    }
}

@Composable
private fun StepItem(step: Step, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                step.imagePath?.let { path ->
                    if (File(path).exists()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = File(path),
                            contentDescription = "步骤图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
