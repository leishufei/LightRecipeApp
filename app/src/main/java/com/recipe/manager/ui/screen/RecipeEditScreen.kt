package com.recipe.manager.ui.screen

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.recipe.manager.data.entity.Category
import com.recipe.manager.data.entity.Ingredient
import com.recipe.manager.data.entity.Recipe
import com.recipe.manager.data.entity.Step
import com.recipe.manager.ui.components.*
import com.recipe.manager.ui.navigation.Screen
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.theme.*
import com.recipe.manager.ui.viewmodel.CategoryViewModel
import com.recipe.manager.ui.viewmodel.RecipeViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class IngredientInput(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var amount: String = ""
)

data class StepInput(
    val id: String = UUID.randomUUID().toString(),
    var description: String = "",
    var imagePath: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    navController: NavController,
    recipeViewModel: RecipeViewModel,
    categoryViewModel: CategoryViewModel,
    recipeId: Long? = null
) {
    val context = LocalContext.current
    val categoryState by categoryViewModel.uiState.collectAsState()
    val detailState by recipeViewModel.detailUiState.collectAsState()
    
    var recipeName by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var coverImagePath by remember { mutableStateOf<String?>(null) }
    var ingredients by remember { mutableStateOf(listOf(IngredientInput())) }
    var steps by remember { mutableStateOf(listOf(StepInput())) }
    var originalRecipe by remember { mutableStateOf<Recipe?>(null) }
    
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var currentStepIndex by remember { mutableStateOf(-1) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    // 折叠状态
    var isIngredientsExpanded by remember { mutableStateOf(false) }
    var isStepsExpanded by remember { mutableStateOf(false) }
    
    val isEditMode = recipeId != null
    
    // 加载编辑数据
    LaunchedEffect(recipeId) {
        if (recipeId != null) {
            recipeViewModel.loadRecipeDetail(recipeId)
        }
    }
    
    // 填充编辑数据
    LaunchedEffect(detailState.recipeWithDetails) {
        if (isEditMode && detailState.recipeWithDetails != null) {
            val details = detailState.recipeWithDetails!!
            originalRecipe = details.recipe
            recipeName = details.recipe.name
            selectedCategoryId = details.recipe.categoryId
            coverImagePath = details.recipe.coverImagePath
            ingredients = if (details.ingredients.isEmpty()) {
                listOf(IngredientInput())
            } else {
                details.ingredients.map { 
                    IngredientInput(name = it.name, amount = it.amount) 
                }
            }
            steps = if (details.steps.isEmpty()) {
                listOf(StepInput())
            } else {
                details.steps.sortedBy { it.stepNumber }.map { 
                    StepInput(description = it.description, imagePath = it.imagePath) 
                }
            }
        }
    }
    
    // 图片选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = saveImageToPrivate(context, it)
            if (currentStepIndex == -1) {
                coverImagePath = path
            } else {
                steps = steps.toMutableList().also { list ->
                    list[currentStepIndex] = list[currentStepIndex].copy(imagePath = path)
                }
            }
        }
        showImagePicker = false
    }
    
    // 相机拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            val path = saveImageToPrivate(context, tempPhotoUri!!)
            if (currentStepIndex == -1) {
                coverImagePath = path
            } else {
                steps = steps.toMutableList().also { list ->
                    list[currentStepIndex] = list[currentStepIndex].copy(imagePath = path)
                }
            }
        }
        showImagePicker = false
    }
    
    // 相机权限
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = createImageFile(context)
            tempPhotoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(tempPhotoUri)
        } else {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun validateAndSave() {
        if (recipeName.isBlank()) {
            Toast.makeText(context, "请输入菜谱名称", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCategoryId == null) {
            Toast.makeText(context, "请选择分类", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 用料和步骤改为可选，只过滤有效数据
        val validIngredients = ingredients.filter { it.name.isNotBlank() && it.amount.isNotBlank() }
        val validSteps = steps.filter { it.description.isNotBlank() }
        
        val recipe = if (isEditMode && originalRecipe != null) {
            // 编辑模式：保留原有的创建时间、点击次数和收藏状态
            originalRecipe!!.copy(
                name = recipeName.trim(),
                categoryId = selectedCategoryId!!,
                coverImagePath = coverImagePath
            )
        } else {
            // 新增模式：创建新的Recipe
            Recipe(
                id = 0,
                name = recipeName.trim(),
                categoryId = selectedCategoryId!!,
                coverImagePath = coverImagePath
            )
        }
        
        val ingredientEntities = validIngredients.map {
            Ingredient(name = it.name.trim(), amount = it.amount.trim(), recipeId = 0)
        }
        val stepEntities = validSteps.map {
            Step(description = it.description.trim(), imagePath = it.imagePath, recipeId = 0, stepNumber = 0)
        }
        
        if (isEditMode) {
            recipeViewModel.updateRecipe(recipe, ingredientEntities, stepEntities) { success, msg ->
                if (success) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            recipeViewModel.addRecipe(recipe, ingredientEntities, stepEntities) { success, msg, id ->
                if (success) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            RecipeTopBar(
                title = if (isEditMode) "编辑菜谱" else "新增菜谱",
                onBackClick = { navController.popBackStack() },
                actions = {
                    TextButton(onClick = { validateAndSave() }) {
                        Text("保存", color = OnPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 封面图
            item {
                CoverImageSection(
                    imagePath = coverImagePath,
                    onPickImage = {
                        currentStepIndex = -1
                        showImagePicker = true
                    }
                )
            }
            
            // 基础信息
            item {
                OutlinedTextField(
                    value = recipeName,
                    onValueChange = { if (it.length <= 20) recipeName = it },
                    label = { Text("名称 *", fontSize = 13.sp) },
                    placeholder = { Text("请输入菜谱名称", fontSize = 13.sp, color = MediumGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Primary
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
            }
            
            // 分类选择
            item {
                CategorySelector(
                    categories = categoryState.categories.map { 
                        Category(it.id, it.name, it.createdAt, it.updatedAt) 
                    },
                    selectedId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it },
                    showPicker = showCategoryPicker,
                    onShowPickerChange = { showCategoryPicker = it }
                )
            }
            
            // 用料
            item {
                CollapsibleSectionHeader(
                    title = "用料",
                    isExpanded = isIngredientsExpanded,
                    onToggle = { isIngredientsExpanded = !isIngredientsExpanded },
                    onAdd = if (isIngredientsExpanded) {
                        { ingredients = ingredients + IngredientInput() }
                    } else null
                )
            }
            
            if (isIngredientsExpanded) {
                itemsIndexed(ingredients, key = { _, item -> item.id }) { index, ingredient ->
                    IngredientInputItem(
                        ingredient = ingredient,
                        onNameChange = { name ->
                            ingredients = ingredients.toMutableList().also {
                                it[index] = it[index].copy(name = name)
                            }
                        },
                        onAmountChange = { amount ->
                            ingredients = ingredients.toMutableList().also {
                                it[index] = it[index].copy(amount = amount)
                            }
                        },
                        onDelete = {
                            if (ingredients.size > 1) {
                                ingredients = ingredients.toMutableList().also { it.removeAt(index) }
                            }
                        }
                    )
                }
            }
            
            // 步骤
            item {
                CollapsibleSectionHeader(
                    title = "步骤",
                    isExpanded = isStepsExpanded,
                    onToggle = { isStepsExpanded = !isStepsExpanded },
                    onAdd = if (isStepsExpanded) {
                        { steps = steps + StepInput() }
                    } else null
                )
            }
            
            if (isStepsExpanded) {
                itemsIndexed(steps, key = { _, item -> item.id }) { index, step ->
                    StepInputItem(
                        step = step,
                        index = index + 1,
                        onDescriptionChange = { desc ->
                            steps = steps.toMutableList().also {
                                it[index] = it[index].copy(description = desc)
                            }
                        },
                        onPickImage = {
                            currentStepIndex = index
                            showImagePicker = true
                        },
                        onDelete = {
                            if (steps.size > 1) {
                                steps = steps.toMutableList().also { it.removeAt(index) }
                            }
                        }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
    
    // 图片选择对话框
    if (showImagePicker) {
        ImagePickerDialog(
            onDismiss = { showImagePicker = false },
            onCameraClick = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGalleryClick = {
                galleryLauncher.launch("image/*")
            }
        )
    }
}

@Composable
private fun CoverImageSection(
    imagePath: String?,
    onPickImage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onPickImage),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (imagePath != null && File(imagePath).exists()) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = "封面图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, "更换", tint = OnPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("更换", color = OnPrimary, fontSize = 12.sp)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "添加封面",
                        modifier = Modifier.size(40.dp),
                        tint = MediumGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击添加封面图", color = MediumGray, fontSize = 13.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    showPicker: Boolean,
    onShowPickerChange: (Boolean) -> Unit
) {
    val selectedCategory = categories.find { it.id == selectedId }
    
    ExposedDropdownMenuBox(
        expanded = showPicker,
        onExpandedChange = { onShowPickerChange(it) }
    ) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("分类 *", fontSize = 13.sp) },
            placeholder = { Text("请选择分类", fontSize = 13.sp, color = MediumGray) },
            trailingIcon = { 
                Box(
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onShowPickerChange(!showPicker)
                    }
                ) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPicker)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = LightGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onShowPickerChange(!showPicker)
                }
        )
        
        ExposedDropdownMenu(
            expanded = showPicker,
            onDismissRequest = { onShowPickerChange(false) }
        ) {
            if (categories.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("暂无分类，请先添加", fontSize = 13.sp, color = MediumGray) },
                    onClick = { onShowPickerChange(false) }
                )
            } else {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name, fontSize = 14.sp) },
                        onClick = {
                            onSelect(category.id)
                            onShowPickerChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, "添加", tint = Primary)
        }
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAdd: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onToggle,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isExpanded) Primary.copy(alpha = 0.08f) else Surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = if (isExpanded) Primary else MediumGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = if (isExpanded) Primary else DarkGray
                )
            }
            
            if (isExpanded && onAdd != null) {
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, "添加", tint = Primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun IngredientInputItem(
    ingredient: IngredientInput,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = ingredient.name,
            onValueChange = onNameChange,
            placeholder = { Text("食材名称", fontSize = 13.sp, color = MediumGray) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = LightGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Primary
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
        )
        OutlinedTextField(
            value = ingredient.amount,
            onValueChange = onAmountChange,
            placeholder = { Text("用量", fontSize = 13.sp, color = MediumGray) },
            modifier = Modifier
                .weight(0.6f)
                .height(48.dp),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = LightGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Primary
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.RemoveCircleOutline, "删除", tint = Error, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StepInputItem(
    step: StepInput,
    index: Int,
    onDescriptionChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$index", color = OnPrimary, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("步骤", fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = DarkGray)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, "删除", tint = Error, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = step.description,
                onValueChange = onDescriptionChange,
                placeholder = { Text("描述这一步的操作...", fontSize = 13.sp, color = MediumGray) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Primary
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (step.imagePath != null && File(step.imagePath!!).exists()) {
                Box {
                    AsyncImage(
                        model = File(step.imagePath!!),
                        contentDescription = "步骤图片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(onClick = onPickImage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, "更换", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MediumGray),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(LightGray)
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("添加步骤图片", fontSize = 13.sp)
                }
            }
        }
    }
}

private fun createImageFile(context: Context): File {
    val imageDir = File(context.filesDir, "images")
    if (!imageDir.exists()) imageDir.mkdirs()
    return File(imageDir, "IMG_${System.currentTimeMillis()}.jpg")
}

private fun saveImageToPrivate(context: Context, uri: Uri): String? {
    return try {
        val imageDir = File(context.filesDir, "images")
        if (!imageDir.exists()) imageDir.mkdirs()
        val file = File(imageDir, "IMG_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
