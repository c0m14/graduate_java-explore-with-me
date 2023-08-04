package ru.practicum.ewm.statistic.service.repository;

import ru.practicum.ewm.statistic.dto.ViewStatsDto;
import ru.practicum.ewm.statistic.service.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticServiceRepository {
    void save(EndpointHit endpointHit);

    void deleteAll();

    List<EndpointHit> findAll();

    List<ViewStatsDto> getViewStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
