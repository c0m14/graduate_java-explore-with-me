package ru.practicum.ewm.main.event.model;

import java.util.Optional;

public enum EventState {
    PENDING, PUBLISHED, CANCELED;

    public static Optional<EventState> from(String stringState) {
        for (EventState eventState : values()) {
            if (eventState.name().equalsIgnoreCase(stringState)) {
                return Optional.of(eventState);
            }
        }
        return Optional.empty();
    }
}
