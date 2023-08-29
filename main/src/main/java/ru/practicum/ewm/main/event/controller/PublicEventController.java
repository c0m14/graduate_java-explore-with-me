package ru.practicum.ewm.main.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.dto.searchrequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.SearchSortOptionDto;
import ru.practicum.ewm.main.event.model.RateType;
import ru.practicum.ewm.main.event.service.EventService;
import ru.practicum.ewm.main.exception.InvalidParamException;
import ru.practicum.ewm.statistic.dto.Formats;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class PublicEventController {

    private final EventService eventService;

    @GetMapping("/events")
    public List<EventShortDto> findEvents(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "categories", required = false) Set<Integer> categoriesIds,
            @RequestParam(name = "paid", required = false) Boolean paid,

            @RequestParam(name = "rangeStart", required = false)
            @DateTimeFormat(pattern = Formats.DATE_TIME_PATTERN) LocalDateTime rangeStart,

            @RequestParam(name = "rangeEnd", required = false)
            @DateTimeFormat(pattern = Formats.DATE_TIME_PATTERN) LocalDateTime rangeEnd,

            @RequestParam(name = "onlyAvailable", required = false, defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(name = "sort", required = false) String sort,
            @PositiveOrZero @RequestParam(name = "from", required = false, defaultValue = "0") Integer from,
            @Positive @RequestParam(name = "size", required = false, defaultValue = "10") Integer size,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Start GET /events with text: {}, categories: {}, paid: {}, rangeStart: {}, rangeEnd: {}," +
                        "onlyAvailable: {}, sort: {}, from: {}, size: {}, ip: {}",
                text, categoriesIds, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size,
                httpServletRequest.getRemoteAddr());
        SearchSortOptionDto sortOption = null;
        if (sort != null) {
            sortOption = SearchSortOptionDto.from(sort)
                    .orElseThrow(() -> new InvalidParamException("Event sort option", "Unknown state: " + sort));
        }
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .text(text)
                .categoriesIds(categoriesIds)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sortOption(sortOption)
                .from(from)
                .size(size)
                .build();
        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, httpServletRequest.getRemoteAddr());
        log.info("Finish GET /events with {}", foundEvents);
        return foundEvents;
    }

    @GetMapping("/events/{id}")
    public EventFullDto findEventById(
            @PathVariable(name = "id") Long eventId,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Start GET /events/{id} with eventId: {}, ip: {}", eventId, httpServletRequest.getRemoteAddr());
        EventFullDto foundEvent = eventService.findEventByIdPublic(eventId, httpServletRequest.getRemoteAddr());
        log.info("Finish GET /events/{id} with {}", foundEvent);
        return foundEvent;
    }

    @PatchMapping("/users/{userId}/events/{eventId}/like")
    public void addLikeToEvent(@PathVariable(name = "userId") Long userId,
                               @PathVariable(name = "eventId") Long eventId) {
        log.info("Start POST /users/{userId}/events/{eventId}/like with userId: {}, eventId: {}", userId, eventId);
        eventService.addRateToEvent(userId, eventId, RateType.LIKE);
        log.info("Finish POST /users/{userId}/events/{eventId}/like with userId: {}, eventId: {}", userId, eventId);
    }

    @PatchMapping("/users/{userId}/events/{eventId}/dislike")
    public void addDislikeToEvent(@PathVariable(name = "userId") Long userId,
                                  @PathVariable(name = "eventId") Long eventId) {
        log.info("Start POST /users/{userId}/events/{eventId}/dislike with userId: {}, eventId: {}", userId, eventId);
        eventService.addRateToEvent(userId, eventId, RateType.DISLIKE);
        log.info("Start POST /users/{userId}/events/{eventId}/dislike with userId: {}, eventId: {}", userId, eventId);
    }

    @DeleteMapping("/users/{userId}/events/{eventId}/like")
    public void deleteLikeFromEvent(@PathVariable(name = "userId") Long userId,
                                    @PathVariable(name = "eventId") Long eventId) {
        log.info("Start DELETE /users/{userId}/events/{eventId}/like with userId: {}, eventId: {}", userId, eventId);
        eventService.deleteRateFromEvent(userId, eventId, RateType.LIKE);
        log.info("Finish DELETE /users/{userId}/events/{eventId}/like with userId: {}, eventId: {}", userId, eventId);
    }

    @DeleteMapping("/users/{userId}/events/{eventId}/dislike")
    public void deleteDislikeFromEvent(@PathVariable(name = "userId") Long userId,
                                       @PathVariable(name = "eventId") Long eventId) {
        log.info("Start DELETE /users/{userId}/events/{eventId}/dislike with userId: {}, eventId: {}", userId, eventId);
        eventService.deleteRateFromEvent(userId, eventId, RateType.DISLIKE);
        log.info("Start DELETE /users/{userId}/events/{eventId}/dislike with userId: {}, eventId: {}", userId, eventId);
    }


}
