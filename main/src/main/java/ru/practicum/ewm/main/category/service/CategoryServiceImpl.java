package ru.practicum.ewm.main.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.dto.NewCategoryDto;
import ru.practicum.ewm.main.category.mapper.CategoryMapper;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService{

    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        Category newCategory = CategoryMapper.mapToEntity(newCategoryDto);

        Category savedCategory = categoryRepository.save(newCategory);
        return CategoryMapper.mapToCategoryDto(savedCategory);
    }

    @Override
    public void deleteCategory(int categoryId) {
        categoryRepository.delete(categoryId);
    }

    @Override
    public CategoryDto updateCategory(int categoryId, CategoryDto updateCategoryRequest) {
        Category updatedCategory = CategoryMapper.mapToEntity(updateCategoryRequest);
        Map<String, Object> updateParams = Map.of(
                "categoryId", categoryId,
                "categoryName", updatedCategory.getName()
        );

        categoryRepository.update(updateParams);

        updateCategoryRequest.setId(categoryId);
        return updateCategoryRequest;
    }
}
