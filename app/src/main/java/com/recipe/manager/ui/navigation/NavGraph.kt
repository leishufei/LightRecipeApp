package com.recipe.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.recipe.manager.ui.screen.*
import com.recipe.manager.ui.viewmodel.CategoryViewModel
import com.recipe.manager.ui.viewmodel.RecipeViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CategoryList : Screen("category_list")
    object RecipeList : Screen("recipe_list?categoryId={categoryId}") {
        fun createRoute(categoryId: Long? = null) = 
            if (categoryId != null) "recipe_list?categoryId=$categoryId" else "recipe_list"
    }
    object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: Long) = "recipe_detail/$recipeId"
    }
    object RecipeEdit : Screen("recipe_edit?recipeId={recipeId}") {
        fun createRoute(recipeId: Long? = null) = 
            if (recipeId != null) "recipe_edit?recipeId=$recipeId" else "recipe_edit"
    }
    object CookingMode : Screen("cooking_mode/{recipeId}") {
        fun createRoute(recipeId: Long) = "cooking_mode/$recipeId"
    }
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Backup : Screen("backup")
}

@Composable
fun RecipeNavGraph(
    navController: NavHostController,
    categoryViewModel: CategoryViewModel,
    recipeViewModel: RecipeViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                categoryViewModel = categoryViewModel,
                recipeViewModel = recipeViewModel
            )
        }
        
        composable(Screen.CategoryList.route) {
            CategoryListScreen(
                navController = navController,
                viewModel = categoryViewModel
            )
        }
        
        composable(
            route = Screen.RecipeList.route,
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId")
            RecipeListScreen(
                navController = navController,
                viewModel = recipeViewModel,
                categoryId = if (categoryId == -1L) null else categoryId
            )
        }
        
        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: return@composable
            RecipeDetailScreen(
                navController = navController,
                viewModel = recipeViewModel,
                recipeId = recipeId
            )
        }
        
        composable(
            route = Screen.RecipeEdit.route,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId")
            RecipeEditScreen(
                navController = navController,
                recipeViewModel = recipeViewModel,
                categoryViewModel = categoryViewModel,
                recipeId = if (recipeId == -1L) null else recipeId
            )
        }
        
        composable(
            route = Screen.CookingMode.route,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: return@composable
            CookingModeScreen(
                navController = navController,
                viewModel = recipeViewModel,
                recipeId = recipeId
            )
        }
        
        composable(Screen.Search.route) {
            SearchScreen(
                navController = navController,
                viewModel = recipeViewModel
            )
        }
        
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                navController = navController,
                viewModel = recipeViewModel
            )
        }
        
        composable(Screen.Backup.route) {
            BackupScreen(navController = navController)
        }
    }
}
