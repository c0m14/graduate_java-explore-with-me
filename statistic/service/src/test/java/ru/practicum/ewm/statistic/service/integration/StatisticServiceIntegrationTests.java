package ru.practicum.ewm.statistic.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.dto.Formats;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;
import ru.practicum.ewm.statistic.service.model.EndpointHit;
import ru.practicum.ewm.statistic.service.repository.StatisticServiceRepository;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatisticServiceIntegrationTests {

    private static final String HOST = "http://localhost:";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_PATTERN);
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private StatisticServiceRepository statisticRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value(value = "${local.server.port}")
    private int port;
    private URI hitUrl;
    private URI startURL;

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate.update("DELETE FROM endpoint_hit");
        hitUrl = URI.create(HOST + port + "/hit");
        startURL = URI.create(HOST + port + "/stats");
    }

    @Test
    void endpointHitSaved() {
        LocalDateTime timestamp = LocalDateTime.parse("2022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("app")
                .uri("/uri")
                .ip("1.1.1.1")
                .timestamp(timestamp)
                .build();

        testRestTemplate.postForEntity(hitUrl, endpointHitDto, Void.class);

        EndpointHit savedHit = jdbcTemplate.query("SELECT * FROM endpoint_hit", this::mapRowToEndpointHit).get(0);
        assertThat(savedHit.getApp(), equalTo(endpointHitDto.getApp()));
        assertThat(savedHit.getUri(), equalTo(endpointHitDto.getUri()));
        assertThat(savedHit.getIp(), equalTo(endpointHitDto.getIp()));
        assertThat(savedHit.getTimestamp(), equalTo(endpointHitDto.getTimestamp()));
    }

    @Test
    void getViewStatsWithEmptyUrisList() {
        EndpointHit endpointHit = getDefaultEndpointHit();
        statisticRepository.save(endpointHit);
        Map<String, Object> queryParams = Map.of(
                "start", "2023-01-01 00:00:00",
                "end", "2024-01-01 00:00:00",
                "unique", false);
        testRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(startURL.toString()));

        List<ViewStatsDto> stat = testRestTemplate.exchange(
                "?start={start}&end={end}&unique={unique}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {
                },
                queryParams
        ).getBody();

        ViewStatsDto foundRecord = stat.get(0);
        assertThat(foundRecord.getUri(), equalTo(endpointHit.getUri()));
        assertThat(foundRecord.getApp(), equalTo(endpointHit.getApp()));
        assertThat(foundRecord.getHits(), equalTo(1L));
    }

    @Test
    void getViewStatsWithUrisListFilter() {
        String requestedUri = "/requestedUri";
        EndpointHit requestedUriHit = getEndpointHitWithUri(requestedUri);
        EndpointHit otherUriHit = getEndpointHitWithUri("/otherUri");
        statisticRepository.save(requestedUriHit);
        statisticRepository.save(otherUriHit);
        Map<String, Object> queryParams = Map.of(
                "start", "2023-01-01 00:00:00",
                "end", "2024-01-01 00:00:00",
                "uris", requestedUri,
                "unique", false);
        testRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(startURL.toString()));

        List<ViewStatsDto> stat = testRestTemplate.exchange(
                "?start={start}&end={end}&uris={uris}&unique={unique}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {
                },
                queryParams
        ).getBody();

        ViewStatsDto foundRecord = stat.get(0);
        assertThat(foundRecord.getUri(), equalTo(requestedUriHit.getUri()));
        assertThat(foundRecord.getApp(), equalTo(requestedUriHit.getApp()));
        assertThat(foundRecord.getHits(), equalTo(1L));
    }

    private EndpointHit getDefaultEndpointHit() {
        LocalDateTime timestamp = LocalDateTime.parse("2023-07-01 12:00:00", formatter);
        return EndpointHit.builder()
                .app("app")
                .uri("/uri")
                .ip("1.1.1.1")
                .timestamp(timestamp)
                .build();
    }

    private EndpointHit getEndpointHitWithUri(String uri) {
        LocalDateTime timestamp = LocalDateTime.parse("2023-07-01 12:00:00", formatter);
        return EndpointHit.builder()
                .app("app")
                .uri(uri)
                .ip("1.1.1.1")
                .timestamp(timestamp)
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
