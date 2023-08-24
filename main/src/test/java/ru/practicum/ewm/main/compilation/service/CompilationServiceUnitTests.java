package ru.practicum.ewm.main.compilation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.compilation.dto.CompilationDto;
import ru.practicum.ewm.main.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.main.compilation.model.Compilation;
import ru.practicum.ewm.main.compilation.repository.CompilationRepository;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.user.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceUnitTests {

    @Mock
    private CompilationRepository compilationRepository;
    @Mock
    private EventRepository eventRepository;
    @InjectMocks
    private CompilationServiceImpl compilationService;

    @Captor
    private ArgumentCaptor<Compilation> compilationArgumentCaptor;

    @Test
    void addCompilation_whenEventsNotFoundByIds_thenForbiddenExceptionThrown() {
        NewCompilationDto newCompilationDto = TestDataProvider.getValidNewCompilationDto();
        when(eventRepository.findEventsByIds(newCompilationDto.getEvents()))
                .thenReturn(List.of());

        Executable executable = () -> compilationService.addCompilation(newCompilationDto);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateCompilation_whenCompilationNotFound_thenNotExistsExceptionThrown() {
        Long compilationId = 0L;
        NewCompilationDto updateRequest = NewCompilationDto.builder().build();
        when(compilationRepository.findByIdWithoutEvents(compilationId))
                .thenReturn(Optional.empty());

        Executable executable = () -> compilationService.updateCompilation(compilationId, updateRequest);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void updateCompilation_whenNewEventsNotFound_thenForbiddenExceptionThrown() {
        Long compilationId = 0L;
        Long newEventId = 1L;
        NewCompilationDto updateRequest = NewCompilationDto.builder()
                .events(Set.of(newEventId))
                .build();
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of());
        when(compilationRepository.findByIdWithoutEvents(compilationId))
                .thenReturn(Optional.of(compilation));
        when(eventRepository.findEventsByIds(updateRequest.getEvents()))
                .thenReturn(List.of());

        Executable executable = () -> compilationService.updateCompilation(compilationId, updateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateCompilation_whenEventsUpdate_thenOldEventsRecordsClearedByCompilationId() {
        Long compilationId = 0L;
        Long newEventId = 1L;
        NewCompilationDto updateRequest = NewCompilationDto.builder()
                .events(Set.of(newEventId))
                .build();
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of());
        compilation.setId(compilationId);
        Event newEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(compilationRepository.findByIdWithoutEvents(compilationId))
                .thenReturn(Optional.of(compilation));
        when(eventRepository.findEventsByIds(updateRequest.getEvents()))
                .thenReturn(List.of(newEvent));

        try {
            compilationService.updateCompilation(compilationId, updateRequest);
        } catch (Throwable e) {
            //catch params before execution complete
        }

        verify(compilationRepository, times(1))
                .clearEventsRecordsForCompilation(compilationId);
    }

    @Test
    void updateCompilation_whenEventsUpdate_thenNewEventsRecordsAddedByCompilationId() {
        Long compilationId = 0L;
        Long newEventId = 1L;
        NewCompilationDto updateRequest = NewCompilationDto.builder()
                .events(Set.of(newEventId))
                .build();
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of());
        compilation.setId(compilationId);
        Event newEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(compilationRepository.findByIdWithoutEvents(compilationId))
                .thenReturn(Optional.of(compilation));
        when(eventRepository.findEventsByIds(updateRequest.getEvents()))
                .thenReturn(List.of(newEvent));

        try {
            compilationService.updateCompilation(compilationId, updateRequest);
        } catch (Throwable e) {
            //catch params before execution complete
        }

        verify(compilationRepository, times(1))
                .addEventsRecordsForCompilation(compilation);
    }

    @Test
    void updateCompilation_whenEventsUpdate_thenCompilationWithNewEventsPassedToRepository() {
        Long compilationId = 0L;
        Long newEventId = 1L;
        NewCompilationDto updateRequest = NewCompilationDto.builder()
                .events(Set.of(newEventId))
                .build();
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of());
        compilation.setId(compilationId);
        Event newEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(compilationRepository.findByIdWithoutEvents(compilationId))
                .thenReturn(Optional.of(compilation));
        when(eventRepository.findEventsByIds(updateRequest.getEvents()))
                .thenReturn(List.of(newEvent));

        try {
            compilationService.updateCompilation(compilationId, updateRequest);
        } catch (Throwable e) {
            //catch params before execution complete
        }

        verify(compilationRepository, times(1))
                .update(compilationArgumentCaptor.capture());
        assertThat(compilationArgumentCaptor.getValue().getEvents().get(0), equalTo(newEvent));
    }

    @Test
    void updateCompilation_whenTitleUpdate_thenCompilationWithNewTitlePassedToRepository() {
        Long compilationId = 0L;
        String newTitle = "new title";
        NewCompilationDto updateRequest = NewCompilationDto.builder()
                .title(newTitle)
                .build();
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of());
        compilation.setId(compilationId);
        when(compilationRepository.findByIdWithoutEvents(compilationId))
                .thenReturn(Optional.of(compilation));

        try {
            compilationService.updateCompilation(compilationId, updateRequest);
        } catch (Throwable e) {
            //catch params before execution complete
        }

        verify(compilationRepository, times(1))
                .update(compilationArgumentCaptor.capture());
        assertThat(compilationArgumentCaptor.getValue().getTitle(), equalTo(newTitle));
    }

    @Test
    void updateCompilation_whenPinnedUpdate_thenCompilationWithNewPinnedPassedToRepository() {
        Long compilationId = 0L;
        boolean newPinned = true;
        NewCompilationDto updateRequest = NewCompilationDto.builder()
                .pinned(newPinned)
                .build();
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of());
        compilation.setPinned(false);
        compilation.setId(compilationId);
        when(compilationRepository.findByIdWithoutEvents(compilationId))
                .thenReturn(Optional.of(compilation));

        try {
            compilationService.updateCompilation(compilationId, updateRequest);
        } catch (Throwable e) {
            //catch params before execution complete
        }

        verify(compilationRepository, times(1))
                .update(compilationArgumentCaptor.capture());
        assertThat(compilationArgumentCaptor.getValue().isPinned(), equalTo(newPinned));
    }

    @Test
    void findCompilations_whenCompilationsNotFound_thenEmptyListReturned() {
        boolean pinned = false;
        int from = 0;
        int size = 10;
        when(compilationRepository.findCompilationsWithoutEvents(pinned, from, size))
                .thenReturn(List.of());

        List<CompilationDto> foundCompilations = compilationService.findCompilations(pinned, from, size);

        verify(eventRepository, times(0))
                .findEventsForCompilations(anyList());
        assertTrue(foundCompilations.isEmpty());
    }

    @Test
    void findCompilations_whenCompilationsFound_thenEventsRequestedFromRepository() {
        boolean pinned = false;
        int from = 0;
        int size = 10;
        Long compilationId = 0L;
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of());
        compilation.setId(compilationId);
        when(compilationRepository.findCompilationsWithoutEvents(pinned, from, size))
                .thenReturn(List.of(compilation));

        try {
            compilationService.findCompilations(pinned, from, size);
        } catch (Throwable e) {
            //catch params before execution complete
        }

        verify(eventRepository, times(1))
                .findEventsForCompilations(List.of(compilationId));
    }

    @Test
    void findById_whenCompilationNotFound_thenNotExistsExceptionThrown() {
        Long compilationId = 0L;
        when(compilationRepository.findByIdWithoutEvents(compilationId))
                .thenReturn(Optional.empty());

        Executable executable = () -> compilationService.findById(compilationId);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void findById_whenCompilationFound_thenEventsRequestedFromRepository() {
        Long compilationId = 0L;
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of());
        compilation.setId(compilationId);
        when(compilationRepository.findByIdWithoutEvents(compilationId))
                .thenReturn(Optional.of(compilation));

        try {
            compilationService.findById(compilationId);
        } catch (Throwable e) {
            //catch params before execution complete
        }

        verify(eventRepository, times(1))
                .findEventsForCompilations(List.of(compilationId));
    }
}