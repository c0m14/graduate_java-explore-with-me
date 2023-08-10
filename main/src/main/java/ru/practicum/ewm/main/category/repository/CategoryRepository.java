package ru.practicum.ewm.main.category.repository;

import ru.practicum.ewm.main.category.model.Category;

import java.util.Map;
import java.util.Optional;

public interface CategoryRepository {
    Category save(Category category);
    void delete(int categoryId);
    void update(Map<String, Object> updateParams);
    Optional<Category> getCategoryById(int categoryId);
}
