package ru.practicum.ewm.main.event.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.event.dto.searchrequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.SearchSortOptionDto;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.request.model.RequestStatus;
import ru.practicum.ewm.main.user.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    public List<Event> findUserEvents(Long userId, int offset, int size) {
        String query = getSelectQueryWithUserCategoryAndRequest() +
                "WHERE e.initiator_id = :userId " +
                "GROUP BY e.event_id, e.event_date, c.category_id, u.user_id " +
                "ORDER BY e.event_date DESC " +
                "OFFSET :offset ROWS " +
                "FETCH NEXT :size ROWS ONLY";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("offset", offset)
                .addValue("size", size)
                .addValue("requestStatus", RequestStatus.CONFIRMED.toString());

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToEventFull);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public Optional<Event> findEventByInitiatorIdAndEventId(Long userId, Long eventId) {
        String query = getSelectQueryWithUserCategoryAndRequest() +
                "WHERE e.event_id = :eventId " +
                "AND e.initiator_id = :userId " +
                "GROUP BY e.event_id, e.event_date, c.category_id, u.user_id ";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("userId", userId)
                .addValue("requestStatus", RequestStatus.CONFIRMED.toString());


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
                "participant_limit = :participantLimit, published_on = :publishedOn " +
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
                .addValue("eventId", event.getId())
                .addValue("publishedOn", event.getPublishedOn());

        jdbcTemplate.update(query, namedParams);

    }

    @Override
    public List<Event> findEventsPublic(PublicSearchParamsDto searchParams) {
        StringBuilder queryBuilder = getStandardSelectEventQueryBuilder();
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
        if (searchParams.getOnlyAvailable() != null && searchParams.getOnlyAvailable()) {
            queryBuilder.append("AND confirmed_requests < e.participant_limit ");
        }
        queryBuilder.append("GROUP BY e.event_id, e.event_date, c.category_id, u.user_id ");
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
                .addValue("size", searchParams.getSize())
                .addValue("requestStatus", RequestStatus.CONFIRMED.toString());

        try {
            return jdbcTemplate.query(queryBuilder.toString(), namedParams, this::mapRowToEventFull);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public Optional<Event> findEventByIdAndState(Long eventId, EventState state) {
        String query = getSelectQueryWithUserCategoryAndRequest() +
                "WHERE e.event_id = :eventId " +
                "AND e.state = :state " +
                "GROUP BY e.event_id, e.event_date, c.category_id, u.user_id ";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("state", state.toString())
                .addValue("requestStatus", RequestStatus.CONFIRMED.toString());

        try {
            return Optional.of(jdbcTemplate.queryForObject(query, namedParams, this::mapRowToEventFull));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Event> findEventsAdmin(AdminSearchParamsDto searchParams) {
        StringBuilder queryBuilder = getStandardSelectEventQueryBuilder();
        if (hasWhereBlock(searchParams)) {
            queryBuilder.append("WHERE 1=1 ");
            if (searchParams.getUsersIds() != null && !searchParams.getUsersIds().isEmpty()) {
                queryBuilder.append("AND e.initiator_id IN (:userIds) ");
            }
            if (searchParams.getCategoriesIds() != null && !searchParams.getCategoriesIds().isEmpty()) {
                queryBuilder.append("AND e.category_id IN (:categoriesIds) ");
            }
            if (searchParams.getStates() != null && !searchParams.getStates().isEmpty()) {
                queryBuilder.append("AND e.state IN (:states) ");
            }
            if (searchParams.getRangeStart() != null) {
                queryBuilder.append("AND e.event_date > :start ");
            }
            if (searchParams.getRangeEnd() != null) {
                queryBuilder.append("AND e.event_date < :end ");
            }
        }
        queryBuilder.append("GROUP BY e.event_id, e.event_date, c.category_id, u.user_id ");
        queryBuilder.append("ORDER BY e.event_date ASC ");
        queryBuilder.append("OFFSET :offset ROWS ");
        queryBuilder.append("FETCH NEXT :size ROWS ONLY");

        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("userIds", searchParams.getUsersIds())
                .addValue("categoriesIds", searchParams.getCategoriesIds())
                .addValue("states", mapEventStatesToString(searchParams.getStates()))
                .addValue("start", searchParams.getRangeStart())
                .addValue("end", searchParams.getRangeEnd())
                .addValue("offset", searchParams.getFrom())
                .addValue("size", searchParams.getSize())
                .addValue("requestStatus", RequestStatus.CONFIRMED.toString());


        try {
            return jdbcTemplate.query(queryBuilder.toString(), namedParams, this::mapRowToEventFull);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public Optional<Event> findEventById(Long eventId) {
        String query = getSelectQueryWithUserCategoryAndRequest() +
                "WHERE e.event_id = :eventId " +
                "GROUP BY e.event_id, e.event_date, c.category_id, u.user_id ";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("requestStatus", RequestStatus.CONFIRMED.toString());

        try {
            return Optional.of(jdbcTemplate.queryForObject(query, namedParams, this::mapRowToEventFull));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Event> findEventByIdWithoutCategory(Long eventId) {
        String query = "SELECT e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, " +
                "e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, e.request_moderation, e.created_on, " +
                "e.published_on, e.state, " +
                "u.user_id, u.user_name, " +
                "COUNT(r.request_id) AS confirmed_requests " +
                "FROM event AS e " +
                "INNER JOIN users AS u ON e.initiator_id = u.user_id " +
                "LEFT JOIN event_participation_request AS r ON e.event_id = r.event_id " +
                "AND r.request_status = :requestStatus " +
                "WHERE e.event_id = :eventId " +
                "GROUP BY e.event_id, e.event_date, u.user_id ";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("requestStatus", RequestStatus.CONFIRMED.toString());

        try {
            return Optional.of(jdbcTemplate.queryForObject(query, namedParams,
                    this::mapRowToEventNoCategory));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void lockEventForShare(Long eventId) {
        String query = "SELECT event_id FROM event WHERE event_id = :eventId FOR SHARE";
        SqlParameterSource namedParams = new MapSqlParameterSource("eventId", eventId);

        try {
            jdbcTemplate.queryForObject(query, namedParams, Long.class);
        } catch (EmptyResultDataAccessException e) {
            throw new NotExistsException(
                    "Event",
                    String.format("Event with id %d not exists", eventId)
            );
        }

    }

    @Override
    public List<Event> findEventsByIds(Set<Long> eventsIds) {
        String query = getSelectQueryWithUserCategoryAndRequest() +
                "WHERE e.event_id IN (:eventsIds) " +
                "GROUP BY e.event_id, e.event_date, c.category_id, u.user_id " +
                "ORDER BY e.event_date ";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventsIds", eventsIds)
                .addValue("requestStatus", RequestStatus.CONFIRMED.toString());

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToEventFull);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public Map<Long, List<Event>> findEventsForCompilations(List<Long> compilationsIds) {
        String query = "SELECT ce.compilation_id, " +
                "e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, " +
                "e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, e.request_moderation, " +
                "e.created_on, e.published_on, e.state, " +
                "c.category_id, c.category_name, " +
                "u.user_id, u.user_name, " +
                "COUNT(r.request_id) AS confirmed_requests " +
                "FROM event AS e " +
                "INNER JOIN category AS c ON e.category_id = c.category_id " +
                "INNER JOIN users AS u ON e.initiator_id = u.user_id " +
                "LEFT JOIN event_participation_request AS r ON e.event_id = r.event_id " +
                "AND r.request_status = :requestStatus " +
                "INNER JOIN compilations_events AS ce ON e.event_id = ce.event_id " +
                "AND ce.compilation_id IN (:compilationsIds) " +
                "GROUP BY e.event_id, e.event_date, u.user_id, ce.compilation_id, c.category_id ";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("requestStatus", RequestStatus.CONFIRMED.toString())
                .addValue("compilationsIds", compilationsIds);

        Map<Long, List<Event>> compilationsEvents = new HashMap<>();

        List<Map<Long, Event>> compilationsIdWithEventsList = jdbcTemplate.query(query, namedParams,
                ((rs, rowNum) -> Collections.singletonMap(
                        rs.getLong("compilation_id"),
                        mapRowToEventFull(rs, rowNum))));

        compilationsIdWithEventsList.stream()
                .flatMap(map -> map.entrySet().stream())
                .forEach((entry -> {
                    if (compilationsEvents.containsKey(entry.getKey())) {
                        compilationsEvents.get(entry.getKey()).add(entry.getValue());
                    } else {
                        List<Event> events = new ArrayList<>();
                        events.add(entry.getValue());
                        compilationsEvents.put(entry.getKey(), events);
                    }
                }
                ));

        return compilationsEvents;
    }

    @Override
    public List<Event> findUsersEventsWithoutCategoryAndRequest(List<Long> usersIds) {
        String query = "SELECT e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, " +
                "e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, e.request_moderation, e.created_on, " +
                "e.published_on, e.state, " +
                "u.user_id, u.user_name " +
                "FROM event AS e " +
                "INNER JOIN users AS u ON e.initiator_id = u.user_id " +
                "WHERE e.initiator_id IN (:usersIds)";
        SqlParameterSource namedParams = new MapSqlParameterSource("usersIds", usersIds);

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToEventNoCategoryAndRequest);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    private Event mapRowToEventNoCategory(ResultSet resultSet, int rowNum) throws SQLException {
        return getEventBuilderWithBaseFields(resultSet, rowNum)
                .confirmedRequests(resultSet.getInt("confirmed_requests"))
                .build();
    }

    private Event mapRowToEventNoCategoryAndRequest(ResultSet resultSet, int rowNum) throws SQLException {
        return getEventBuilderWithBaseFields(resultSet, rowNum).build();
    }

    private Event mapRowToEventFull(ResultSet resultSet, int rowNum) throws SQLException {
        return getEventBuilderWithBaseFields(resultSet, rowNum)
                .category(new Category(
                        resultSet.getInt("category_id"),
                        resultSet.getString("category_name")))
                .confirmedRequests(resultSet.getInt("confirmed_requests"))
                .build();
    }

    private Event.EventBuilder getEventBuilderWithBaseFields(ResultSet resultSet, int rowNum) throws SQLException {
        return Event.builder()
                .id(resultSet.getLong("event_id"))
                .title(resultSet.getString("title"))
                .annotation(resultSet.getString("annotation"))
                .description(resultSet.getString("description"))
                .eventDate(LocalDateTime.parse(resultSet.getString("event_date"), formatter))
                .initiator(new User(
                        resultSet.getLong("user_id"),
                        resultSet.getString("user_name")))
                .paid(resultSet.getBoolean("paid"))
                .location(new Location(
                        resultSet.getFloat("latitude"),
                        resultSet.getFloat("longitude")
                ))
                .participantLimit(resultSet.getInt("participant_limit"))
                .requestModeration(resultSet.getBoolean("request_moderation"))
                .createdOn(LocalDateTime.parse(resultSet.getString("created_on"), formatter))
                .publishedOn(resultSet.getString("published_on") == null ?
                        null : LocalDateTime.parse(resultSet.getString("published_on"), formatter))
                .state(EventState.valueOf(resultSet.getString("state")));

    }

    private boolean hasWhereBlock(AdminSearchParamsDto searchParams) {
        return searchParams.getUsersIds() != null ||
                searchParams.getCategoriesIds() != null ||
                searchParams.getStates() != null ||
                searchParams.getRangeStart() != null ||
                searchParams.getRangeEnd() != null;

    }

    private StringBuilder getStandardSelectEventQueryBuilder() {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, ");
        queryBuilder.append("e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, ");
        queryBuilder.append("e.request_moderation, e.created_on, e.published_on, e.state, e.published_on, e.state, ");
        queryBuilder.append("c.category_id, c.category_name, ");
        queryBuilder.append("u.user_id, u.user_name, ");
        queryBuilder.append("COUNT(r.request_id) AS confirmed_requests ");
        queryBuilder.append("FROM event AS e ");
        queryBuilder.append("INNER JOIN category AS c ON e.category_id = c.category_id ");
        queryBuilder.append("INNER JOIN users AS u ON e.initiator_id = u.user_id ");
        queryBuilder.append("LEFT JOIN event_participation_request AS r ON e.event_id = r.event_id ");
        queryBuilder.append("AND r.request_status = :requestStatus ");
        return queryBuilder;
    }

    private String getSelectQueryWithUserCategoryAndRequest() {
        return "SELECT e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, " +
                "e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, e.request_moderation, e.created_on, " +
                "e.published_on, e.state, " +
                "c.category_id, c.category_name, " +
                "u.user_id, u.user_name, " +
                "COUNT(r.request_id) AS confirmed_requests " +
                "FROM event AS e " +
                "INNER JOIN category AS c ON e.category_id = c.category_id " +
                "INNER JOIN users AS u ON e.initiator_id = u.user_id " +
                "LEFT JOIN event_participation_request AS r ON e.event_id = r.event_id " +
                "AND r.request_status = :requestStatus ";
    }

    private List<String> mapEventStatesToString(Set<EventState> eventStates) {
        if (eventStates != null) {
            return eventStates.stream()
                    .map(Enum::toString)
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }
}
