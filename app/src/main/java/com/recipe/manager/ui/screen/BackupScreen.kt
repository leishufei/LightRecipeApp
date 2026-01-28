package com.recipe.manager.ui.screen

import android.content.Context
import android.util.Base64
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
import com.recipe.manager.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * 备份数据结构（含图片 Base64）
 */
data class BackupData(
    val version: Int = 2,
    val exportTime: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val recipes: List<RecipeBackup> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<StepBackup> = emptyList()
)

/**
 * 菜谱备份（含封面图 Base64）
 */
data class RecipeBackup(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val coverImageBase64: String? = null,
    val clickCount: Int = 0,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 步骤备份（含步骤图 Base64）
 */
data class StepBackup(
    val id: Long,
    val recipeId: Long,
    val description: String,
    val imageBase64: String? = null,
    val stepNumber: Int,
    val sortOrder: Int = 0
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
    
    val gson = remember { GsonBuilder().create() }
    
    // 导出文件选择器
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    val backupData = exportDataWithImages(context)
                    val json = gson.toJson(backupData)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
                importDataWithImages(context, backupData)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                            text = "• 导出会将所有数据和图片保存为 JSON 文件\n" +
                                   "• 导入时会根据名称匹配，新数据覆盖旧数据\n" +
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
                            text = "将菜谱数据和图片导出为 JSON 文件",
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

/**
 * 导出数据（含图片 Base64）
 */
private suspend fun exportDataWithImages(context: Context): BackupData = withContext(Dispatchers.IO) {
    val db = RecipeDatabase.getDatabase(context)
    
    val categories = db.categoryDao().getAllCategories().first()
    val recipes = db.recipeDao().getAllRecipes().first()
    
    val allIngredients = mutableListOf<Ingredient>()
    val allSteps = mutableListOf<StepBackup>()
    
    val recipeBackups = recipes.map { recipe ->
        val ingredients = db.ingredientDao().getIngredientsByRecipeSync(recipe.id)
        val steps = db.stepDao().getStepsByRecipeSync(recipe.id)
        allIngredients.addAll(ingredients)
        
        // 转换步骤，包含图片 Base64
        val stepBackups = steps.map { step ->
            StepBackup(
                id = step.id,
                recipeId = step.recipeId,
                description = step.description,
                imageBase64 = encodeImageToBase64(step.imagePath),
                stepNumber = step.stepNumber,
                sortOrder = step.sortOrder
            )
        }
        allSteps.addAll(stepBackups)
        
        // 转换菜谱，包含封面图 Base64
        RecipeBackup(
            id = recipe.id,
            name = recipe.name,
            categoryId = recipe.categoryId,
            coverImageBase64 = encodeImageToBase64(recipe.coverImagePath),
            clickCount = recipe.clickCount,
            isFavorite = recipe.isFavorite,
            createdAt = recipe.createdAt,
            updatedAt = recipe.updatedAt
        )
    }
    
    BackupData(
        categories = categories,
        recipes = recipeBackups,
        ingredients = allIngredients,
        steps = allSteps
    )
}

/**
 * 导入数据（含图片 Base64）
 */
private suspend fun importDataWithImages(context: Context, data: BackupData) = withContext(Dispatchers.IO) {
    val db = RecipeDatabase.getDatabase(context)
    val currentTime = System.currentTimeMillis()
    
    val existingCategories = db.categoryDao().getAllCategories().first()
    val existingRecipes = db.recipeDao().getAllRecipes().first()
    
    val categoryIdMap = mutableMapOf<Long, Long>()
    
    // 导入分类
    data.categories.forEach { importCategory ->
        val existingCategory = existingCategories.find { it.name == importCategory.name }
        
        if (existingCategory != null) {
            db.categoryDao().update(existingCategory.copy(updatedAt = currentTime))
            categoryIdMap[importCategory.id] = existingCategory.id
        } else {
            val newId = db.categoryDao().insert(importCategory.copy(id = 0))
            categoryIdMap[importCategory.id] = newId
        }
    }
    
    // 导入菜谱
    data.recipes.forEach { importRecipe ->
        val newCategoryId = categoryIdMap[importRecipe.categoryId] ?: importRecipe.categoryId
        val existingRecipe = existingRecipes.find { 
            it.name == importRecipe.name && it.categoryId == newCategoryId 
        }
        
        // 解码封面图
        val coverImagePath = decodeBase64ToImage(context, importRecipe.coverImageBase64, "cover")
        
        if (existingRecipe != null) {
            // 删除旧图片
            ImageUtils.deleteImage(existingRecipe.coverImagePath)
            val oldSteps = db.stepDao().getStepsByRecipeSync(existingRecipe.id)
            oldSteps.forEach { ImageUtils.deleteImage(it.imagePath) }
            
            // 更新菜谱
            db.recipeDao().update(existingRecipe.copy(
                coverImagePath = coverImagePath,
                updatedAt = currentTime
            ))
            
            db.ingredientDao().deleteByRecipeId(existingRecipe.id)
            db.stepDao().deleteByRecipeId(existingRecipe.id)
            
            // 导入用料
            data.ingredients.filter { it.recipeId == importRecipe.id }.forEach { ingredient ->
                db.ingredientDao().insert(ingredient.copy(id = 0, recipeId = existingRecipe.id))
            }
            
            // 导入步骤
            data.steps.filter { it.recipeId == importRecipe.id }.forEach { step ->
                val stepImagePath = decodeBase64ToImage(context, step.imageBase64, "step")
                db.stepDao().insert(Step(
                    id = 0,
                    recipeId = existingRecipe.id,
                    description = step.description,
                    imagePath = stepImagePath,
                    stepNumber = step.stepNumber,
                    sortOrder = step.sortOrder
                ))
            }
        } else {
            // 新增菜谱
            val newId = db.recipeDao().insert(Recipe(
                id = 0,
                name = importRecipe.name,
                categoryId = newCategoryId,
                coverImagePath = coverImagePath,
                clickCount = importRecipe.clickCount,
                isFavorite = importRecipe.isFavorite,
                createdAt = importRecipe.createdAt,
                updatedAt = importRecipe.updatedAt
            ))
            
            // 导入用料
            data.ingredients.filter { it.recipeId == importRecipe.id }.forEach { ingredient ->
                db.ingredientDao().insert(ingredient.copy(id = 0, recipeId = newId))
            }
            
            // 导入步骤
            data.steps.filter { it.recipeId == importRecipe.id }.forEach { step ->
                val stepImagePath = decodeBase64ToImage(context, step.imageBase64, "step")
                db.stepDao().insert(Step(
                    id = 0,
                    recipeId = newId,
                    description = step.description,
                    imagePath = stepImagePath,
                    stepNumber = step.stepNumber,
                    sortOrder = step.sortOrder
                ))
            }
        }
    }
}

/**
 * 将图片文件编码为 Base64
 */
private fun encodeImageToBase64(imagePath: String?): String? {
    if (imagePath.isNullOrBlank()) return null
    return try {
        val file = File(imagePath)
        if (file.exists()) {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * 将 Base64 解码并保存为图片文件
 */
private fun decodeBase64ToImage(context: Context, base64: String?, prefix: String): String? {
    if (base64.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val imageDir = ImageUtils.getImageDir(context)
        val file = File(imageDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
