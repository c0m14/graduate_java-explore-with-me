package ru.practicum.ewm.main.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.compilation.dto.CompilationDto;
import ru.practicum.ewm.main.compilation.dto.NewCompilationDto;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;

    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        List<Event> events;
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            events = eventRepository.findEventsByIds(newCompilationDto.getEvents());
            checkEvents(events);
        } else {
            events = List.of();
        }

        Compilation compilation = Compilation.builder()
                .events(events)
                .pinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)
                .title(newCompilationDto.getTitle())
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);
        List<EventShortDto> eventShortDtos = savedCompilation.getEvents().stream()
                .map(EventMapper::mapToShortDto)
                .collect(Collectors.toList());

        return CompilationMapper.mapToDto(savedCompilation, eventShortDtos);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compilationId) {
        compilationRepository.deleteById(compilationId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compilationId, NewCompilationDto updateCompilationRequest) {
        Compilation compilation = getCompilationFromDbById(compilationId);

        updateCompilationFields(compilation, updateCompilationRequest);
        compilationRepository.update(compilation);


        List<EventShortDto> eventShortDtos;
        if (compilation.getEvents() != null) {
            eventShortDtos = compilation.getEvents().stream()
                    .map(EventMapper::mapToShortDto)
                    .collect(Collectors.toList());
        } else {
            eventShortDtos = List.of();
        }

        return CompilationMapper.mapToDto(compilation, eventShortDtos);
    }

    @Override
    public List<CompilationDto> findCompilations(boolean pinned, int from, int size) {
        List<Compilation> compilationsWithoutEvents =
                compilationRepository.findCompilationsWithoutEvents(pinned, from, size);
        if (compilationsWithoutEvents.isEmpty()) {
            return List.of();
        }

        List<Long> compilationsIds = compilationsWithoutEvents.stream()
                .map(Compilation::getId)
                .collect(Collectors.toList());
        Map<Long, List<Event>> compilationsEventsMap = eventRepository.findEventsForCompilations(compilationsIds);

        return compilationsWithoutEvents.stream()
                .map(compilation ->
                        CompilationMapper.mapToDto(
                                compilation,
                                compilationsEventsMap.get(compilation.getId()) != null ?
                                        compilationsEventsMap.get(compilation.getId()).stream()
                                                .map(EventMapper::mapToShortDto)
                                                .collect(Collectors.toList())
                                        :
                                        List.of()
                        ))
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto findById(Long compilationId) {
        Compilation compilation = getCompilationFromDbById(compilationId);
        Map<Long, List<Event>> compilationsEventsMap =
                eventRepository.findEventsForCompilations(List.of(compilationId));

        if (compilationsEventsMap.get(compilationId) == null) {
            return CompilationMapper.mapToDto(compilation, List.of());
        } else {
            return CompilationMapper.mapToDto(
                    compilation,
                    compilationsEventsMap.get(compilationId).stream()
                            .map(EventMapper::mapToShortDto)
                            .collect(Collectors.toList())
            );
        }
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

    private void updateCompilationFields(Compilation compilation, NewCompilationDto updateRequest) {
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
