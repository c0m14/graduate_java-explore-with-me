package ru.practicum.ewm.main.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequestDto;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateResultDto;
import ru.practicum.ewm.main.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.request.dto.RequestStatusUpdateDto;
import ru.practicum.ewm.main.request.mapper.EventParticipationRequestMapper;
import ru.practicum.ewm.main.request.model.EventParticipationRequest;
import ru.practicum.ewm.main.request.model.RequestStatus;
import ru.practicum.ewm.main.request.repository.RequestRepository;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User requester = getUserFromDB(userId);
        Event event = getEventFromDB(eventId, true);
        checkEvent(event, userId);

        EventParticipationRequest request = EventParticipationRequest.builder()
                .requester(requester)
                .event(event)
                .created(LocalDateTime.now().withNano(0))
                .requestStatus(defineRequestStatus(event))
                .build();

        EventParticipationRequest savedRequest = requestRepository.save(request);

        return EventParticipationRequestMapper.mapToParticipationRequestDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> findUserRequests(Long userId) {
        checkIfUserExist(userId);

        List<EventParticipationRequest> foundRequests = requestRepository.findByUserId(userId);

        return foundRequests.stream()
                .map(EventParticipationRequestMapper::mapToParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        EventParticipationRequest request = getRequestFromDbByIdAndOwner(userId, requestId);

        switch (request.getRequestStatus()) {
            case PENDING:
            case CONFIRMED:
                request.setRequestStatus(RequestStatus.CANCELED);
                requestRepository.update(request);
                break;
            case REJECTED:
                throw new ForbiddenException(
                        "Forbidden",
                        String.format("Can't cancel request with status %s", RequestStatus.REJECTED)
                );
            case CANCELED:
                break;
        }

        return EventParticipationRequestMapper.mapToParticipationRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> findRequestsToEvent(Long eventOwnerId, Long eventId) {
        Event event = getEventFromDB(eventId, false);
        checkIfUserOwnEvent(eventOwnerId, event);

        List<EventParticipationRequest> foundRequests = requestRepository.findRequestsForEvent(eventId);

        return foundRequests.stream()
                .map(EventParticipationRequestMapper::mapToParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResultDto updateRequestsStatuses(
            Long eventOwnerId,
            Long eventId,
            EventRequestStatusUpdateRequestDto updateStatusRequest) {
        Event event = getEventFromDB(eventId, true);
        checkIfUserOwnEvent(eventOwnerId, event);

        List<EventParticipationRequest> requests =
                requestRepository.findRequestsForEvent(eventId, updateStatusRequest.getRequestIds());
        checkIfAvailableToChangeStatus(requests, updateStatusRequest.getStatus());

        switch (updateStatusRequest.getStatus()) {
            case CONFIRMED:
                return confirmRequests(event, updateStatusRequest, requests);
            case REJECTED:
                return rejectRequests(event, updateStatusRequest, requests);
            default:
                throw new ForbiddenException("Forbidden", "Wrong status");
        }
    }

    private User getUserFromDB(Long userId) {
        return userRepository.findUserById(userId).orElseThrow(
                () -> new NotExistsException(
                        "User",
                        String.format("User with id %d not exists", userId)
                )
        );
    }

    private void checkIfUserExist(Long userId) {
        if (!userRepository.userExists(userId)) {
            throw new NotExistsException(
                    "User",
                    String.format("User with id %d not exists", userId)
            );
        }
    }

    private Event getEventFromDB(Long eventId, boolean withLocking) {
        if (withLocking) {
            eventRepository.lockEventForShare(eventId);
        }
        return eventRepository.findEventByIdWithoutCategory(eventId).orElseThrow(
                () -> new NotExistsException(
                        "Event",
                        String.format("Event with id %d not exists", eventId)
                )
        );
    }

    private EventParticipationRequest getRequestFromDbByIdAndOwner(Long userId, Long requestId) {
        return requestRepository.findByUserIdAndRequestId(userId, requestId).orElseThrow(
                () -> new NotExistsException(
                        "Participation request",
                        String.format("There is no request with id %d, from user with id %d", requestId, userId)
                )
        );
    }

    private void checkEvent(Event event, Long requesterId) {
        checkIfRequesterIsEventOwner(event, requesterId);
        checkEventStatus(event);
        checkIfParticipationLimitReached(event);
    }

    private void checkEventStatus(Event event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ForbiddenException(
                    "Forbidden",
                    String.format("Wrong event status for participation request: %s", event.getState())
            );
        }
    }

    private void checkIfRequesterIsEventOwner(Event event, Long requesterId) {
        if (requesterId.equals(event.getInitiator().getId())) {
            throw new ForbiddenException(
                    "Forbidden",
                    String.format("Requester and event initiator must not beent same users, user id: %d",
                            requesterId)
            );
        }
    }

    private void checkIfParticipationLimitReached(Event event) {
        int limit = event.getParticipantLimit();
        if (limit > 0) {
            if (event.getConfirmedRequests() >= limit) {
                throw new ForbiddenException(
                        "Forbidden",
                        String.format("Participation limit reached: %d/%d", event.getConfirmedRequests(), limit)
                );
            }
        }
    }

    private RequestStatus defineRequestStatus(Event event) {
        if (event.getParticipantLimit() == 0) {
            return RequestStatus.CONFIRMED;
        } else if (event.isRequestModeration()) {
            return RequestStatus.PENDING;
        } else {
            return RequestStatus.CONFIRMED;
        }
    }

    private void checkIfUserOwnEvent(Long userId, Event event) {
        if (!userId.equals(event.getInitiator().getId())) {
            throw new ForbiddenException(
                    "Forbidden",
                    String.format("User with id %d is not initiator for event with id %d", userId, event.getId())
            );
        }
    }

    private void checkIfAvailableToChangeStatus(List<EventParticipationRequest> requests,
                                                RequestStatusUpdateDto updateStatus) {
        requests.forEach(request -> {
            if (updateStatus.equals(RequestStatusUpdateDto.CONFIRMED) &&
                    !request.getRequestStatus().equals(RequestStatus.PENDING) ||
                    updateStatus.equals(RequestStatusUpdateDto.REJECTED) &&
                            request.getRequestStatus().equals(RequestStatus.CONFIRMED)) {
                throw new ForbiddenException(
                        "Forbidden",
                        String.format("Can't change status to request with id %d, because status is final: %s",
                                request.getId(), request.getRequestStatus())
                );
            }
        });
    }

    private EventRequestStatusUpdateResultDto confirmRequests(
            Event event,
            EventRequestStatusUpdateRequestDto updateStatusRequest,
            List<EventParticipationRequest> requests
    ) {
        checkIfParticipationLimitReached(event);
        int remains = event.getParticipantLimit() - event.getConfirmedRequests();

        if (remains < updateStatusRequest.getRequestIds().size()) {
            List<Long> idsToConfirm = updateStatusRequest.getRequestIds().stream()
                    .limit(remains)
                    .collect(Collectors.toList());
            List<Long> idsToReject = updateStatusRequest.getRequestIds().stream()
                    .skip(remains)
                    .collect(Collectors.toList());

            List<Long> confirmedIds = requestRepository.updateRequestsStatusForEvent(event.getId(),
                    new EventRequestStatusUpdateRequestDto(idsToConfirm, RequestStatusUpdateDto.CONFIRMED));
            List<Long> rejectedIds = requestRepository.updateRequestsStatusForEvent(event.getId(),
                    new EventRequestStatusUpdateRequestDto(idsToReject, RequestStatusUpdateDto.REJECTED));

            return EventRequestStatusUpdateResultDto.builder()
                    .confirmedRequests(
                            requests.stream()
                                    .filter(request -> confirmedIds.contains(request.getId()))
                                    .peek(request -> request.setRequestStatus(RequestStatus.CONFIRMED))
                                    .map(EventParticipationRequestMapper::mapToParticipationRequestDto)
                                    .collect(Collectors.toList()))
                    .rejectedRequests(
                            requests.stream()
                                    .filter(request -> rejectedIds.contains(request.getId()))
                                    .peek(request -> request.setRequestStatus(RequestStatus.REJECTED))
                                    .map(EventParticipationRequestMapper::mapToParticipationRequestDto)
                                    .collect(Collectors.toList()))
                    .build();
        } else {
            List<Long> confirmedIds = requestRepository.updateRequestsStatusForEvent(event.getId(), updateStatusRequest);

            return EventRequestStatusUpdateResultDto.builder()
                    .confirmedRequests(
                            requests.stream()
                                    .filter(request -> confirmedIds.contains(request.getId()))
                                    .peek(request -> request.setRequestStatus(RequestStatus.CONFIRMED))
                                    .map(EventParticipationRequestMapper::mapToParticipationRequestDto)
                                    .collect(Collectors.toList()))
                    .build();
        }
    }

    private EventRequestStatusUpdateResultDto rejectRequests(
            Event event,
            EventRequestStatusUpdateRequestDto updateStatusRequest,
            List<EventParticipationRequest> requests
    ) {
        List<Long> rejectedIds = requestRepository.updateRequestsStatusForEvent(event.getId(), updateStatusRequest);

        return EventRequestStatusUpdateResultDto.builder()
                .rejectedRequests(
                        requests.stream()
                                .filter(request -> rejectedIds.contains(request.getId()))
                                .peek(request -> request.setRequestStatus(RequestStatus.REJECTED))
                                .map(EventParticipationRequestMapper::mapToParticipationRequestDto)
                                .collect(Collectors.toList()))
                .build();
    }

}
