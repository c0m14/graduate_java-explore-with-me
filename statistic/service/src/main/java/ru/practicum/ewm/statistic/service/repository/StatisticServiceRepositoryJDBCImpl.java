package ru.practicum.ewm.statistic.service.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.statistic.dto.Formats;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;
import ru.practicum.ewm.statistic.service.model.EndpointHit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatisticServiceRepositoryJDBCImpl implements StatisticServiceRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_PATTERN);

    @Override
    public void save(EndpointHit endpointHit) {
        String query = "insert into endpoint_hit (app_name, app_uri, ip, timestamp) " +
                "values (:app, :uri, :ip, :timestamp)";
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("app", endpointHit.getApp())
                .addValue("uri", endpointHit.getUri())
                .addValue("ip", endpointHit.getIp())
                .addValue("timestamp", endpointHit.getTimestamp());

        jdbcTemplate.update(query, namedParams);
    }

    @Override
    public List<ViewStatsDto> getViewStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        StringBuilder queryBuilder = new StringBuilder();
        if (unique) {
            queryBuilder.append("select app_name, app_uri, count(distinct ip) as hits ");
        } else {
            queryBuilder.append("select app_name, app_uri, count(ip) as hits ");
        }
        queryBuilder.append("from endpoint_hit ");
        queryBuilder.append("where timestamp > :start ");
        queryBuilder.append("and timestamp < :end ");
        if (uris != null && !uris.isEmpty()) {
            queryBuilder.append("and app_uri in (:uris) ");
        }
        queryBuilder.append("group by app_name, app_uri ");
        queryBuilder.append("order by hits DESC");

        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("start", start)
                .addValue("end", end)
                .addValue("uris", uris);

        try {
            return jdbcTemplate.query(queryBuilder.toString(), namedParams, this::mapRowToViewStatsDto);
        } catch (EmptyResultDataAccessException ex) {
            return List.of();
        }
    }

    @Override
    public void deleteAll() {
        String query = "delete from endpoint_hit";

        jdbcTemplate.update(query, new MapSqlParameterSource());
    }

    @Override
    public List<EndpointHit> findAll() {
        String query = "select hit_id, app_name, app_uri, ip, timestamp " +
                "from endpoint_hit";
        try {
            return jdbcTemplate.query(query, this::mapRowToEndpointHit);
        } catch (EmptyResultDataAccessException ex) {
            return List.of();
        }
    }

    private ViewStatsDto mapRowToViewStatsDto(ResultSet resultSet, int rowNum) throws SQLException {
        return ViewStatsDto.builder()
                .app(resultSet.getString("app_name"))
                .uri(resultSet.getString("app_uri"))
                .hits(resultSet.getLong("hits"))
                .build();
    }

    private EndpointHit mapRowToEndpointHit(ResultSet resultSet, int rowNum) throws SQLException {
        return EndpointHit.builder()
                .id(resultSet.getLong("hit_id"))
                .app(resultSet.getString("app_name"))
                .uri(resultSet.getString("app_uri"))
                .ip(resultSet.getString("ip"))
                .timestamp(LocalDateTime.parse(resultSet.getString("timestamp"), formatter))
                .build();
    }
}
