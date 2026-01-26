#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将 recipes.xlsx 转换为 APP 可导入的 JSON 格式
Excel 格式：B列=分类, C列=名称, D列=用料, E列=步骤

封面图放在 tool/cover 目录下，文件名格式：<菜谱名称>.jpg
"""

import json
import time
import re
import base64
import io
from pathlib import Path

try:
    import openpyxl
except ImportError:
    print("请先安装 openpyxl: pip install openpyxl")
    exit(1)

try:
    from PIL import Image
except ImportError:
    print("请先安装 Pillow: pip install Pillow")
    exit(1)

# 图片压缩配置
MAX_IMAGE_SIZE = 600  # 最大宽/高像素
JPEG_QUALITY = 70     # JPEG 压缩质量 (1-100)


def parse_ingredients(text: str) -> list:
    """
    解析用料文本，支持多种格式：
    - 换行分隔
    - 逗号/顿号分隔
    - 格式：食材名 用量 或 食材名:用量
    """
    if not text or not text.strip():
        return []
    
    ingredients = []
    # 按换行或逗号/顿号分隔
    lines = re.split(r'[,，、\n]+', text.strip())
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
        
        # 尝试解析 "食材名 用量" 或 "食材名:用量" 或 "食材名：用量"
        match = re.match(r'^(.+?)[:\s：]+(.+)$', line)
        if match:
            name = match.group(1).strip()
            amount = match.group(2).strip()
        else:
            # 没有用量，整行作为食材名
            name = line
            amount = "适量"
        
        if name:
            ingredients.append({"name": name, "amount": amount})
    
    return ingredients


def parse_steps(text: str) -> list:
    """
    解析步骤文本，支持多种格式：
    - 换行分隔
    - 数字编号（1. 2. 或 1、2、）
    """
    if not text or not text.strip():
        return []
    
    steps = []
    # 先按换行分隔
    lines = text.strip().split('\n')
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
        
        # 去除开头的数字编号
        line = re.sub(r'^[\d]+[.、\s:：]+', '', line).strip()
        
        if line:
            steps.append(line)
    
    return steps


def compress_image(image_path: Path) -> bytes:
    """
    压缩图片：
    - 缩放到最大 600x600
    - 转换为 JPEG 格式
    - 压缩质量 70%
    """
    with Image.open(image_path) as img:
        # 转换为 RGB（处理 PNG 透明通道等）
        if img.mode in ('RGBA', 'P'):
            img = img.convert('RGB')
        
        # 计算缩放比例
        width, height = img.size
        if width > MAX_IMAGE_SIZE or height > MAX_IMAGE_SIZE:
            ratio = min(MAX_IMAGE_SIZE / width, MAX_IMAGE_SIZE / height)
            new_size = (int(width * ratio), int(height * ratio))
            img = img.resize(new_size, Image.Resampling.LANCZOS)
        
        # 压缩为 JPEG
        buffer = io.BytesIO()
        img.save(buffer, format='JPEG', quality=JPEG_QUALITY, optimize=True)
        return buffer.getvalue()


def find_cover_image(cover_dir: Path, recipe_name: str) -> tuple[str | None, bool]:
    """
    查找封面图片 <菜谱名称>.jpg，压缩后返回 base64 编码的字符串
    返回: (base64字符串或None, 是否找到图片)
    """
    if not cover_dir.exists():
        return None, False
    
    image_path = cover_dir / f"{recipe_name}.jpg"
    
    if image_path.exists():
        try:
            # 压缩图片
            compressed_data = compress_image(image_path)
            original_size = image_path.stat().st_size
            compressed_size = len(compressed_data)
            
            print(f"  ✓ {recipe_name}.jpg ({original_size//1024}KB -> {compressed_size//1024}KB)")
            
            # 返回带前缀的 base64 字符串
            base64_str = base64.b64encode(compressed_data).decode('utf-8')
            return f"data:image/jpeg;base64,{base64_str}", True
        except Exception as e:
            print(f"  ✗ 处理图片 {image_path} 失败: {e}")
            return None, False
    
    return None, False


def transform_excel_to_json(excel_path: str, output_path: str, cover_dir: Path = None):
    """将 Excel 文件转换为 JSON"""
    
    wb = openpyxl.load_workbook(excel_path)
    ws = wb.active
    
    current_time = int(time.time() * 1000)  # 毫秒时间戳
    
    categories = {}  # name -> id
    category_list = []
    recipe_list = []
    ingredient_list = []
    step_list = []
    
    category_id = 1
    recipe_id = 1
    ingredient_id = 1
    step_id = 1
    
    cover_count = 0  # 统计找到的封面图数量
    missing_covers = []  # 记录缺少封面图的菜谱
    
    print("\n处理菜谱数据...")
    
    # 从第2行开始读取（跳过标题行）
    for row_idx, row in enumerate(ws.iter_rows(min_row=2, values_only=True), start=2):
        # B=分类, C=名称, D=用料, E=步骤 (索引 1,2,3,4)
        if len(row) < 5:
            continue
            
        category_name = str(row[1]).strip() if row[1] else ""
        recipe_name = str(row[2]).strip() if row[2] else ""
        ingredients_text = str(row[3]).strip() if row[3] else ""
        steps_text = str(row[4]).strip() if row[4] else ""
        
        # 跳过空行
        if not category_name or not recipe_name:
            print(f"跳过第 {row_idx} 行：分类或名称为空")
            continue
        
        # 处理分类
        if category_name not in categories:
            categories[category_name] = category_id
            category_list.append({
                "id": category_id,
                "name": category_name,
                "sortOrder": len(category_list),
                "createdAt": current_time,
                "updatedAt": current_time
            })
            category_id += 1
        
        cat_id = categories[category_name]
        
        # 查找封面图
        cover_base64 = None
        if cover_dir:
            cover_base64, found = find_cover_image(cover_dir, recipe_name)
            if found:
                cover_count += 1
            else:
                missing_covers.append((category_name, recipe_name))
        
        # 创建菜谱
        recipe_list.append({
            "id": recipe_id,
            "name": recipe_name,
            "categoryId": cat_id,
            "coverImagePath": None,
            "coverImageBase64": cover_base64,
            "clickCount": 0,
            "isFavorite": False,
            "createdAt": current_time,
            "updatedAt": current_time
        })
        
        # 解析用料
        ingredients = parse_ingredients(ingredients_text)
        for idx, ing in enumerate(ingredients):
            ingredient_list.append({
                "id": ingredient_id,
                "recipeId": recipe_id,
                "name": ing["name"],
                "amount": ing["amount"],
                "sortOrder": idx
            })
            ingredient_id += 1
        
        # 解析步骤
        steps = parse_steps(steps_text)
        for idx, step_desc in enumerate(steps):
            step_list.append({
                "id": step_id,
                "recipeId": recipe_id,
                "description": step_desc,
                "imagePath": None,
                "stepNumber": idx + 1,
                "sortOrder": idx
            })
            step_id += 1
        
        recipe_id += 1
    
    # 构建输出数据
    output_data = {
        "version": 1,
        "exportTime": current_time,
        "categories": category_list,
        "recipes": recipe_list,
        "ingredients": ingredient_list,
        "steps": step_list
    }
    
    # 写入 JSON 文件
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(output_data, f, ensure_ascii=False, indent=2)
    
    # 输出统计信息
    print(f"\n{'='*50}")
    print(f"转换完成！")
    print(f"  - 分类数量: {len(category_list)}")
    print(f"  - 菜谱数量: {len(recipe_list)}")
    print(f"  - 封面图数量: {cover_count}/{len(recipe_list)}")
    print(f"  - 用料数量: {len(ingredient_list)}")
    print(f"  - 步骤数量: {len(step_list)}")
    print(f"  - 输出文件: {output_path}")
    
    # 输出缺少封面图的菜谱
    if missing_covers:
        print(f"\n{'='*50}")
        print(f"缺少封面图的菜谱 ({len(missing_covers)} 个):")
        print("-" * 50)
        
        # 按分类分组显示
        by_category = {}
        for cat, name in missing_covers:
            if cat not in by_category:
                by_category[cat] = []
            by_category[cat].append(name)
        
        for cat, names in by_category.items():
            print(f"\n【{cat}】")
            for name in names:
                print(f"  - {name}")


if __name__ == "__main__":
    # 获取脚本所在目录
    script_dir = Path(__file__).parent
    
    excel_file = script_dir / "recipes.xlsx"
    timestamp = time.strftime("%Y%m%d_%H%M%S")
    output_file = script_dir / f"recipes_backup_{timestamp}.json"
    cover_dir = script_dir / "cover"  # 封面图目录
    
    if not excel_file.exists():
        print(f"错误：找不到文件 {excel_file}")
        exit(1)
    
    print(f"Excel 文件: {excel_file}")
    
    if cover_dir.exists():
        print(f"封面图目录: {cover_dir}")
        print(f"封面图格式: <菜谱名称>.jpg")
    else:
        print(f"提示：未找到封面图目录 {cover_dir}，将不导入封面图")
        cover_dir = None
    
    transform_excel_to_json(str(excel_file), str(output_file), cover_dir)
