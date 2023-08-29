package ru.practicum.ewm.main.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {

    private Long id;
    private String title;
    private String annotation;
    private CategoryDto category;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
    private UserShortDto initiator;
    private boolean paid;
    private long views;
    private long confirmedRequests;

    private Long rating;
}

