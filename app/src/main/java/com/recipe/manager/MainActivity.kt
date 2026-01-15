package com.recipe.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.recipe.manager.ui.navigation.RecipeNavGraph
import com.recipe.manager.ui.theme.RecipeManagerTheme
import com.recipe.manager.ui.viewmodel.CategoryViewModel
import com.recipe.manager.ui.viewmodel.RecipeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val app = application as RecipeApplication
        
        setContent {
            RecipeManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    val categoryViewModel: CategoryViewModel = viewModel(
                        factory = CategoryViewModel.Factory(app.categoryRepository)
                    )
                    
                    val recipeViewModel: RecipeViewModel = viewModel(
                        factory = RecipeViewModel.Factory(app.recipeRepository)
                    )
                    
                    RecipeNavGraph(
                        navController = navController,
                        categoryViewModel = categoryViewModel,
                        recipeViewModel = recipeViewModel
                    )
                }
            }
        }
    }
}
