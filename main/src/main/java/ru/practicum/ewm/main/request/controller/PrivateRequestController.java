package ru.practicum.ewm.main.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.request.service.RequestService;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PrivateRequestController {

    private final RequestService requestService;

    @PostMapping("/users/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequestFromUser(@PathVariable(name = "userId") Long userId,
                                                      @RequestParam(name = "eventId", required = true) Long eventId) {
        log.info("Start POST /users/{userId}/requests with userId: {}, eventId: {}", userId, eventId);
        ParticipationRequestDto savedRequest = requestService.addRequest(userId, eventId);
        log.info("Finish POST /users/{userId}/requests with saved request: {}", savedRequest);
        return savedRequest;
    }

    @GetMapping("/users/{userId}/requests")
    public List<ParticipationRequestDto> findUsersRequests(@PathVariable(name = "userId") Long userId) {
        log.info("Start GET /users/{userId}/requests with userId:{}", userId);
        List<ParticipationRequestDto> foundRequests = requestService.findUserRequests(userId);
        log.info("Finish GET /users/{userId}/requests with {}", foundRequests);
        return foundRequests;
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequestByUser(@PathVariable(name = "userId") Long userId,
                                                       @PathVariable(name = "requestId") Long requestId) {
        log.info("Start PATCH /users/{userId}/requests/{requestId}/cancel with userId: {}, requestId: {}",
                userId, requestId);
        ParticipationRequestDto canceledRequest = requestService.cancelRequest(userId, requestId);
        log.info("Finish PATCH /users/{userId}/requests/{requestId}/cancel with canceled request: {}",
                canceledRequest);
        return canceledRequest;
    }
}
