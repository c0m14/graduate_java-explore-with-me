package ru.practicum.ewm.main.request.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequestDto;
import ru.practicum.ewm.main.request.model.EventParticipationRequest;
import ru.practicum.ewm.main.request.model.RequestStatus;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.statistic.dto.Formats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RequestRepositoryJDCBImpl implements RequestRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_PATTERN);


    @Override
    public EventParticipationRequest save(EventParticipationRequest request) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("event_participation_request")
                .usingGeneratedKeyColumns("request_id");

        Long requestId;
        try {
            requestId = simpleJdbcInsert.executeAndReturnKey(request.mapToDb()).longValue();
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenException(
                    "Forbidden",
                    String.format("Error with request adding: %s", e.getMessage())
            );
        }

        request.setId(requestId);
        return request;
    }

    @Override
    public List<EventParticipationRequest> findByUserId(Long userId) {
        String query = "SELECT request_id, created, event_id, requester_id, request_status " +
                "FROM event_participation_request " +
                "WHERE requester_id = :userId " +
                "ORDER BY created DESC ";
        SqlParameterSource namedParams = new MapSqlParameterSource("userId", userId);

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToRequestShort);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public Optional<EventParticipationRequest> findByUserIdAndRequestId(Long userId, Long requestId) {
        String query = "SELECT request_id, created, event_id, requester_id, request_status " +
                "FROM event_participation_request " +
                "WHERE requester_id = :userId " +
                "AND request_id = :requestId";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("requestId", requestId);

        try {
            return Optional.of(jdbcTemplate.queryForObject(query, namedParams, this::mapRowToRequestShort));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public EventParticipationRequest update(EventParticipationRequest request) {
        String query = "UPDATE event_participation_request " +
                "SET request_status = :requestStatus " +
                "WHERE request_id = :requestId";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("requestStatus", request.getRequestStatus().toString())
                .addValue("requestId", request.getId());

        jdbcTemplate.update(query, namedParams);

        return request;
    }

    @Override
    public List<EventParticipationRequest> findRequestsForEvent(Long eventId) {
        String query = "SELECT request_id, created, event_id, requester_id, request_status " +
                "FROM event_participation_request " +
                "WHERE event_id = :eventId";
        SqlParameterSource namedParams = new MapSqlParameterSource("eventId", eventId);

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToRequestShort);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public List<EventParticipationRequest> findRequestsForEvent(Long eventId, List<Long> requestsIds) {
        String query = "SELECT request_id, created, event_id, requester_id, request_status " +
                "FROM event_participation_request " +
                "WHERE event_id = :eventId " +
                "AND request_id IN (:requestsIds)";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("requestsIds", requestsIds);

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToRequestShort);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Override
    public List<Long> updateRequestsStatusForEvent(Long eventId, EventRequestStatusUpdateRequestDto updateRequest) {
        String query = "UPDATE event_participation_request " +
                "SET request_status = :status " +
                "WHERE event_id = :eventId " +
                "AND request_id IN (:requestsIds) " +
                "RETURNING request_id";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("status", updateRequest.getStatus().toString())
                .addValue("eventId", eventId)
                .addValue("requestsIds", updateRequest.getRequestIds());

        return jdbcTemplate.queryForList(query, namedParams, Long.class);

    }

    @Override
    public Optional<EventParticipationRequest> findByUserEventAndStatus(Long userId, Long eventId, RequestStatus status) {
        String query = "SELECT request_id, created, event_id, requester_id, request_status " +
                "FROM event_participation_request " +
                "WHERE event_id = :eventId " +
                "AND requester_id = :userId " +
                "AND request_status = :requestStatus";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("userId", userId)
                .addValue("requestStatus", status.toString());

        try {
            return Optional.of(jdbcTemplate.queryForObject(query, namedParams, this::mapRowToRequestShort));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private EventParticipationRequest mapRowToRequestShort(ResultSet resultSet, int rowNum) throws SQLException {
        return getRequestBuilderWithBaseFields(resultSet, rowNum)
                .event(Event.builder().id(resultSet.getLong("event_id")).build())
                .requester(new User(resultSet.getLong("requester_id")))
                .build();
    }

    private EventParticipationRequest.EventParticipationRequestBuilder getRequestBuilderWithBaseFields(
            ResultSet resultSet, int rowNum) throws SQLException {
        return EventParticipationRequest.builder()
                .id(resultSet.getLong("request_id"))
                .requestStatus(RequestStatus.valueOf(resultSet.getString("request_status")))
                .created(LocalDateTime.parse(resultSet.getString("created"), formatter));
    }
}
