package ru.practicum.ewm.main.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.request.dto.ParticipationRequestDto;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PublicRequestController {

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getUserParticipationRequestsForEvent(
            @PathVariable("userId") Long userId,
            @PathVariable("eventId") Long eventId) {
        log.info("Start GET /users/{userId}/events/{eventId}/requests with userId: {}, eventId: {}", userId, eventId);
        //TODO calling service;
        log.info("Finish GET /users/{userId}/events/{eventId}/requests with userId: {}, eventId: {}", userId, eventId);
        return null;
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateParticipationRequestForEventByOwner(
            @PathVariable("userId") Long userId,
            @PathVariable("eventId") Long eventId,
            @RequestBody EventRequestStatusUpdateRequest statusUpdateRequest) {
        log.info("Start PATCH /users/{userId}/events/{eventId}/requests with userId: {}, eventId: {}, " +
                "statusUpdateRequest: {}", userId, eventId, statusUpdateRequest);
        //TODO calling service;
        log.info("Finish PATCH /users/{userId}/events/{eventId}/requests with userId: {}, eventId: {}, " +
                "statusUpdateRequest: {}", userId, eventId, statusUpdateRequest);
        return null;
    }
}
