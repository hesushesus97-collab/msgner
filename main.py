import os
from pathlib import Path

# Список папок и файлов, которые не нужно включать в архитектуру
IGNORE_LIST = {
    'node_modules', '.git', '.idea', '.vscode', '__pycache__', 
    '.venv', 'venv', 'env', '.pytest_cache', '.DS_Store', 'build', '.gradle', 'kotlin', 'res', 'gradle', '.obj'
}

def build_tree(dir_path: Path, prefix: str = "") -> str:
    """Рекурсивно строит текстовое дерево архитектуры проекта."""
    tree = []
    
    # Получаем список всех элементов в директории, сортируем (сначала папки, потом файлы)
    try:
        items = sorted(
            list(dir_path.iterdir()), 
            key=lambda x: (not x.is_dir(), x.name.lower())
        )
    except PermissionError:
        return ""

    # Фильтруем игнорируемые элементы
    items = [item for item in items if item.name not in IGNORE_LIST]
    
    count = len(items)
    for i, item in enumerate(items):
        is_last = (i == count - 1)
        connector = "└── " if is_last else "├── "
        
        # Добавляем иконку в зависимости от типа элемента
        icon = "📁 " if item.is_dir() else "📄 "
        tree.append(f"{prefix}{connector}{icon}{item.name}")
        
        # Если это папка, углубляемся дальше
        if item.is_dir():
            extension = "    " if is_last else "│   "
            tree.append(build_tree(item, prefix + extension))
            
    return "\n".join(filter(None, tree))

def save_architecture():
    current_dir = Path.cwd()
    print(f"🏗️  Сканирование архитектуры в: {current_dir}\n")
    
    # Генерируем дерево
    project_name = current_dir.name
    tree_structure = f"📁 {project_name}\n" + build_tree(current_dir)
    
    # Сохраняем результат в файл
    output_file = "project_architecture.txt"
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(tree_structure)
        
    print(tree_structure)
    print(f"\n✅ Архитектура успешно сохранена в файл: {output_file}")

if __name__ == "__main__":
    save_architecture()
