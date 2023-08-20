package ru.practicum.ewm.main.compilation.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.main.event.dto.EventShortDto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
public class CompilationDto {
    @NotNull
    private Long id;

    @NotNull
    private boolean pinned;

    @NotBlank
    private String title;

    private List<EventShortDto> events;
}
