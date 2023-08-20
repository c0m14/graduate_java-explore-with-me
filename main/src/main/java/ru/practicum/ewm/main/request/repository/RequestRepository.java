package ru.practicum.ewm.main.request.repository;

import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.request.model.EventParticipationRequest;

import java.util.List;
import java.util.Optional;

public interface RequestRepository {

    EventParticipationRequest save(EventParticipationRequest request);

    List<EventParticipationRequest> findByUserId(Long userId);

    Optional<EventParticipationRequest> findByUserIdAndRequestId(Long userId, Long requestId);

    EventParticipationRequest update(EventParticipationRequest request);

    List<EventParticipationRequest> findRequestsForEvent(Long eventId);

    List<EventParticipationRequest> findRequestsForEvent(Long eventId, List<Long> requestsIds);

    List<Long> updateRequestsStatusForEvent(Long eventId, EventRequestStatusUpdateRequest updateRequest);
}