package com.recipe.manager.ui.screen

import android.app.Activity
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.recipe.manager.data.entity.Ingredient
import com.recipe.manager.data.entity.Step
import com.recipe.manager.ui.components.*
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.viewmodel.RecipeViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    navController: NavController,
    viewModel: RecipeViewModel,
    recipeId: Long
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.detailUiState.collectAsState()
    
    // 步骤完成状态
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }
    
    // 原始亮度
    var originalBrightness by remember { mutableStateOf(-1f) }
    
    // WakeLock
    var wakeLock by remember { mutableStateOf<PowerManager.WakeLock?>(null) }
    
    LaunchedEffect(recipeId) {
        viewModel.loadRecipeDetail(recipeId)
    }
    
    // 进入做饭模式：提高亮度、保持屏幕常亮
    DisposableEffect(Unit) {
        activity?.let { act ->
            // 保存原始亮度
            val window = act.window
            originalBrightness = window.attributes.screenBrightness
            
            // 设置高亮度
            val params = window.attributes
            params.screenBrightness = 0.9f
            window.attributes = params
            
            // 保持屏幕常亮
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // 获取 WakeLock
            val powerManager = act.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "RecipeManager:CookingMode"
            ).apply {
                acquire(60 * 60 * 1000L) // 最长1小时
            }
        }
        
        onDispose {
            // 退出时恢复原始亮度
            activity?.let { act ->
                val window = act.window
                val params = window.attributes
                params.screenBrightness = if (originalBrightness >= 0) originalBrightness else -1f
                window.attributes = params
                
                // 移除屏幕常亮标志
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            // 释放 WakeLock
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        }
    }
    
    val recipeWithDetails = uiState.recipeWithDetails
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🍳 ", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = recipeWithDetails?.recipe?.name ?: "做饭模式",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "退出")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Secondary,
                    titleContentColor = OnSecondary,
                    navigationIconContentColor = OnSecondary
                )
            )
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
                    .background(CookingBackground),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 进度提示
                item {
                    val totalSteps = recipeWithDetails.steps.size
                    val completedCount = completedSteps.size
                    CookingProgressCard(
                        completed = completedCount,
                        total = totalSteps
                    )
                }
                
                // 用料卡片
                item {
                    CookingIngredientsCard(ingredients = recipeWithDetails.ingredients)
                }
                
                // 步骤标题
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "烹饪步骤",
                            style = MaterialTheme.typography.titleMedium,
                            color = Secondary
                        )
                    }
                }
                
                // 步骤列表
                itemsIndexed(
                    recipeWithDetails.steps.sortedBy { it.stepNumber }
                ) { index, step ->
                    CookingStepCard(
                        step = step,
                        index = index + 1,
                        isCompleted = completedSteps.contains(index),
                        onToggleComplete = {
                            completedSteps = if (completedSteps.contains(index)) {
                                completedSteps - index
                            } else {
                                completedSteps + index
                            }
                        }
                    )
                }
                
                // 完成提示
                if (completedSteps.size == recipeWithDetails.steps.size && recipeWithDetails.steps.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CookingStepComplete)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "🎉 恭喜完成！享用美食吧~",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Secondary
                                )
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun CookingProgressCard(completed: Int, total: Int) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "烹饪进度",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "$completed / $total 步",
                    style = MaterialTheme.typography.labelLarge,
                    color = Secondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Secondary,
                trackColor = LightGray
            )
        }
    }
}

@Composable
private fun CookingIngredientsCard(ingredients: List<Ingredient>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "所需用料",
                    style = MaterialTheme.typography.titleMedium,
                    color = Secondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ingredients.sortedBy { it.sortOrder }.forEach { ingredient ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ingredient.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = ingredient.amount,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CookingStepCard(
    step: Step,
    index: Int,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleComplete),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) CookingStepComplete else Surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 步骤编号
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Secondary else Primary),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已完成",
                        tint = OnSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) MediumGray else OnSurface
                )
                
                step.imagePath?.let { path ->
                    if (File(path).exists()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AsyncImage(
                            model = File(path),
                            contentDescription = "步骤图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            
            // 完成复选框
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Secondary,
                    uncheckedColor = MediumGray
                )
            )
        }
    }
}
