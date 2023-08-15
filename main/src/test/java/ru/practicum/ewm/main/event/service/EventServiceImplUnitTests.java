package ru.practicum.ewm.main.event.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;
import ru.practicum.ewm.main.event.dto.*;
import ru.practicum.ewm.main.event.mapper.EventMapper;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.InvalidParamException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;
import ru.practicum.ewm.statistic.client.StatisticClient;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplUnitTests {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private StatisticClient statisticClient;
    @InjectMocks
    private EventServiceImpl eventService;
    @Captor
    private ArgumentCaptor<Event> eventArgumentCaptor;
    @Captor
    private ArgumentCaptor<LocalDateTime> startArgumentCaptor;
    @Captor
    private ArgumentCaptor<LocalDateTime> endArgumentCaptor;
    @Captor
    private ArgumentCaptor<List<String>> urisArgumentCaptor;
    @Captor
    private ArgumentCaptor<Boolean> uniqueArgumentCaptor;

    @Test
    void addEvent_whenEventDateEarlierThan2HoursFromNow_thenInvalidParamExceptionThrown() {
        Long userId = 0L;
        NewEventDto newEventDto = TestDataProvider.getValidNewEventDto();
        newEventDto.setEventDate(LocalDateTime.now().plusMinutes(1));

        Executable executable = () -> eventService.addEvent(userId, newEventDto);

        verify(eventRepository, times(0))
                .save(any());
        assertThrows(InvalidParamException.class,
                executable);
    }

    @Test
    void addEvent_whenUserNotFound_thenInvalidParamExceptionThrown() {
        Long userId = 0L;
        NewEventDto newEventDto = TestDataProvider.getValidNewEventDto();
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.addEvent(userId, newEventDto);

        verify(eventRepository, times(0))
                .save(any());
        assertThrows(InvalidParamException.class,
                executable);
    }

    @Test
    void addEvent_whenCategoryNotFound_thenInvalidParamExceptionThrown() {
        Long userId = 0L;
        NewEventDto newEventDto = TestDataProvider.getValidNewEventDto();
        int categoryId = newEventDto.getCategory();
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(categoryRepository.getCategoryById(categoryId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.addEvent(userId, newEventDto);

        verify(eventRepository, times(0))
                .save(any());
        assertThrows(InvalidParamException.class,
                executable);
    }

    @Test
    void addEvent_whenInvoked_thenEventSavedWithStatusPending() {
        Long userId = 0L;
        NewEventDto newEventDto = TestDataProvider.getValidNewEventDto();
        int categoryId = newEventDto.getCategory();
        User user = new User();
        Category category = new Category();
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(user));
        when(categoryRepository.getCategoryById(categoryId))
                .thenReturn(Optional.of(category));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToEntity(newEventDto, user, category))
                    .thenReturn(TestDataProvider.getValidNotSavedEvent(user, category));
        }

        try {
            eventService.addEvent(userId, newEventDto);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(eventRepository, times(1))
                .save(eventArgumentCaptor.capture());
        assertThat(eventArgumentCaptor.getValue().getState(), equalTo(EventState.PENDING));
    }

    @Test
    void addEvent_whenInvoked_thenEventSavedWithCreatedOnField() {
        Long userId = 0L;
        NewEventDto newEventDto = TestDataProvider.getValidNewEventDto();
        int categoryId = newEventDto.getCategory();
        User user = new User();
        Category category = new Category();
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(user));
        when(categoryRepository.getCategoryById(categoryId))
                .thenReturn(Optional.of(category));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToEntity(newEventDto, user, category))
                    .thenReturn(TestDataProvider.getValidNotSavedEvent(user, category));
        }

        try {
            eventService.addEvent(userId, newEventDto);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(eventRepository, times(1))
                .save(eventArgumentCaptor.capture());
        assertNotNull(eventArgumentCaptor.getValue().getCreatedOn());
    }

    @Test
    void findUsersEvents_whenUserNoExists_thenInvalidParamExceptionThrown() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.findUsersEvents(userId, from, size);

        assertThrows(InvalidParamException.class, executable);
    }

    @Test
    void findUsersEvents_whenEventsNotFound_thenEmptyListReturned() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getUsersEvents(userId, from, size))
                .thenReturn(List.of());

        List<EventShortDto> froundEvents = eventService.findUsersEvents(userId, from, size);

        assertThat(froundEvents, empty());
    }

    @Test
    void findUsersEvents_whenEventsFound_thenStatisticCalledWithStartEqualsEarliestOdCreationEventDate() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        LocalDateTime event1CreatedOn = LocalDateTime.now().minusDays(1L).withNano(0);
        LocalDateTime event2CreatedOn = LocalDateTime.now().minusDays(2L).withNano(0);
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setCreatedOn(event1CreatedOn);
        event2.setCreatedOn(event2CreatedOn);
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getUsersEvents(userId, from, size))
                .thenReturn(List.of(event1, event2));

        try {
            eventService.findUsersEvents(userId, from, size);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(statisticClient, times(1))
                .getViewStats(startArgumentCaptor.capture(), any(), anyList(), anyBoolean());

        assertThat(startArgumentCaptor.getValue(), equalTo(event2CreatedOn));
    }

    @Test
    void findUsersEvents_whenEventsFound_thenStatisticCalledWithEndEqualsNow() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        LocalDateTime event1CreatedOn = LocalDateTime.now().minusDays(1L).withNano(0);
        LocalDateTime event2CreatedOn = LocalDateTime.now().minusDays(2L).withNano(0);
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setCreatedOn(event1CreatedOn);
        event2.setCreatedOn(event2CreatedOn);
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getUsersEvents(userId, from, size))
                .thenReturn(List.of(event1, event2));

        try {
            eventService.findUsersEvents(userId, from, size);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(statisticClient, times(1))
                .getViewStats(any(), endArgumentCaptor.capture(), anyList(), anyBoolean());

        assertThat(endArgumentCaptor.getValue().truncatedTo(ChronoUnit.SECONDS),
                equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void findUsersEvents_whenEventsFound_thenStatisticCalledWithUrisContainFoundEventsIds() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        Long event1Id = 1L;
        Long event2Id = 2L;
        LocalDateTime event1CreatedOn = LocalDateTime.now().minusDays(1L).withNano(0);
        LocalDateTime event2CreatedOn = LocalDateTime.now().minusDays(2L).withNano(0);
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setCreatedOn(event1CreatedOn);
        event1.setId(event1Id);
        event2.setCreatedOn(event2CreatedOn);
        event2.setId(event2Id);
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getUsersEvents(userId, from, size))
                .thenReturn(List.of(event1, event2));

        try {
            eventService.findUsersEvents(userId, from, size);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(statisticClient, times(1))
                .getViewStats(any(), any(), urisArgumentCaptor.capture(), anyBoolean());

        assertTrue(urisArgumentCaptor.getValue().contains(String.format("/events/%d", event1.getId())));
        assertTrue(urisArgumentCaptor.getValue().contains(String.format("/events/%d", event2.getId())));
    }

    @Test
    void findUsersEvents_whenEventsFound_thenStatisticCalledWithUniqueTrue() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        LocalDateTime event1CreatedOn = LocalDateTime.now().minusDays(1L).withNano(0);
        LocalDateTime event2CreatedOn = LocalDateTime.now().minusDays(2L).withNano(0);
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setCreatedOn(event1CreatedOn);
        event2.setCreatedOn(event2CreatedOn);
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getUsersEvents(userId, from, size))
                .thenReturn(List.of(event1, event2));

        try {
            eventService.findUsersEvents(userId, from, size);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(statisticClient, times(1))
                .getViewStats(any(), any(), anyList(), uniqueArgumentCaptor.capture());

        assertTrue(uniqueArgumentCaptor.getValue());
    }

    @Test
    void findUsersEvents_whenStatFound_thenViewsAddedToEvents() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        Long event1Id = 1L;
        Long event2Id = 2L;
        LocalDateTime event1CreatedOn = LocalDateTime.now().minusDays(1L).withNano(0);
        LocalDateTime event2CreatedOn = LocalDateTime.now().minusDays(2L).withNano(0);
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setCreatedOn(event1CreatedOn);
        event1.setId(event1Id);
        event2.setCreatedOn(event2CreatedOn);
        event2.setId(event2Id);
        ViewStatsDto event1Stat = ViewStatsDto.builder()
                .app("app")
                .uri("/events/1")
                .hits(5L)
                .build();
        ViewStatsDto event2Stat = ViewStatsDto.builder()
                .app("app")
                .uri("/events/2")
                .hits(10L)
                .build();
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getUsersEvents(userId, from, size))
                .thenReturn(List.of(event1, event2));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(event2Stat, event1Stat));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event1))
                    .thenReturn(TestDataProvider.getValidShortDto(event1Id));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event2))
                    .thenReturn(TestDataProvider.getValidShortDto(event2Id));
        }

        List<EventShortDto> foundEvents = eventService.findUsersEvents(userId, from, size);

        assertThat(foundEvents.get(0).getViews(), equalTo(5L));
        assertThat(foundEvents.get(1).getViews(), equalTo(10L));
    }

    @Test
    void findUsersEvents_whenStatNotFound_thenZeroViewsAddedToEvents() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        Long event1Id = 1L;
        Long event2Id = 2L;
        LocalDateTime event1CreatedOn = LocalDateTime.now().minusDays(1L).withNano(0);
        LocalDateTime event2CreatedOn = LocalDateTime.now().minusDays(2L).withNano(0);
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setCreatedOn(event1CreatedOn);
        event1.setId(event1Id);
        event2.setCreatedOn(event2CreatedOn);
        event2.setId(event2Id);
        ViewStatsDto event2Stat = ViewStatsDto.builder()
                .app("app")
                .uri("/events/2")
                .hits(10L)
                .build();
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getUsersEvents(userId, from, size))
                .thenReturn(List.of(event1, event2));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(event2Stat));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event1))
                    .thenReturn(TestDataProvider.getValidShortDto(event1Id));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event2))
                    .thenReturn(TestDataProvider.getValidShortDto(event2Id));
        }

        List<EventShortDto> foundEvents = eventService.findUsersEvents(userId, from, size);

        assertThat(foundEvents.get(0).getViews(), equalTo(0L));
        assertThat(foundEvents.get(1).getViews(), equalTo(10L));
    }

    @Test
    void findUserEventById_whenUserNotExist_thenInvalidParamExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.findUserEventById(userId, eventId);

        assertThrows(InvalidParamException.class, executable);
    }

    @Test
    void findUserEventById_whenEventNotFound_thenNotExistsExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.findUserEventById(userId, eventId);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void findUserEventById_whenEventFound_thenParamsPassedToStatisticClient() {
        Long userId = 0L;
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(foundEvent));

        try {
            eventService.findUserEventById(userId, eventId);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(statisticClient, times(1))
                .getViewStats(
                        startArgumentCaptor.capture(),
                        endArgumentCaptor.capture(),
                        urisArgumentCaptor.capture(),
                        uniqueArgumentCaptor.capture()
                );
        assertThat(startArgumentCaptor.getValue(), equalTo(foundEvent.getCreatedOn()));
        assertThat(endArgumentCaptor.getValue(), equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
        assertThat(urisArgumentCaptor.getValue().get(0), equalTo(String.format("/events/%d", eventId)));
        assertThat(uniqueArgumentCaptor.getValue(), equalTo(true));
    }

    @Test
    void findUserEventById_whenViewsFround_thenAddedToDto() {
        Long userId = 0L;
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri(String.format("/events/%d", eventId))
                .hits(10L)
                .build();
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.findUserEventById(userId, eventId);

        assertThat(foundEventDto.getViews(), equalTo(10L));
    }

    @Test
    void findUserEventById_whenViewsNotFround_thenZeroViewsAddedToDto() {
        Long userId = 0L;
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri("/events/999")
                .hits(10L)
                .build();
        when(userRepository.getUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.findUserEventById(userId, eventId);

        assertThat(foundEventDto.getViews(), equalTo(0L));
    }

    @Test
    void updateEvent_whenEventNotFound_thenNotExistsExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.updateEvent(userId, eventId, updateRequest);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void updateEvent_whenEventStatusIsPublished_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.PUBLISHED);
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        Executable executable = () -> eventService.updateEvent(userId, eventId, updateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateEvent_whenUpdateTitleNotNull_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setTitle("a".repeat(5));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getTitle(), equalTo(updateRequest.getTitle()));
    }

    @Test
    void updateEvent_whenUpdateDescriptionNotNull_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setDescription("a".repeat(21));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getDescription(), equalTo(updateRequest.getDescription()));
    }

    @Test
    void updateEvent_whenUpdateCategoryNotNullAndExist_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        int categoryId = 1;
        Category category = TestDataProvider.getValidCategoryToSave();
        category.setId(categoryId);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setCategory(categoryId);
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));
        when(categoryRepository.getCategoryById(categoryId))
                .thenReturn(Optional.of(category));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getCategory(), equalTo(category));
    }

    @Test
    void updateEvent_whenUpdateCategoryNotNullAndNotExist_thenInvalidParamExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        int categoryId = 1;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setCategory(categoryId);
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));
        when(categoryRepository.getCategoryById(categoryId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.updateEvent(userId, eventId, updateRequest);

        assertThrows(InvalidParamException.class, executable);
    }

    @Test
    void updateEvent_whenUpdateEventDate_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setEventDate(LocalDateTime.now().plusDays(1).withNano(0));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getEventDate(), equalTo(updateRequest.getEventDate()));
    }

    @Test
    void updateEvent_whenUpdateEventDateAndNotValid_thenInvalidParamExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setEventDate(LocalDateTime.now().plusMinutes(1).withNano(0));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        Executable executable = () -> eventService.updateEvent(userId, eventId, updateRequest);

        assertThrows(InvalidParamException.class, executable);
    }

    @Test
    void updateEvent_whenUpdatePaid_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setPaid(false);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setPaid(true);
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().isPaid(), equalTo(updateRequest.getPaid()));
    }

    @Test
    void updateEvent_whenUpdateLocation_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setLocation(new Location(333.222f, 777.333f));
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getLocation(), equalTo(updateRequest.getLocation()));
    }

    @Test
    void updateEvent_whenUpdateRequestModeration_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setRequestModeration(false);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setRequestModeration(true);
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().isRequestModeration(), equalTo(updateRequest.getRequestModeration()));
    }

    @Test
    void updateEvent_whenUpdateParticipantLimit_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setParticipantLimit(0);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setParticipantLimit(100);
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getParticipantLimit(), equalTo(updateRequest.getParticipantLimit()));
    }

    @Test
    void updateEvent_whenUpdateStateAndActionIsCancelReview_thenCanceledStatePassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.PENDING);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setStateAction(StateAction.CANCEL_REVIEW);
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getState(), equalTo(EventState.CANCELED));
    }

    @Test
    void updateEvent_whenUpdateStateAndActionIsSendToReview_thenPendingStatePassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.CANCELED);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setStateAction(StateAction.SEND_TO_REVIEW);
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getState(), equalTo(EventState.PENDING));
    }

    @Test
    void updateEvent_whenEventFound_thenParamsPassedToStatisticClient() {
        Long userId = 0L;
        Long eventId = 1L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(foundEvent));

        eventService.updateEvent(userId, eventId, updateRequest);

        verify(statisticClient, times(1))
                .getViewStats(
                        startArgumentCaptor.capture(),
                        endArgumentCaptor.capture(),
                        urisArgumentCaptor.capture(),
                        uniqueArgumentCaptor.capture()
                );
        assertThat(startArgumentCaptor.getValue(), equalTo(foundEvent.getCreatedOn()));
        assertThat(endArgumentCaptor.getValue(), equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
        assertThat(urisArgumentCaptor.getValue().get(0), equalTo(String.format("/events/%d", eventId)));
        assertThat(uniqueArgumentCaptor.getValue(), equalTo(true));
    }

    @Test
    void updateEvent_whenViewsFound_thenAddedToDto() {
        Long userId = 0L;
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri(String.format("/events/%d", eventId))
                .hits(10L)
                .build();
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.updateEvent(userId, eventId, updateRequest);

        assertThat(foundEventDto.getViews(), equalTo(10L));
    }

    @Test
    void updateEvent_whenViewsNotFround_thenZeroViewsAddedToDto() {
        Long userId = 0L;
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri("/events/999")
                .hits(10L)
                .build();
        when(eventRepository.getByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.updateEvent(userId, eventId, updateRequest);

        assertThat(foundEventDto.getViews(), equalTo(0L));
    }

}