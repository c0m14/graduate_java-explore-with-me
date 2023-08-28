package ru.practicum.ewm.main.event.dto.searchrequest;

import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.main.event.model.EventState;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class AdminSearchParamsDto {
    Set<Long> usersIds;
    Set<EventState> states;
    Set<Integer> categoriesIds;
    LocalDateTime rangeStart;
    LocalDateTime rangeEnd;
    Integer from;
    Integer size;
}
