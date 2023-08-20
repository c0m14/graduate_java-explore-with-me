package ru.practicum.ewm.main.request.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.user.model.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventParticipationRequest {
    private Long id;
    private LocalDateTime created;
    private Event event;
    private User requester;
    private RequestStatus requestStatus;

    public Map<String, Object> mapToDb() {
        Map<String, Object> requestFields = new HashMap<>();

        requestFields.put("request_id", id);
        requestFields.put("created", created);
        requestFields.put("event_id", event.getId());
        requestFields.put("requester_id", requester.getId());
        requestFields.put("request_status", requestStatus);

        return requestFields;
    }
}
