package ru.practicum.ewm.statistic.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StatisticRestTemplateClientImpl implements StatisticClient {
    private final String HIT_URI_PREFIX = "/hit";
    private final String STATS_URI_PREFIX = "/stats";
    private final RestTemplate restTemplate;

    public StatisticRestTemplateClientImpl(@Value("${statistic-service.url}") String serverUrl) {
        this.restTemplate = new RestTemplateBuilder()
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build();
    }

    @Override
    public ResponseEntity<Void> saveEndpointHit(EndpointHitDto endpointHitDto) {
        RequestEntity<EndpointHitDto> requestEntity = RequestEntity
                .post(HIT_URI_PREFIX)
                .body(endpointHitDto);

        restTemplate.exchange(
                requestEntity,
                Void.class
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<List<ViewStatsDto>> getViewStats(
            LocalDateTime start,
            LocalDateTime end,
            List<String> uris,
            boolean unique) {
        String url = UriComponentsBuilder.fromHttpUrl(STATS_URI_PREFIX)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .toUriString();

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {
                }
        );
    }
}
