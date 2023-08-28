package ru.practicum.ewm.main.event.dto.searchrequest;

import java.util.Optional;

public enum SearchSortOptionDto {
    VIEWS, EVENT_DATE, RATING;

    public static Optional<SearchSortOptionDto> from(String stringSort) {
        for (SearchSortOptionDto sortOption : values()) {
            if (sortOption.name().equalsIgnoreCase(stringSort)) {
                return Optional.of(sortOption);
            }
        }
        return Optional.empty();
    }
}
