package ru.practicum.ewm.main.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.user.model.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    private Long id;
    private String title;
    private String annotation;
    private String description;
    private Category category;
    private LocalDateTime eventDate;
    private User initiator;
    private boolean paid;
    private Location location;
    private int participantLimit;
    private boolean requestModeration;
    private LocalDateTime createdOn;
    private LocalDateTime publishedOn;
    private EventState state;
    private int confirmedRequests;

    public Map<String, Object> mapToDb() {
        Map<String, Object> eventFields = new HashMap<>();

        eventFields.put("event_id", id);
        eventFields.put("title", title);
        eventFields.put("annotation", annotation);
        eventFields.put("description", description);
        eventFields.put("category_id", category.getId());
        eventFields.put("event_date", eventDate);
        eventFields.put("initiator_id", initiator.getId());
        eventFields.put("paid", paid);
        eventFields.put("latitude", location.getLat());
        eventFields.put("longitude", location.getLon());
        eventFields.put("participant_limit", participantLimit);
        eventFields.put("request_moderation", requestModeration);
        eventFields.put("created_on", createdOn);
        eventFields.put("published_on", publishedOn);
        eventFields.put("state", state);

        return eventFields;
    }
}
