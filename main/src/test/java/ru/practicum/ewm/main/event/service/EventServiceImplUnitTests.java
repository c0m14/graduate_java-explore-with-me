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
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
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
    @Captor
    private ArgumentCaptor<SearchEventParamsDto> searchParamsArgumentCaptor;
    @Captor
    private ArgumentCaptor<EndpointHitDto> endpointHitArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> eventIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<EventState> eventStateArgumentCaptor;

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

    @Test
    void findEvents_whenInvoked_thenSearchTextFormatToLowerCase() {
        String ip = "1.1.1.1";
        String text = "TeXt";
        String expectedText = text.toLowerCase();
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder()
                .text(text)
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEvents(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getText(), equalTo(expectedText));
    }

    @Test
    void findEvents_whenInvoked_thenSearchStateEqualsPublished() {
        String ip = "1.1.1.1";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder().build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEvents(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getState(), equalTo(EventState.PUBLISHED));
    }

    @Test
    void findEvents_whenStartRangeIsNullButEndRangeIsNot_thenEndPassedToRepository() {
        String ip = "1.1.1.1";
        LocalDateTime endRange = LocalDateTime.now().plusDays(1).withNano(0);
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder()
                .rangeEnd(endRange)
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEvents(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getRangeEnd(), equalTo(endRange));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(), equalTo(null));
    }

    @Test
    void findEvents_whenEndRangeIsNullButStartRangeIsNot_thenStarttPassedToRepository() {
        String ip = "1.1.1.1";
        LocalDateTime startRange = LocalDateTime.now().plusDays(1).withNano(0);
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder()
                .rangeStart(startRange)
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEvents(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(), equalTo(startRange));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeEnd(), equalTo(null));
    }

    @Test
    void findEvents_whenEndRangeIsNullAndStartRangeIsNull_thenStartEqualsNowToRepository() {
        String ip = "1.1.1.1";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder()
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEvents(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(),
                equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void findEvents_whenSortOptionIsNull_thenSortByEventDate() {
        String ip = "1.1.1.1";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder()
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEvents(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getSortOption(),
                equalTo(SearchSortOptionDto.EVENT_DATE));
    }

    @Test
    void findEvents_whenEventFound_thenParamsPassedToStatisticClient() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder().build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEvents(searchParams))
                .thenReturn(List.of(foundEvent));

        eventService.findEvents(searchParams, ip);

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
    void findEvents_whenViewsFound_thenAddedToDto() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder().build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri(String.format("/events/%d", eventId))
                .hits(10L)
                .build();
        when(eventRepository.findEvents(searchParams))
                .thenReturn(List.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEvents(searchParams, ip);

        assertThat(foundEvents.get(0).getViews(), equalTo(10L));
    }

    @Test
    void findEvents_whenViewsNotFround_thenZeroViewsAddedToDto() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder().build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri("/events/999")
                .hits(10L)
                .build();
        when(eventRepository.findEvents(searchParams))
                .thenReturn(List.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEvents(searchParams, ip);

        assertThat(foundEvents.get(0).getViews(), equalTo(0L));
    }

    @Test
    void findEvents_whenSortByViewsWithFromAndSize_thenSortedByViewsWithFromAndSizeLimit() {
        Long event1Id = 1L;
        Long event2Id = 2L;
        Long event3Id = 3L;
        Integer from = 1;
        Integer size = 1;
        String ip = "1.1.1.1";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder()
                .sortOption(SearchSortOptionDto.VIEWS)
                .from(from)
                .size(size)
                .build();
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event3 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setId(event1Id);
        event2.setId(event2Id);
        event3.setId(event3Id);
        ViewStatsDto viewStatsDto1 = ViewStatsDto.builder()
                .app("app")
                .uri("/events/1")
                .hits(1L)
                .build();
        ViewStatsDto viewStatsDto2 = ViewStatsDto.builder()
                .app("app")
                .uri("/events/2")
                .hits(10L)
                .build();
        ViewStatsDto viewStatsDto3 = ViewStatsDto.builder()
                .app("app")
                .uri("/events/3")
                .hits(20L)
                .build();
        when(eventRepository.findEvents(searchParams))
                .thenReturn(List.of(event1, event2, event3));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto1, viewStatsDto2, viewStatsDto3));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event1))
                    .thenReturn(TestDataProvider.getValidShortDto(event1.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event2))
                    .thenReturn(TestDataProvider.getValidShortDto(event2.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event3))
                    .thenReturn(TestDataProvider.getValidShortDto(event3.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEvents(searchParams, ip);

        assertThat(foundEvents.size(), equalTo(1));
        assertThat(foundEvents.get(0).getViews(), equalTo(10L));
    }

    @Test
    void findEvents_whenSortByViewsWithFrom_thenSortedByViewsWithFromLimit() {
        Long event1Id = 1L;
        Long event2Id = 2L;
        Long event3Id = 3L;
        Integer from = 1;
        String ip = "1.1.1.1";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder()
                .sortOption(SearchSortOptionDto.VIEWS)
                .from(from)
                .build();
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event3 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setId(event1Id);
        event2.setId(event2Id);
        event3.setId(event3Id);
        ViewStatsDto viewStatsDto1 = ViewStatsDto.builder()
                .app("app")
                .uri("/events/1")
                .hits(1L)
                .build();
        ViewStatsDto viewStatsDto2 = ViewStatsDto.builder()
                .app("app")
                .uri("/events/2")
                .hits(10L)
                .build();
        ViewStatsDto viewStatsDto3 = ViewStatsDto.builder()
                .app("app")
                .uri("/events/3")
                .hits(20L)
                .build();
        when(eventRepository.findEvents(searchParams))
                .thenReturn(List.of(event1, event2, event3));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto1, viewStatsDto2, viewStatsDto3));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event1))
                    .thenReturn(TestDataProvider.getValidShortDto(event1.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event2))
                    .thenReturn(TestDataProvider.getValidShortDto(event2.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event3))
                    .thenReturn(TestDataProvider.getValidShortDto(event3.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEvents(searchParams, ip);

        assertThat(foundEvents.size(), equalTo(2));
        assertThat(foundEvents.get(0).getViews(), equalTo(10L));
        assertThat(foundEvents.get(1).getViews(), equalTo(1L));
    }

    @Test
    void findEvents_whenSortByViewsWithSize_thenSortedByViewsWithSizeLimit() {
        Long event1Id = 1L;
        Long event2Id = 2L;
        Long event3Id = 3L;
        Integer size = 1;
        String ip = "1.1.1.1";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder()
                .sortOption(SearchSortOptionDto.VIEWS)
                .size(size)
                .build();
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event3 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setId(event1Id);
        event2.setId(event2Id);
        event3.setId(event3Id);
        ViewStatsDto viewStatsDto1 = ViewStatsDto.builder()
                .app("app")
                .uri("/events/1")
                .hits(1L)
                .build();
        ViewStatsDto viewStatsDto2 = ViewStatsDto.builder()
                .app("app")
                .uri("/events/2")
                .hits(10L)
                .build();
        ViewStatsDto viewStatsDto3 = ViewStatsDto.builder()
                .app("app")
                .uri("/events/3")
                .hits(20L)
                .build();
        when(eventRepository.findEvents(searchParams))
                .thenReturn(List.of(event1, event2, event3));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto1, viewStatsDto2, viewStatsDto3));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event1))
                    .thenReturn(TestDataProvider.getValidShortDto(event1.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event2))
                    .thenReturn(TestDataProvider.getValidShortDto(event2.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event3))
                    .thenReturn(TestDataProvider.getValidShortDto(event3.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEvents(searchParams, ip);

        assertThat(foundEvents.size(), equalTo(1));
        assertThat(foundEvents.get(0).getViews(), equalTo(20L));
    }

    @Test
    void findEvents_whenInvoked_thenRequestToSaveEndpointHitSent() {
        String ip = "1.1.1.1";
        String uri = "/events";
        SearchEventParamsDto searchParams = SearchEventParamsDto.builder()
                .build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEvents(searchParams))
                .thenReturn(List.of(foundEvent));

        eventService.findEvents(searchParams, ip);

        verify(statisticClient, times(1))
                .saveEndpointHit(endpointHitArgumentCaptor.capture());
        assertThat(endpointHitArgumentCaptor.getValue().getUri(), equalTo(uri));
        assertThat(endpointHitArgumentCaptor.getValue().getIp(), equalTo(ip));
        assertThat(endpointHitArgumentCaptor.getValue().getTimestamp(),
                equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void findEventByIdPublic_whenInvoked_thenIdAndPublishedStatePassedToRepository() {
        String ip = "1.1.1.1";
        Long eventId = 0L;

        try {
            eventService.findEventByIdPublic(eventId, ip);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(eventRepository, times(1))
                .findEventByIdAndState(eventIdArgumentCaptor.capture(),
                        eventStateArgumentCaptor.capture());
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(eventStateArgumentCaptor.getValue(), equalTo(EventState.PUBLISHED));
    }

    @Test
    void findEventByIdPublic_whenEventFound_thenParamsPassedToStatisticClient() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventByIdAndState(eventId, EventState.PUBLISHED))
                .thenReturn(Optional.of(foundEvent));

        eventService.findEventByIdPublic(eventId, ip);

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
    void findEventByIdPublic_whenViewsFound_thenAddedToDto() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri(String.format("/events/%d", eventId))
                .hits(10L)
                .build();
        when(eventRepository.findEventByIdAndState(eventId, EventState.PUBLISHED))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToFullDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidFullDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.findEventByIdPublic(eventId, ip);

        assertThat(foundEventDto.getViews(), equalTo(10L));
    }

    @Test
    void findEventByIdPublic_whenViewsNotFround_thenZeroViewsAddedToDto() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri("/events/999")
                .hits(10L)
                .build();
        when(eventRepository.findEventByIdAndState(eventId, EventState.PUBLISHED))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToFullDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidFullDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.findEventByIdPublic(eventId, ip);

        assertThat(foundEventDto.getViews(), equalTo(0L));
    }

}