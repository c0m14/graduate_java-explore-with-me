package ru.practicum.ewm.statistic.client;

import org.springframework.http.ResponseEntity;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticClient {
    ResponseEntity<Void> saveEndpointHit(EndpointHitDto endpointHitDto);

    ResponseEntity<List<ViewStatsDto>> getViewStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
