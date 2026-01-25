#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将 recipes.xlsx 转换为 APP 可导入的 JSON 格式
Excel 格式：B列=分类, C列=名称, D列=用料, E列=步骤
"""

import json
import time
import re
from pathlib import Path

try:
    import openpyxl
except ImportError:
    print("请先安装 openpyxl: pip install openpyxl")
    exit(1)


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


def transform_excel_to_json(excel_path: str, output_path: str):
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
        
        # 创建菜谱
        recipe_list.append({
            "id": recipe_id,
            "name": recipe_name,
            "categoryId": cat_id,
            "coverImagePath": None,
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
    
    print(f"转换完成！")
    print(f"  - 分类数量: {len(category_list)}")
    print(f"  - 菜谱数量: {len(recipe_list)}")
    print(f"  - 用料数量: {len(ingredient_list)}")
    print(f"  - 步骤数量: {len(step_list)}")
    print(f"  - 输出文件: {output_path}")


if __name__ == "__main__":
    # 获取脚本所在目录
    script_dir = Path(__file__).parent
    
    excel_file = script_dir / "recipes.xlsx"
    output_file = script_dir / "recipes_backup.json"
    
    if not excel_file.exists():
        print(f"错误：找不到文件 {excel_file}")
        exit(1)
    
    transform_excel_to_json(str(excel_file), str(output_file))
