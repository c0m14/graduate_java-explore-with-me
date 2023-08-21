package ru.practicum.ewm.main.category.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryJDBCImpl implements CategoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Category save(Category category) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("category")
                .usingGeneratedKeyColumns("category_id");

        int savedCategoryId;
        try {
            savedCategoryId = simpleJdbcInsert.executeAndReturnKey(category.mapToDb()).intValue();
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenException(
                    "Category name",
                    String.format("Category with name %s already exists", category.getName())
            );
        }


        category.setId(savedCategoryId);
        return category;
    }

    @Override
    public void delete(int categoryId) {
        String query = "DELETE FROM category " +
                "WHERE category_id = :categoryId";
        SqlParameterSource namedParams = new MapSqlParameterSource("categoryId", categoryId);

        boolean isDeleted = jdbcTemplate.update(query, namedParams) > 0;
        if (!isDeleted) {
            throw new NotExistsException(
                    "Category",
                    String.format("Category with id %d not exists", categoryId)
            );
        }
    }

    @Override
    public void update(Map<String, Object> updateParams) {
        String query = "UPDATE category " +
                "SET category_name = :categoryName " +
                "WHERE category_id = :categoryId";

        jdbcTemplate.update(query, updateParams);
    }

    @Override
    public List<Category> findCategories(int offset, int size) {
        String query = "SELECT category_id, category_name " +
                "FROM category " +
                "OFFSET :offset ROWS " +
                "FETCH NEXT :size ROWS ONLY";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("offset", offset)
                .addValue("size", size);

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToCategory);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public Optional<Category> findCategoryById(int categoryId) {
        String query = "SELECT category_id, category_name " +
                "FROM category " +
                "WHERE category_id = :categoryId";
        SqlParameterSource namedParams = new MapSqlParameterSource("categoryId", categoryId);

        try {
            return Optional.of(jdbcTemplate.queryForObject(query, namedParams, this::mapRowToCategory));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private Category mapRowToCategory(ResultSet resultSet, int rowNum) throws SQLException {
        return new Category(
                resultSet.getInt("category_id"),
                resultSet.getString("category_name")
        );
    }

}
