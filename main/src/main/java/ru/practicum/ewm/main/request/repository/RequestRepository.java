package ru.practicum.ewm.main.request.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.request.model.EventParticipationRequest;
import ru.practicum.ewm.main.request.model.RequestStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RequestRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<EventParticipationRequest> getUserRequestsForEvent(Long userId, Long eventId) {
        String query = "SELECT request_id, created, event_id, requester_id, request_status " +
                "FROM event_participation_request " +
                "WHERE requester_id = :userId " +
                "AND event_id = :eventId";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("eventId", eventId);

        try {
            return jdbcTemplate.query(query, namedParams, this::mapRowToRequestShort);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    @Transactional
    public List<Long> updateRequestsStatusForEventByOwner(
            Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        String query = "UPDATE event_participation_request " +
                "SET request_status = :status " +
                "WHERE requester_id = :userId " +
                "AND event_id = :eventId " +
                "AND request_id IN (:requestsIds) " +
                "RETURNING request_id";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("status", updateRequest.getStatus())
                .addValue("userId", userId)
                .addValue("eventId", eventId)
                .addValue("requestsIds", updateRequest.getRequestIds());

        return jdbcTemplate.queryForList(query, namedParams, Long.class);

    }

    private EventParticipationRequest mapRowToRequestShort(ResultSet resultSet, int rowNum) throws SQLException {
        return getRequestBuilderWithBaseFields(resultSet, rowNum)
                .created(resultSet.getTimestamp("created").toInstant())
                .build();
    }

    private EventParticipationRequest.EventParticipationRequestBuilder getRequestBuilderWithBaseFields(
            ResultSet resultSet, int rowNum) throws SQLException {
        return EventParticipationRequest.builder()
                .id(resultSet.getLong("request_id"))
                .requestStatus(RequestStatus.valueOf(resultSet.getString("request_status")));
    }
}
