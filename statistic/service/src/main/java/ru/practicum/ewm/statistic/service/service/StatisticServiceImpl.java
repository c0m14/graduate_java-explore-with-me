package ru.practicum.ewm.statistic.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;
import ru.practicum.ewm.statistic.service.mapper.EndpointHitMapper;
import ru.practicum.ewm.statistic.service.model.EndpointHit;
import ru.practicum.ewm.statistic.service.repository.StatisticServiceRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticServiceImpl implements StatisticService {
    private final StatisticServiceRepository repository;

    @Override
    public void saveEndpointHit(EndpointHitDto endpointHitDto) {
        EndpointHit endpointHit = EndpointHitMapper.mapToEntity(endpointHitDto);
        repository.save(endpointHit);
        log.info("Finish saving for {}", endpointHitDto);
    }

    @Override
    public List<ViewStatsDto> getViewStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        return repository.getViewStats(start, end, uris, unique);
    }
}
