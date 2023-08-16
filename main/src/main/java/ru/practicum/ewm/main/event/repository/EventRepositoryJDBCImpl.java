package ru.practicum.ewm.main.event.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.event.dto.SearchEventParamsDto;
import ru.practicum.ewm.main.event.dto.SearchSortOptionDto;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.user.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventRepositoryJDBCImpl implements EventRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Event save(Event event) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("event")
                .usingGeneratedKeyColumns("event_id");

        Long eventId = simpleJdbcInsert.executeAndReturnKey(event.mapToDb()).longValue();
        event.setId(eventId);
        return event;
    }

    @Override
    public List<Event> getUsersEvents(Long userId, int offset, int size) {
        String query = "SELECT e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, " +
                "e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, e.request_moderation, e.created_on, " +
                "e.published_on, e.state, " +
                "c.category_id, c.category_name, " +
                "u.user_id, u.user_name " +
                "FROM event AS e " +
                "INNER JOIN category AS c ON e.category_id = c.category_id " +
                "INNER JOIN users AS u ON e.initiator_id = u.user_id " +
                "WHERE e.initiator_id = :userId " +
                "ORDER BY e.event_date DESC " +
                "OFFSET :offset ROWS " +
                "FETCH NEXT :size ROWS ONLY";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("offset", offset)
                .addValue("size", size);

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToEventShort);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public Optional<Event> getByInitiatorIdAndEventId(Long userId, Long eventId) {
        String query = "SELECT e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, " +
                "e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, e.request_moderation, e.created_on, " +
                "e.published_on, e.state, " +
                "c.category_id, c.category_name, " +
                "u.user_id, u.user_name " +
                "FROM event AS e " +
                "INNER JOIN category AS c ON e.category_id = c.category_id " +
                "INNER JOIN users AS u ON e.initiator_id = u.user_id " +
                "WHERE e.event_id = :eventId " +
                "AND e.initiator_id = :userId";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("userId", userId);

        try {
            return Optional.of(jdbcTemplate.queryForObject(query, namedParams, this::mapRowToEventFull));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void updateEvent(Event event) {
        String query = "UPDATE event " +
                "SET title = :title, annotation = :annotation, description = :description, " +
                "category_id = :categoryId, event_date = :eventDate, paid = :paid, latitude = :latitude, " +
                "longitude = :longitude, request_moderation = :requestModeration, state = :state, " +
                "participant_limit = :participantLimit " +
                "WHERE event_id = :eventId";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("title", event.getTitle())
                .addValue("annotation", event.getAnnotation())
                .addValue("description", event.getDescription())
                .addValue("categoryId", event.getCategory().getId())
                .addValue("eventDate", event.getEventDate())
                .addValue("paid", event.isPaid())
                .addValue("latitude", event.getLocation().getLat())
                .addValue("longitude", event.getLocation().getLon())
                .addValue("requestModeration", event.isRequestModeration())
                .addValue("state", event.getState().toString())
                .addValue("participantLimit", event.getParticipantLimit())
                .addValue("eventId", event.getId());

        jdbcTemplate.update(query, namedParams);

    }

    @Override
    public List<Event> findEvents(SearchEventParamsDto searchParams) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, ");
        queryBuilder.append("e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, ");
        queryBuilder.append("e.request_moderation, e.created_on, e.published_on, e.state, e.published_on, e.state, ");
        queryBuilder.append("c.category_id, c.category_name, ");
        queryBuilder.append("u.user_id, u.user_name ");
        queryBuilder.append("FROM event AS e ");
        queryBuilder.append("INNER JOIN category AS c ON e.category_id = c.category_id ");
        queryBuilder.append("INNER JOIN users AS u ON e.initiator_id = u.user_id ");
        queryBuilder.append("WHERE e.state = :state ");
        queryBuilder.append("AND e.event_date > :start ");
        if (searchParams.getRangeEnd() != null) {
            queryBuilder.append("AND e.event_date < :end ");
        }
        if (searchParams.getText() != null && !searchParams.getText().isBlank()) {
            queryBuilder.append("AND (LOWER(e.annotation) LIKE CONCAT('%',:text,'%') ");
            queryBuilder.append("OR LOWER(e.description) LIKE CONCAT('%',:text,'%')) ");
        }
        if (searchParams.getCategoriesIds() != null && !searchParams.getCategoriesIds().isEmpty()) {
            queryBuilder.append("AND e.category_id IN (:categoriesIds) ");
        }
        if (searchParams.getPaid() != null) {
            queryBuilder.append("AND e.paid = :paid ");
        }
        if (searchParams.getSortOption().equals(SearchSortOptionDto.EVENT_DATE)) {
            queryBuilder.append("ORDER BY e.event_date ");
            if (searchParams.getFrom() != null) {
                queryBuilder.append("OFFSET :offset ROWS ");
            }
            if (searchParams.getSize() != null) {
                queryBuilder.append("FETCH NEXT :size ROWS ONLY");
            }
        }

        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("state", searchParams.getState().toString())
                .addValue("start", searchParams.getRangeStart())
                .addValue("end", searchParams.getRangeEnd())
                .addValue("text", searchParams.getText())
                .addValue("categoriesIds", searchParams.getCategoriesIds())
                .addValue("paid", searchParams.getPaid())
                .addValue("offset", searchParams.getFrom())
                .addValue("size", searchParams.getSize());

        try {
            return jdbcTemplate.query(queryBuilder.toString(), namedParams, this::mapRowToEventShort);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public Optional<Event> findEventByIdAndState(Long eventId, EventState state) {
        String query = "SELECT e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, " +
                "e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, e.request_moderation, e.created_on, " +
                "e.published_on, e.state, " +
                "c.category_id, c.category_name, " +
                "u.user_id, u.user_name " +
                "FROM event AS e " +
                "INNER JOIN category AS c ON e.category_id = c.category_id " +
                "INNER JOIN users AS u ON e.initiator_id = u.user_id " +
                "WHERE e.event_id = :eventId " +
                "AND e.state = :state";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("state", state.toString());

        try {
            return Optional.of(jdbcTemplate.queryForObject(query, namedParams, this::mapRowToEventFull));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private Event mapRowToEventShort(ResultSet resultSet, int rowNum) throws SQLException {
        return getEventBuilderWithBaseFields(resultSet, rowNum).build();
    }

    private Event mapRowToEventFull(ResultSet resultSet, int rowNum) throws SQLException {
        return getEventBuilderWithBaseFields(resultSet, rowNum)
                .location(new Location(
                        resultSet.getFloat("latitude"),
                        resultSet.getFloat("longitude")
                ))
                .state(EventState.valueOf(resultSet.getString("state")))
                .build();
    }

    private Event.EventBuilder getEventBuilderWithBaseFields(ResultSet resultSet, int rowNum) throws SQLException {
        return Event.builder()
                .id(resultSet.getLong("event_id"))
                .title(resultSet.getString("title"))
                .annotation(resultSet.getString("annotation"))
                .description(resultSet.getString("description"))
                .category(new Category(
                        resultSet.getInt("category_id"),
                        resultSet.getString("category_name")))
                .eventDate(LocalDateTime.parse(resultSet.getString("event_date"), formatter))
                .initiator(new User(
                        resultSet.getLong("user_id"),
                        resultSet.getString("user_name")))
                .paid(resultSet.getBoolean("paid"))
                .location(null)
                .participantLimit(resultSet.getInt("participant_limit"))
                .requestModeration(resultSet.getBoolean("request_moderation"))
                .createdOn(LocalDateTime.parse(resultSet.getString("created_on"), formatter))
                .publishedOn(resultSet.getString("published_on") == null ?
                        null : LocalDateTime.parse(resultSet.getString("published_on")))
                .state(null);
    }
}
