package ru.practicum.ewm.main.request.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.user.model.User;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventParticipationRequest {
    private Long id;
    private Instant created = Instant.now();
    private Event event;
    private User requester;
    private RequestStatus requestStatus;
}
