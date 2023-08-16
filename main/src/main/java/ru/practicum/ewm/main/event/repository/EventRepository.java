package ru.practicum.ewm.main.event.repository;

import ru.practicum.ewm.main.event.dto.SearchEventParamsDto;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;

import java.util.List;
import java.util.Optional;

public interface EventRepository {
    void updateEvent(Event event);

    Optional<Event> getByInitiatorIdAndEventId(Long userId, Long eventId);

    List<Event> getUsersEvents(Long userId, int offset, int size);

    Event save(Event event);

    List<Event> findEvents(SearchEventParamsDto searchParams);

    Optional<Event> findEventByIdAndState(Long eventId, EventState state);
}
