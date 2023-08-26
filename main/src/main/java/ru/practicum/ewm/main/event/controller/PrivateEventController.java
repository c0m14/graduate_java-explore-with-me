package ru.practicum.ewm.main.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.dto.NewEventDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventUserRequestDto;
import ru.practicum.ewm.main.event.service.EventService;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequestDto;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateResultDto;
import ru.practicum.ewm.main.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.request.service.RequestService;
import ru.practicum.ewm.main.validator.OnCreateValidation;
import ru.practicum.ewm.main.validator.OnUpdateValidation;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class PrivateEventController {

    private final EventService eventService;
    private final RequestService requestService;

    @PostMapping("/users/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Validated(OnCreateValidation.class)
    public EventFullDto addEvent(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody NewEventDto newEventDto) {
        log.info("Start POST /users/{userId}/events with userId: {}, event: {}", userId, newEventDto);
        EventFullDto savedEvent = eventService.addEvent(userId, newEventDto);
        log.info("Finish POST /users/{userId}/events with userId: {}, saved event: {}", userId, savedEvent);
        return savedEvent;
    }

    @GetMapping("/users/{userId}/events")
    public List<EventShortDto> getUsersEvents(
            @PathVariable("userId") Long userId,
            @PositiveOrZero @RequestParam(required = false, defaultValue = "0") int from,
            @Positive @RequestParam(required = false, defaultValue = "10") int size) {
        log.info("Start GET /users/{userId}/events with userId: {}, from: {}, size: {}", userId, from, size);
        List<EventShortDto> foundEvents = eventService.findUsersEvents(userId, from, size);
        log.info("Finish GET /users/{userId}/events with {}", foundEvents);
        return foundEvents;
    }

    @GetMapping("/users/{userId}/events/{eventId}")
    public EventFullDto getEventByOwnerAndId(
            @PathVariable("userId") Long userId,
            @PathVariable("eventId") Long eventId) {
        log.info("Start GET /users/{userId}/events/{eventId} with userId: {}, eventId: {}", userId, eventId);
        EventFullDto foundEvent = eventService.findUserEventById(userId, eventId);
        log.info("Finish GET /users/{userId}/events/{eventId} with {}", foundEvent);
        return foundEvent;
    }

    @PatchMapping("/users/{userId}/events/{eventId}")
    @Validated(OnUpdateValidation.class)
    public EventFullDto updateEventByOwner(
            @PathVariable("userId") Long userId,
            @PathVariable("eventId") Long eventId,
            @Valid @RequestBody UpdateEventUserRequestDto updateEventRequest) {
        log.info("Start PATCH /users/{userId}/events/{eventId} with userId: {}, eventId: {}, " +
                "updateEventRequest: {}", userId, eventId, updateEventRequest);
        EventFullDto updatedEvent = eventService.updateEventByUser(userId, eventId, updateEventRequest);
        log.info("Finish PATCH /users/{userId}/events/{eventId} with {}", updatedEvent);
        return updatedEvent;
    }


    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getParticipationRequestsForEvent(
            @PathVariable("userId") Long eventOwnerId,
            @PathVariable("eventId") Long eventId) {
        log.info("Start GET /users/{userId}/events/{eventId}/requests with userId: {}, eventId: {}", eventOwnerId, eventId);
        List<ParticipationRequestDto> foundRequests = requestService.findRequestsToEvent(eventOwnerId, eventId);
        log.info("Finish GET /users/{userId}/events/{eventId}/requests with: {}", foundRequests);
        return foundRequests;
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResultDto updateParticipationRequestForEventByOwner(
            @PathVariable("userId") Long eventOwnerId,
            @PathVariable("eventId") Long eventId,
            @RequestBody EventRequestStatusUpdateRequestDto statusUpdateRequest) {
        log.info("Start PATCH /users/{userId}/events/{eventId}/requests with userId: {}, eventId: {}, " +
                "statusUpdateRequest: {}", eventOwnerId, eventId, statusUpdateRequest);
        EventRequestStatusUpdateResultDto statusUpdateResult =
                requestService.updateRequestsStatuses(eventOwnerId, eventId, statusUpdateRequest);
        log.info("Finish PATCH /users/{userId}/events/{eventId}/requests with {}", statusUpdateResult);
        return statusUpdateResult;
    }
}
