package ru.practicum.ewm.main.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.dto.NewCategoryDto;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CategoryServiceIntegrationTests {

    private final String host = "http://localhost:";
    @Autowired
    private TestRestTemplate restTemplate;
    @Value("${local.server.port}")
    private String port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CategoryRepository categoryRepository;
    private HttpHeaders headers = new HttpHeaders();

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM event");
        jdbcTemplate.update("DELETE FROM category");
    }

    @Test
    void addCategory() {
        URI uri = URI.create(host + port + "/admin/categories");
        NewCategoryDto newCategory = new NewCategoryDto();
        newCategory.setName("name");

        CategoryDto returnedCategory = restTemplate.postForEntity(uri, newCategory, CategoryDto.class)
                .getBody();

        Category savedCategory = jdbcTemplate.queryForObject("select category_id, category_name " +
                "from category " +
                "where category_id = ?", this::mapRowToCategory, returnedCategory.getId());
        assertThat(returnedCategory.getName(), equalTo(newCategory.getName()));
        assertThat(savedCategory.getName(), equalTo(newCategory.getName()));
    }

    @Test
    void deleteCategory() {
        Category category = new Category("name");
        int categoryId = categoryRepository.save(category).getId();

        restTemplate.exchange(
                host + port + "/admin/categories/" + categoryId,
                HttpMethod.DELETE,
                null,
                void.class
        );

        List<Category> foundCategories = jdbcTemplate.query("select category_id, category_name " +
                "from category " +
                "where category_id = ?", this::mapRowToCategory, categoryId);

        assertTrue(foundCategories.isEmpty());
    }

    @Test
    void updateCategory() {
        Category originalCategory = new Category("oldName");
        int savedCategoryId = categoryRepository.save(originalCategory).getId();
        CategoryDto updateCategoryRequest = new CategoryDto();
        updateCategoryRequest.setName("new name");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CategoryDto> entity = new HttpEntity<>(updateCategoryRequest, headers);

        CategoryDto returnedCategory = restTemplate.exchange(
                host + port + "/admin/categories/" + savedCategoryId,
                HttpMethod.PATCH,
                entity,
                CategoryDto.class
        ).getBody();

        Category updatedCategory = jdbcTemplate.queryForObject("select category_id, category_name " +
                "from category " +
                "where category_id = ?", this::mapRowToCategory, savedCategoryId);
        assertThat(returnedCategory.getName(), equalTo(updateCategoryRequest.getName()));
        assertThat(returnedCategory.getId(), equalTo(savedCategoryId));
        assertThat(updatedCategory.getName(), equalTo(updateCategoryRequest.getName()));
    }

    @Test
    void getCategories() {
        Category category1 = new Category("name1");
        Category category2 = new Category("name2");
        Category category3 = new Category("name3");
        categoryRepository.save(category1);
        categoryRepository.save(category2);
        categoryRepository.save(category3);
        Map<String, String> queryParams = Map.of(
                "from", "1",
                "size", "2"
        );

        List<CategoryDto> foundCategories = restTemplate.exchange(
                host + port + "/categories?from={from}&size={size}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CategoryDto>>() {
                },
                queryParams
        ).getBody();

        assertThat(foundCategories.size(), equalTo(2));
        assertThat(foundCategories.get(0).getName(), equalTo(category2.getName()));
        assertThat(foundCategories.get(1).getName(), equalTo(category3.getName()));
    }

    @Test
    void getCategoryById() {
        Category category = new Category("name");
        int categoryId = categoryRepository.save(category).getId();

        CategoryDto foundCategory = restTemplate.exchange(
                host + port + "/categories/" + categoryId,
                HttpMethod.GET,
                null,
                CategoryDto.class
        ).getBody();

        assertThat(foundCategory.getName(), equalTo(category.getName()));
        assertThat(foundCategory.getId(), equalTo(categoryId));
    }

    private Category mapRowToCategory(ResultSet resultSet, int rowNum) throws SQLException {
        return new Category(
                resultSet.getInt("category_id"),
                resultSet.getString("category_name")
        );
    }
}
