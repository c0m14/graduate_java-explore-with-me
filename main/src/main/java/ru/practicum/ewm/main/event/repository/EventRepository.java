package ru.practicum.ewm.main.event.repository;

import ru.practicum.ewm.main.event.dto.searchrequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface EventRepository {
    void updateEvent(Event event);

    Optional<Event> findEventByInitiatorIdAndEventId(Long userId, Long eventId);

    List<Event> findUserEvents(Long userId, int offset, int size);

    Event save(Event event);

    List<Event> findEventsPublic(PublicSearchParamsDto searchParams);

    List<Event> findEventsAdmin(AdminSearchParamsDto searchParams);

    Optional<Event> findEventByIdAndState(Long eventId, EventState state);

    Optional<Event> findEventById(Long eventId);

    Optional<Event> findEventByIdWithoutCategory(Long eventId);

    void lockEventForShare(Long eventId);

    List<Event> findEventsByIds(Set<Long> eventsIds);

    Map<Long, List<Event>> findEventsForCompilations(List<Long> compilationsIds);

    List<Event> findUsersEventsWithoutCategoryAndRequest(List<Long> usersIds);
}
