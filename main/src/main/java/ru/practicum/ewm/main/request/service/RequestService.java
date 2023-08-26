package ru.practicum.ewm.main.request.service;

import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequestDto;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateResultDto;
import ru.practicum.ewm.main.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> findUserRequests(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> findRequestsToEvent(Long eventOwnerId, Long eventId);

    EventRequestStatusUpdateResultDto updateRequestsStatuses(Long eventOwnerId, Long eventId,
                                                             EventRequestStatusUpdateRequestDto updateStatusRequest);
}
