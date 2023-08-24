package ru.practicum.ewm.main.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.user.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryJDBCImpl implements UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public User save(User user) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("users")
                .usingGeneratedKeyColumns("user_id");
        long savedUserId;
        try {
            savedUserId = simpleJdbcInsert.executeAndReturnKey(user.mapToDb()).longValue();
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenException(
                    "Email",
                    String.format("User with email %s already exists", user.getEmail())
            );
        }

        user.setId(savedUserId);
        return user;
    }

    @Override
    public List<User> findUsers(List<Long> ids, int offset, int size) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT user_id, user_name, email ");
        queryBuilder.append("FROM users ");
        if (ids != null && !ids.isEmpty()) {
            queryBuilder.append("WHERE user_id in (:ids) ");
        }
        queryBuilder.append("ORDER BY user_id ASC ");
        queryBuilder.append("OFFSET :offset ROWS ");
        queryBuilder.append("FETCH NEXT :size ROWS ONLY");

        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("ids", ids)
                .addValue("offset", offset)
                .addValue("size", size);

        try {
            return jdbcTemplate.query(queryBuilder.toString(), namedParams, this::mapRowToUser);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public void deleteUser(Long userId) {
        String query = "DELETE FROM users " +
                "WHERE user_id = :userId";
        SqlParameterSource namedParams = new MapSqlParameterSource("userId", userId);

        boolean isDeleted = jdbcTemplate.update(query, namedParams) > 0;
        if (!isDeleted) {
            throw new NotExistsException(
                    "User",
                    String.format("User with id %d not found", userId)
            );
        }
    }

    @Override
    public Optional<User> findUserById(Long userId) {
        String query = "SELECT user_id, user_name, email " +
                "FROM users " +
                "WHERE user_id = :userId";
        SqlParameterSource namedParams = new MapSqlParameterSource("userId", userId);

        try {
            return Optional.of(
                    jdbcTemplate.queryForObject(query, namedParams, this::mapRowToUser)
            );
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Boolean userExists(Long userId) {
        String query = "SELECT EXISTS(SELECT user_id FROM users WHERE user_id = :userId)";
        SqlParameterSource namedParams = new MapSqlParameterSource("userId", userId);

        return jdbcTemplate.queryForObject(query, namedParams, Boolean.class);
    }

    private User mapRowToUser(ResultSet resultSet, int rowNum) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("user_id"));
        user.setName(resultSet.getString("user_name"));
        user.setEmail(resultSet.getString("email"));

        return user;
    }
}
