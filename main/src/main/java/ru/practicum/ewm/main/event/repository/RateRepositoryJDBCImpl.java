package ru.practicum.ewm.main.event.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class RateRepositoryJDBCImpl implements RateRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void addRate(Long userId, Long eventId, int rate) {
        String query = "INSERT INTO  user_event_rate (user_id, event_id, rate) " +
                "VALUES (:userId, :eventId, :rate)";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("eventId", eventId)
                .addValue("rate", rate);

        try {
            jdbcTemplate.update(query, namedParams);
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenException(
                    "Forbidden",
                    "Can't be more than one like/dislike for event from one user"
            );
        }
    }

    @Override
    public Long getRatingForEvent(Long eventId) {
        String query = "SELECT SUM(rate) " +
                "FROM user_event_rate " +
                "WHERE event_id = :eventId";
        SqlParameterSource namedParams = new MapSqlParameterSource("eventId", eventId);

        Optional<Long> rate = Optional.ofNullable(jdbcTemplate.queryForObject(query, namedParams, Long.class));

        return rate.orElse(0L);
    }

    @Override
    public Map<Long, Long> getRatingsForEvents(List<Long> eventsIds) {
        String query = "SELECT event_id, SUM(rate) AS rating " +
                "FROM user_event_rate " +
                "WHERE event_id IN (:eventsIds) " +
                "GROUP BY event_id";
        SqlParameterSource namedParams = new MapSqlParameterSource("eventsIds", eventsIds);

        List<Map<Long, Long>> eventsRatesList = jdbcTemplate.query(query, namedParams,
                ((rs, rowNum) -> Collections.singletonMap(
                        rs.getLong("event_id"),
                        rs.getLong("rating"))
                ));

        Optional<Map<Long, Long>> eventsRates = eventsRatesList.stream()
                .reduce((firstMap, secondMap) -> Stream.concat(firstMap.entrySet().stream(), secondMap.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                Long::sum)));

        return eventsRates.orElse(Map.of());
    }

    @Override
    public void deleteRate(Long userId, Long eventId, int rate) {
        String query = "DELETE FROM user_event_rate " +
                "WHERE user_id = :userId " +
                "AND event_id = :eventId " +
                "AND rate = :rate";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("eventId", eventId)
                .addValue("rate", rate);

        boolean isDeleted = jdbcTemplate.update(query, namedParams) > 0;

        if (!isDeleted) {
            throw new NotExistsException(
                    "Rate",
                    String.format("There is no :%s from user with id %d to event with id %d",
                            rate == 1 ? "like" : "dislike", userId, eventId)
            );
        }
    }
}
