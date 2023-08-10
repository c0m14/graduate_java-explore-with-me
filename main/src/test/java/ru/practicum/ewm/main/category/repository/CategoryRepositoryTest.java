package ru.practicum.ewm.main.category.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.exception.InvalidParamException;
import ru.practicum.ewm.main.exception.NotExistsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CategoryRepositoryTest {

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM category");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void save_whenNameExists_thenDataIntegrityViolationExceptionThrown() {
        String categoryName = "name";
        Category category1 = new Category();
        category1.setName(categoryName);
        categoryRepository.save(category1);
        Category category2 = new Category();
        category2.setName(categoryName);

        assertThrows(DataIntegrityViolationException.class,
                () -> categoryRepository.save(category2));
    }

    @Test
    void delete_whenCategoryNotFound_thenNotExistExceptionThrown() {
        assertThrows(NotExistsException.class,
                () -> categoryRepository.delete(999_999_999));
    }

    @Test
    void update_whenNameExists_thenDataIntegrityViolationExceptionThrown() {
        String categoryName = "name";
        String anotherCategoryName = "oldname";
        Category category1 = new Category();
        category1.setName(categoryName);
        Category category2 = new Category();
        category2.setName(anotherCategoryName);
        categoryRepository.save(category1);
        int idToUpdate = categoryRepository.save(category2).getId();
        Map<String, Object> updateParams = Map.of(
                "categoryId", idToUpdate,
                "categoryName", categoryName
        );

        assertThrows(DataIntegrityViolationException.class,
                () -> categoryRepository.update(updateParams));
    }


}