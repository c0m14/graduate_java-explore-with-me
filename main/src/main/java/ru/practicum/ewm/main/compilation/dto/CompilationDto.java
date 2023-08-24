package ru.practicum.ewm.main.compilation.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.main.event.dto.EventShortDto;

import java.util.List;

@Data
@Builder
public class CompilationDto {

    private Long id;
    private boolean pinned;
    private String title;
    private List<EventShortDto> events;
}
