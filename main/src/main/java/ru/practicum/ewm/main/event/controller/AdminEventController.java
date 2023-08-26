package ru.practicum.ewm.main.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.searchrequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventAdminRequestDto;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.service.EventService;
import ru.practicum.ewm.main.exception.InvalidParamException;
import ru.practicum.ewm.main.validator.OnUpdateValidation;
import ru.practicum.ewm.statistic.dto.Formats;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class AdminEventController {
    private final EventService eventService;

    @GetMapping("/admin/events")
    List<EventFullDto> findEvents(
            @RequestParam(name = "users", required = false) Set<Long> usersIds,
            @RequestParam(name = "states", required = false) Set<String> stringStates,
            @RequestParam(name = "categories", required = false) Set<Integer> categoriesIds,
            @RequestParam(name = "rangeStart", required = false)
            @DateTimeFormat(pattern = Formats.DATE_TIME_PATTERN) LocalDateTime rangeStart,
            @RequestParam(name = "rangeEnd", required = false)
            @DateTimeFormat(pattern = Formats.DATE_TIME_PATTERN) LocalDateTime rangeEnd,
            @PositiveOrZero @RequestParam(name = "from", required = false, defaultValue = "0") Integer from,
            @Positive @RequestParam(name = "size", required = false, defaultValue = "10") Integer size
    ) {
        log.info("Start GET /admin/events with users: {}, states:{}, categories: {}, " +
                        "rangeStart: {}, rangeEnd: {}, from: {}, size: {}",
                usersIds, stringStates, categoriesIds, rangeStart, rangeEnd, from, size);

        Set<EventState> states = null;
        if (stringStates != null) {
            states = new HashSet<>();
            for (String stringState : stringStates) {
                EventState eventState = EventState.from(stringState).orElseThrow(
                        () -> new InvalidParamException(
                                "Event State",
                                String.format("No such event state available: %s", stringState)
                        )
                );
                states.add(eventState);
            }

        }
        AdminSearchParamsDto searchParams = AdminSearchParamsDto.builder()
                .usersIds(usersIds)
                .states(states)
                .categoriesIds(categoriesIds)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .from(from)
                .size(size)
                .build();
        List<EventFullDto> foundEvents = eventService.findEventsAdmin(searchParams);

        log.info("Finish GET /admin/events with {}", foundEvents);
        return foundEvents;
    }

    @PatchMapping("/admin/events/{eventId}")
    @Validated(OnUpdateValidation.class)
    public EventFullDto updateEvent(@PathVariable(name = "eventId") Long eventId,
                                    @Valid @RequestBody UpdateEventAdminRequestDto updateRequest) {
        log.info("Start PATCH /admin/events/{eventId} with eventId: {}, updateRequest: {}", eventId, updateRequest);
        EventFullDto updatedEvent = eventService.updateEventByAdmin(eventId, updateRequest);
        log.info("Finish PATCH /admin/events/{eventId} with {}", updatedEvent);
        return updatedEvent;
    }


}
