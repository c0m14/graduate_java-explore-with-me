package ru.practicum.ewm.main.event.service;

import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.dto.NewEventDto;
import ru.practicum.ewm.main.event.dto.searchrequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventAdminRequestDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventUserRequestDto;
import ru.practicum.ewm.main.event.model.RateType;

import java.util.List;

public interface EventService {
    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> findUsersEvents(Long userId, int from, int size);

    EventFullDto findUserEventById(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequestDto updateEventUserRequest);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequestDto updateEventAdminRequest);

    List<EventShortDto> findEventsPublic(PublicSearchParamsDto searchParams, String ip);

    EventFullDto findEventByIdPublic(Long eventId, String ip);

    List<EventFullDto> findEventsAdmin(AdminSearchParamsDto searchParams);

    void addRateToEvent(Long userId, Long eventId, RateType rateType);

    void deleteRateFromEvent(Long userId, Long eventId, RateType rateType);
}
