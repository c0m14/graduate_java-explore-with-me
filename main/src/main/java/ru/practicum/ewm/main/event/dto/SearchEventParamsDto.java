package ru.practicum.ewm.main.event.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.main.event.model.EventState;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class SearchEventParamsDto {

    String text;
    Set<Integer> categoriesIds;
    Boolean paid;
    LocalDateTime rangeStart;
    LocalDateTime rangeEnd;
    Boolean onlyAvailable;
    SearchSortOptionDto sortOption;
    Integer from;
    Integer size;
    EventState state;
}
