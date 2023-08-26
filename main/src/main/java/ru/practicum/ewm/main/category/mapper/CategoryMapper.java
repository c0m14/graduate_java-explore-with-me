package ru.practicum.ewm.main.category.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.dto.NewCategoryDto;
import ru.practicum.ewm.main.category.model.Category;

@UtilityClass
public class CategoryMapper {

    public Category mapToEntity(NewCategoryDto newCategoryDto) {
        return new Category(newCategoryDto.getName());
    }

    public Category mapToEntity(CategoryDto categoryDto) {
        return new Category(categoryDto.getName());
    }

    public CategoryDto mapToCategoryDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }

}
