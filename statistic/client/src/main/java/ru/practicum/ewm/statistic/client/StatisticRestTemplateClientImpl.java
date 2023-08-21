package ru.practicum.ewm.statistic.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.dto.Formats;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class StatisticRestTemplateClientImpl implements StatisticClient {
    private static final String HIT_URI_PREFIX = "/hit";
    private static final String STATS_URI_PREFIX = "/stats";
    private final RestTemplate restTemplate;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_PATTERN);

    public StatisticRestTemplateClientImpl(@Value("${statistic-service.url}") String serverUrl) {
        this.restTemplate = new RestTemplateBuilder()
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build();
    }

    @Override
    public void saveEndpointHit(EndpointHitDto endpointHitDto) {
        RequestEntity<EndpointHitDto> requestEntity = RequestEntity
                .post(HIT_URI_PREFIX)
                .body(endpointHitDto);

        restTemplate.exchange(
                requestEntity,
                Void.class
        );
    }

    @Override
    public List<ViewStatsDto> getViewStats(
            LocalDateTime start,
            LocalDateTime end,
            List<String> uris,
            boolean unique) {
        Map<String, Object> queryParams = Map.of(
                "start", start.format(formatter),
                "end", end.format(formatter),
                "uris", uris.toArray(),
                "unique", String.valueOf(unique)
        );


        return restTemplate.exchange(
                STATS_URI_PREFIX + "?start={start}&end={end}&uris={uris}&unique={unique}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {
                },
                queryParams
        ).getBody();
    }
}
