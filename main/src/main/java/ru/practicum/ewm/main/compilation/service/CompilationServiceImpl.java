package ru.practicum.ewm.main.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.compilation.dto.CompilationDto;
import ru.practicum.ewm.main.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.main.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.main.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.main.compilation.model.Compilation;
import ru.practicum.ewm.main.compilation.repository.CompilationRepository;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.mapper.EventMapper;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;

    @Override
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        List<Event> events;
        if (!newCompilationDto.getEvents().isEmpty()) {
            events = eventRepository.findEventsByIds(newCompilationDto.getEvents());
            checkEvents(events);
        } else {
            events = List.of();
        }

        Compilation compilation = Compilation.builder()
                .events(events)
                .pinned(newCompilationDto.isPinned())
                .title(newCompilationDto.getTitle())
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);
        List<EventShortDto> eventShortDtos = savedCompilation.getEvents().stream()
                .map(EventMapper::mapToShortDto)
                .collect(Collectors.toList());

        return CompilationMapper.mapToDto(savedCompilation, eventShortDtos);
    }

    @Override
    public void deleteCompilation(Long compilationId) {
        compilationRepository.deleteById(compilationId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = getCompilationFromDbById(compilationId);

        updateCompilationFields(compilation, updateCompilationRequest);
        compilationRepository.update(compilation);

        List<EventShortDto> eventShortDtos = compilation.getEvents().stream()
                .map(EventMapper::mapToShortDto)
                .collect(Collectors.toList());
        return CompilationMapper.mapToDto(compilation, eventShortDtos);
    }

    private void checkEvents(List<Event> events) {
        if (events.isEmpty()) {
            throw new ForbiddenException(
                    "Fail adding compilation",
                    "No events with passed ids found"
            );
        }
    }

    private Compilation getCompilationFromDbById(Long compilationId) {
        return compilationRepository.findByIdWithoutEvents(compilationId).orElseThrow(
                () -> new NotExistsException(
                        "Compilation",
                        String.format("Compilation with id %d not exists", compilationId)
                ));
    }

    private void updateCompilationFields(Compilation compilation, UpdateCompilationRequest updateRequest) {
        if (updateRequest.getEvents() != null) {
            List<Event> events = eventRepository.findEventsByIds(updateRequest.getEvents());
            checkEvents(events);
            compilationRepository.clearEventsRecordsForCompilation(compilation.getId());
            compilation.setEvents(events);
            compilationRepository.addEventsRecordsForCompilation(compilation);
        }
        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }
    }
}
