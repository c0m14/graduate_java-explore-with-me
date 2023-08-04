package ru.practicum.ewm.statistic.service.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.service.model.EndpointHit;

@Component
public class EndpointHitMapper {

    public EndpointHit mapToEntity(EndpointHitDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }
}
