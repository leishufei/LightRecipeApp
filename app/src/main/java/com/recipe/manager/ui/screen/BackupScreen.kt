package com.recipe.manager.ui.screen

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.recipe.manager.data.database.RecipeDatabase
import com.recipe.manager.data.entity.*
import com.recipe.manager.ui.components.*
import com.recipe.manager.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupData(
    val version: Int = 1,
    val exportTime: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<Step> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    
    // 导出文件选择器
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    val backupData = exportData(context)
                    val json = gson.toJson(backupData)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isExporting = false
                }
            }
        }
    }
    
    // 导入文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            showImportConfirm = true
        }
    }
    
    fun performImport(uri: android.net.Uri) {
        scope.launch {
            isImporting = true
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                } ?: throw Exception("无法读取文件")
                
                val backupData = gson.fromJson(json, BackupData::class.java)
                importData(context, backupData)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isImporting = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            RecipeTopBar(
                title = "备份与恢复",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 说明卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "数据备份说明",
                            style = MaterialTheme.typography.titleSmall,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• 导出功能会将所有分类和菜谱数据保存为 JSON 文件\n" +
                                   "• 导入功能会覆盖现有数据，请谨慎操作\n" +
                                   "• 建议定期备份，防止数据丢失",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkGray
                        )
                    }
                }
            }
            
            // 导出按钮
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
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
                            .background(Secondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "导出数据",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "将菜谱数据导出为 JSON 文件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MediumGray
                        )
                    }
                    Button(
                        onClick = {
                            val fileName = "recipe_backup_${System.currentTimeMillis()}.json"
                            exportLauncher.launch(fileName)
                        },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = OnSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("导出")
                        }
                    }
                }
            }
            
            // 导入按钮
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
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
                            .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "导入数据",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "从 JSON 文件恢复菜谱数据",
                            style = MaterialTheme.typography.bodySmall,
                            color = MediumGray
                        )
                    }
                    Button(
                        onClick = { importLauncher.launch("application/json") },
                        enabled = !isImporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = OnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("导入")
                        }
                    }
                }
            }
        }
    }
    
    // 导入确认对话框
    if (showImportConfirm) {
        ConfirmDialog(
            title = "确认导入",
            message = "导入数据将覆盖现有的所有分类和菜谱，此操作不可撤销。确定要继续吗？",
            confirmText = "确定导入",
            onConfirm = {
                showImportConfirm = false
                pendingImportUri?.let { performImport(it) }
                pendingImportUri = null
            },
            onDismiss = {
                showImportConfirm = false
                pendingImportUri = null
            }
        )
    }
}

private suspend fun exportData(context: Context): BackupData = withContext(Dispatchers.IO) {
    val db = RecipeDatabase.getDatabase(context)
    
    // 使用 suspend 函数获取数据
    val categoriesFlow = db.categoryDao().getAllCategories()
    var categories = emptyList<Category>()
    categoriesFlow.collect { 
        categories = it 
        return@collect
    }
    
    val recipesFlow = db.recipeDao().getAllRecipes()
    var recipes = emptyList<Recipe>()
    recipesFlow.collect { 
        recipes = it 
        return@collect
    }
    
    val allIngredients = mutableListOf<Ingredient>()
    val allSteps = mutableListOf<Step>()
    
    recipes.forEach { recipe ->
        val ingredients = db.ingredientDao().getIngredientsByRecipeSync(recipe.id)
        val steps = db.stepDao().getStepsByRecipeSync(recipe.id)
        allIngredients.addAll(ingredients)
        allSteps.addAll(steps)
    }
    
    BackupData(
        categories = categories,
        recipes = recipes,
        ingredients = allIngredients,
        steps = allSteps
    )
}

private suspend fun importData(context: Context, data: BackupData) = withContext(Dispatchers.IO) {
    val db = RecipeDatabase.getDatabase(context)
    
    // 创建 ID 映射（旧ID -> 新ID）
    val categoryIdMap = mutableMapOf<Long, Long>()
    val recipeIdMap = mutableMapOf<Long, Long>()
    
    // 导入分类
    data.categories.forEach { category ->
        val newId = db.categoryDao().insert(category.copy(id = 0))
        categoryIdMap[category.id] = newId
    }
    
    // 导入菜谱（更新分类ID）
    data.recipes.forEach { recipe ->
        val newCategoryId = categoryIdMap[recipe.categoryId] ?: recipe.categoryId
        val newId = db.recipeDao().insert(recipe.copy(id = 0, categoryId = newCategoryId))
        recipeIdMap[recipe.id] = newId
    }
    
    // 导入用料（更新菜谱ID）
    data.ingredients.forEach { ingredient ->
        val newRecipeId = recipeIdMap[ingredient.recipeId] ?: ingredient.recipeId
        db.ingredientDao().insert(ingredient.copy(id = 0, recipeId = newRecipeId))
    }
    
    // 导入步骤（更新菜谱ID）
    data.steps.forEach { step ->
        val newRecipeId = recipeIdMap[step.recipeId] ?: step.recipeId
        db.stepDao().insert(step.copy(id = 0, recipeId = newRecipeId))
    }
}
