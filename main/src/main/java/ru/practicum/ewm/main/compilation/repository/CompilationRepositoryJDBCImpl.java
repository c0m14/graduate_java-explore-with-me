package ru.practicum.ewm.main.compilation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.main.compilation.model.Compilation;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompilationRepositoryJDBCImpl implements CompilationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Compilation save(Compilation compilation) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("compilation")
                .usingGeneratedKeyColumns("compilation_id");

        Long compilationId = simpleJdbcInsert.executeAndReturnKey(compilation.mapToDb()).longValue();
        compilation.setId(compilationId);

        if (!compilation.getEvents().isEmpty()) {
            addEventsRecordsForCompilation(compilation);
        }

        return compilation;
    }

    @Override
    public void deleteById(Long compilationId) {
        String query = "DELETE FROM compilation " +
                "WHERE compilation_id = :compilationId";
        SqlParameterSource namedParams = new MapSqlParameterSource("compilationId", compilationId);

        boolean isDeleted = jdbcTemplate.update(query, namedParams) > 0;

        if (!isDeleted) {
            throw new NotExistsException(
                    "Compilation",
                    String.format("No compilation with id %d found", compilationId)
            );
        }
    }

    @Override
    public Optional<Compilation> findByIdWithoutEvents(Long compilationId) {
        String query = "SELECT compilation_id, pinned, title " +
                "FROM compilation " +
                "WHERE compilation_id = :compilationId";
        SqlParameterSource namedParams = new MapSqlParameterSource("compilationId", compilationId);

        try {
            return Optional.of(jdbcTemplate.queryForObject(query, namedParams, this::mapRowToCompilationShort));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void clearEventsRecordsForCompilation(Long compilationId) {
        String query = "DELETE FROM compilations_events " +
                "WHERE compilation_id = :compilationId";
        SqlParameterSource namedParams = new MapSqlParameterSource("compilationId", compilationId);

        jdbcTemplate.update(query, namedParams);
    }

    @Override
    public void addEventsRecordsForCompilation(Compilation compilation) {
        String query = "INSERT INTO compilations_events (compilation_id, event_id) " +
                "VALUES (:compilationId, :eventId)";
        List<SqlParameterSource> batchedUpdateParams = new ArrayList<>(compilation.getEvents().size());
        List<Long> eventsIds = compilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        for (Long eventId : eventsIds) {
            MapSqlParameterSource namedParam = new MapSqlParameterSource()
                    .addValue("compilationId", compilation.getId())
                    .addValue("eventId", eventId);
            batchedUpdateParams.add(namedParam);
        }

        try {
            jdbcTemplate.batchUpdate(query, batchedUpdateParams.toArray(SqlParameterSource[]::new));
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenException(
                    "Event",
                    "One of events doesn't exist"
            );
        }
    }

    @Override
    public void update(Compilation compilation) {
        String query = "UPDATE compilation " +
                "SET pinned = :pinned, title = :title " +
                "WHERE compilation_id = :compilationId";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("pinned", compilation.isPinned())
                .addValue("title", compilation.getTitle())
                .addValue("compilationId", compilation.getId());

        jdbcTemplate.update(query, namedParams);
    }

    @Override
    public List<Compilation> findCompilationsWithoutEvents(boolean pinned, int offset, int size) {
        String query = "SELECT compilation_id, pinned, title " +
                "FROM compilation " +
                "WHERE pinned = :pinned " +
                "OFFSET :offset ROWS " +
                "FETCH NEXT :size ROWS ONLY";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("pinned", pinned)
                .addValue("offset", offset)
                .addValue("size", size);

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToCompilationShort);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    private Compilation mapRowToCompilationShort(ResultSet resultSet, int rowNum) throws SQLException {
        return Compilation.builder()
                .id(resultSet.getLong("compilation_id"))
                .title(resultSet.getString("title"))
                .pinned(resultSet.getBoolean("pinned"))
                .build();
    }
}
