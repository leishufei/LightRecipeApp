package com.recipe.manager.ui.screen

import androidx.compose.foundation.background
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
import com.recipe.manager.data.entity.Recipe
import com.recipe.manager.data.entity.RecipeWithCategory
import com.recipe.manager.ui.components.*
import com.recipe.manager.ui.navigation.Screen
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: RecipeViewModel
) {
    var searchText by remember { mutableStateOf("") }
    val searchResults by viewModel.listUiState.collectAsState()
    
    LaunchedEffect(searchText) {
        viewModel.setSearchKeyword(searchText)
    }
    
    // 退出时清除搜索
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setSearchKeyword("")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("搜索菜谱名称或用料", color = OnPrimary.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnPrimary,
                            unfocusedTextColor = OnPrimary,
                            cursorColor = OnPrimary,
                            focusedBorderColor = OnPrimary.copy(alpha = 0.5f),
                            unfocusedBorderColor = OnPrimary.copy(alpha = 0.3f)
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = OnPrimary.copy(alpha = 0.7f))
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(Icons.Default.Clear, "清除", tint = OnPrimary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = OnPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        }
    ) { padding ->
        if (searchText.isBlank()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MediumGray
                    )
                },
                message = "输入关键词搜索菜谱"
            )
        } else if (searchResults.recipes.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MediumGray
                    )
                },
                message = "未找到相关菜谱"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Background),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = "找到 ${searchResults.recipes.size} 个结果",
                        style = MaterialTheme.typography.labelMedium,
                        color = MediumGray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(searchResults.recipes, key = { it.recipe.id }) { recipe ->
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
