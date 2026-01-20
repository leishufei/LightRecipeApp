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
import kotlinx.coroutines.flow.first
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
                                   "• 导入时会根据名称匹配，新数据会覆盖旧数据\n" +
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
            message = "导入时会根据名称匹配现有数据，较新的数据会覆盖较旧的数据。确定要继续吗？",
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
    
    // 使用 first() 获取数据
    val categories = db.categoryDao().getAllCategories().first()
    val recipes = db.recipeDao().getAllRecipes().first()
    
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
    
    // 获取现有数据
    val existingCategories = db.categoryDao().getAllCategories().first()
    val existingRecipes = db.recipeDao().getAllRecipes().first()
    
    // 创建 ID 映射（旧ID -> 新ID）
    val categoryIdMap = mutableMapOf<Long, Long>()
    val recipeIdMap = mutableMapOf<Long, Long>()
    
    // 导入分类（根据名称匹配，根据修改时间决定是否覆盖）
    data.categories.forEach { importCategory ->
        val existingCategory = existingCategories.find { it.name == importCategory.name }
        
        if (existingCategory != null) {
            // 已存在同名分类，比较修改时间
            if (importCategory.updatedAt > existingCategory.updatedAt) {
                // 导入的数据更新，覆盖
                db.categoryDao().update(existingCategory.copy(
                    name = importCategory.name,
                    updatedAt = importCategory.updatedAt
                ))
            }
            // 使用现有分类的ID
            categoryIdMap[importCategory.id] = existingCategory.id
        } else {
            // 不存在，新增
            val newId = db.categoryDao().insert(importCategory.copy(id = 0))
            categoryIdMap[importCategory.id] = newId
        }
    }
    
    // 导入菜谱（根据名称和分类匹配，根据修改时间决定是否覆盖）
    data.recipes.forEach { importRecipe ->
        val newCategoryId = categoryIdMap[importRecipe.categoryId] ?: importRecipe.categoryId
        val existingRecipe = existingRecipes.find { 
            it.name == importRecipe.name && it.categoryId == newCategoryId 
        }
        
        if (existingRecipe != null) {
            // 已存在同名菜谱，比较修改时间
            if (importRecipe.updatedAt > existingRecipe.updatedAt) {
                // 导入的数据更新，覆盖
                db.recipeDao().update(existingRecipe.copy(
                    name = importRecipe.name,
                    coverImagePath = importRecipe.coverImagePath,
                    isFavorite = importRecipe.isFavorite,
                    clickCount = importRecipe.clickCount,
                    updatedAt = importRecipe.updatedAt
                ))
                
                // 删除旧的用料和步骤
                db.ingredientDao().deleteByRecipeId(existingRecipe.id)
                db.stepDao().deleteByRecipeId(existingRecipe.id)
                
                // 导入新的用料
                data.ingredients.filter { it.recipeId == importRecipe.id }.forEach { ingredient ->
                    db.ingredientDao().insert(ingredient.copy(id = 0, recipeId = existingRecipe.id))
                }
                
                // 导入新的步骤
                data.steps.filter { it.recipeId == importRecipe.id }.forEach { step ->
                    db.stepDao().insert(step.copy(id = 0, recipeId = existingRecipe.id))
                }
            }
            // 使用现有菜谱的ID
            recipeIdMap[importRecipe.id] = existingRecipe.id
        } else {
            // 不存在，新增
            val newId = db.recipeDao().insert(importRecipe.copy(id = 0, categoryId = newCategoryId))
            recipeIdMap[importRecipe.id] = newId
            
            // 导入用料
            data.ingredients.filter { it.recipeId == importRecipe.id }.forEach { ingredient ->
                db.ingredientDao().insert(ingredient.copy(id = 0, recipeId = newId))
            }
            
            // 导入步骤
            data.steps.filter { it.recipeId == importRecipe.id }.forEach { step ->
                db.stepDao().insert(step.copy(id = 0, recipeId = newId))
            }
        }
    }
}
