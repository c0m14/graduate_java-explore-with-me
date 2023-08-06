package ru.practicum.ewm.statistic.service.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.service.model.EndpointHit;

@UtilityClass
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
