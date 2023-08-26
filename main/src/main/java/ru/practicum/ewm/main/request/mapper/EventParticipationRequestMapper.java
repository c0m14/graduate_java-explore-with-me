package ru.practicum.ewm.main.request.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.request.model.EventParticipationRequest;

@UtilityClass
public class EventParticipationRequestMapper {

    public ParticipationRequestDto mapToParticipationRequestDto(EventParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .requester(request.getRequester().getId())
                .event(request.getEvent().getId())
                .created(request.getCreated())
                .status(request.getRequestStatus())
                .build();
    }
}
