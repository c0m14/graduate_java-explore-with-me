package ru.practicum.ewm.main.category.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CategoryRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM event");
        jdbcTemplate.update("DELETE FROM category");
    }

    @Test
    void save_whenNameExists_thenForbiddenExceptionThrown() {
        String categoryName = "name";
        Category category1 = new Category();
        category1.setName(categoryName);
        categoryRepository.save(category1);
        Category category2 = new Category();
        category2.setName(categoryName);

        assertThrows(ForbiddenException.class,
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

    @Test
    void getCategoriesReturnedWithOffsetLimit() {
        Category category1 = new Category("name1");
        Category savedCategory1 = categoryRepository.save(category1);
        Category category2 = new Category("name2");
        Category savedCategory2 = categoryRepository.save(category2);

        List<Category> foundCategories = categoryRepository.findCategories(1, 1);

        assertThat(foundCategories.size(), equalTo(1));
        assertThat(foundCategories.get(0), equalTo(savedCategory2));
        assertFalse(foundCategories.contains(savedCategory1));
    }

    @Test
    void getCategoriesReturnedWithSizeLimit() {
        Category category1 = new Category("name1");
        Category savedCategory1 = categoryRepository.save(category1);
        Category category2 = new Category("name2");
        Category savedCategory2 = categoryRepository.save(category2);

        List<Category> foundCategories = categoryRepository.findCategories(0, 2);

        assertThat(foundCategories.size(), equalTo(2));
        assertThat(foundCategories.get(0), equalTo(savedCategory1));
        assertThat(foundCategories.get(1), equalTo(savedCategory2));
    }

    @Test
    void getCategoriesReturnedEmptyListWhenNotFound() {
        List<Category> foundCategories = categoryRepository.findCategories(0, 2);

        assertTrue(foundCategories.isEmpty());
    }

    @Test
    void getCategoryReturnedOptionalEmptyWhenNotFound() {
        int categoryId = 0;

        Optional<Category> foundCategory = categoryRepository.findCategoryById(categoryId);

        assertTrue(foundCategory.isEmpty());
    }


}