package ru.practicum.ewm.main.event.service;

import ru.practicum.ewm.main.event.dto.*;

import java.util.List;

public interface EventService {
    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> findUsersEvents(Long userId, int from, int size);

    EventFullDto findUserEventById(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<EventShortDto> findEvents(SearchEventParamsDto searchParams, String ip);

    EventFullDto findEventByIdPublic(Long eventId, String ip);
}
