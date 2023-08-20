package ru.practicum.ewm.main.request.service;

import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> findUserRequests(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> findRequestsToEvent(Long eventOwnerId, Long eventId);

    EventRequestStatusUpdateResult updateRequestsStatuses(Long eventOwnerId, Long eventId,
                                                          EventRequestStatusUpdateRequest updateStatusRequest);
}
