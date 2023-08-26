package ru.practicum.ewm.main.category.service;

import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto addCategory(NewCategoryDto newCategoryDto);

    void deleteCategory(int categoryId);

    CategoryDto updateCategory(int categoryId, CategoryDto updateCategoryRequest);

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryById(int categoryId);
}
