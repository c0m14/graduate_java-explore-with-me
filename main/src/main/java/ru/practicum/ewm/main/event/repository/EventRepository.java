package ru.practicum.ewm.main.event.repository;

import ru.practicum.ewm.main.event.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository {
    void updateEvent(Event event);

    Optional<Event> getByInitiatorIdAndEventId(Long userId, Long eventId);

    List<Event> getUsersEvents(Long userId, int offset, int size);

    Event save(Event event);
}
