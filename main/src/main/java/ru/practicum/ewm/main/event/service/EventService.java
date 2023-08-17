package ru.practicum.ewm.main.event.service;

import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.dto.NewEventDto;
import ru.practicum.ewm.main.event.dto.searchRequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchRequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.dto.updateRequest.UpdateEventAdminRequest;
import ru.practicum.ewm.main.event.dto.updateRequest.UpdateEventUserRequest;

import java.util.List;

public interface EventService {
    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> findUsersEvents(Long userId, int from, int size);

    EventFullDto findUserEventById(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    List<EventShortDto> findEvents(PublicSearchParamsDto searchParams, String ip);

    EventFullDto findEventByIdPublic(Long eventId, String ip);

    List<EventFullDto> findEventsAdmin(AdminSearchParamsDto searchParams);
}
