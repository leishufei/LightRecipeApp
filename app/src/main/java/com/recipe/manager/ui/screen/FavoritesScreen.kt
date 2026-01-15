package com.recipe.manager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.recipe.manager.data.entity.Recipe
import com.recipe.manager.data.entity.RecipeWithCategory
import com.recipe.manager.ui.components.*
import com.recipe.manager.ui.navigation.Screen
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.viewmodel.RecipeViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: RecipeViewModel
) {
    // 直接从 listUiState 过滤收藏的菜谱
    val uiState by viewModel.listUiState.collectAsState()
    val favoriteRecipes = uiState.recipes.filter { it.recipe.isFavorite }
    
    Scaffold(
        topBar = {
            RecipeTopBar(
                title = "我的收藏",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { padding ->
        if (favoriteRecipes.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MediumGray
                    )
                },
                message = "还没有收藏的菜谱"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Background),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(favoriteRecipes, key = { it.recipe.id }) { recipe ->
                    RecipeListItem(
                        recipe = recipe,
                        onClick = {
                            navController.navigate(Screen.RecipeDetail.createRoute(recipe.recipe.id))
                        }
                    )
                }
            }
        }
    }
}
