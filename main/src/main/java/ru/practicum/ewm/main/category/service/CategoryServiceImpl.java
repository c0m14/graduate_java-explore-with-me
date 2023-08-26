package ru.practicum.ewm.main.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.dto.NewCategoryDto;
import ru.practicum.ewm.main.category.mapper.CategoryMapper;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;
import ru.practicum.ewm.main.exception.NotExistsException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        Category newCategory = CategoryMapper.mapToEntity(newCategoryDto);

        Category savedCategory = categoryRepository.save(newCategory);
        return CategoryMapper.mapToCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(int categoryId) {
        categoryRepository.delete(categoryId);
    }

    @Override
    @Transactional
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

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        List<Category> foundCategories = categoryRepository.findCategories(from, size);

        return foundCategories.stream()
                .map(CategoryMapper::mapToCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(int categoryId) {
        Category foundCategory = categoryRepository.findCategoryById(categoryId).orElseThrow(
                () -> new NotExistsException(
                        "Category",
                        String.format("Category with id %d not exists", categoryId)
                )
        );

        return CategoryMapper.mapToCategoryDto(foundCategory);
    }
}
