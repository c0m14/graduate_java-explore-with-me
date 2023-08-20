package ru.practicum.ewm.main.compilation.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.compilation.dto.CompilationDto;
import ru.practicum.ewm.main.compilation.model.Compilation;
import ru.practicum.ewm.main.event.dto.EventShortDto;

import java.util.List;

@UtilityClass
public class CompilationMapper {

    public CompilationDto mapToDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(events)
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .build();
    }
}
