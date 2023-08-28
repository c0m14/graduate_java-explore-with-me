package ru.practicum.ewm.main.event.repository;

import java.util.List;
import java.util.Map;

public interface RateRepository {
    void addRate(Long userId, Long eventId, int rate);

    Long getRatingForEvent(Long eventId);

    Map<Long, Long> getRatingsForEvents(List<Long> eventsIds);

    void deleteRate(Long userId, Long eventId, int rate);

}
