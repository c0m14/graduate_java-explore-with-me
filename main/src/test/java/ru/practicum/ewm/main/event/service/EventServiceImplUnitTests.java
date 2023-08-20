package ru.practicum.ewm.main.event.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;
import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.dto.NewEventDto;
import ru.practicum.ewm.main.event.dto.searchRequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchRequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchRequest.SearchSortOptionDto;
import ru.practicum.ewm.main.event.dto.updateRequest.AdminRequestStateAction;
import ru.practicum.ewm.main.event.dto.updateRequest.UpdateEventAdminRequest;
import ru.practicum.ewm.main.event.dto.updateRequest.UpdateEventUserRequest;
import ru.practicum.ewm.main.event.dto.updateRequest.UserRequestStateAction;
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
    private ArgumentCaptor<PublicSearchParamsDto> searchParamsArgumentCaptor;
    @Captor
    private ArgumentCaptor<EndpointHitDto> endpointHitArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> eventIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<EventState> eventStateArgumentCaptor;

    @Test
    void addEvent_whenEventDateEarlierThan2HoursFromNow_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        NewEventDto newEventDto = TestDataProvider.getValidNewEventDto();
        newEventDto.setEventDate(LocalDateTime.now().plusMinutes(1));

        Executable executable = () -> eventService.addEvent(userId, newEventDto);

        verify(eventRepository, times(0))
                .save(any());
        assertThrows(ForbiddenException.class,
                executable);
    }

    @Test
    void addEvent_whenUserNotFound_thenInvalidParamExceptionThrown() {
        Long userId = 0L;
        NewEventDto newEventDto = TestDataProvider.getValidNewEventDto();
        when(userRepository.findUserById(userId))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(categoryRepository.findCategoryById(categoryId))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(user));
        when(categoryRepository.findCategoryById(categoryId))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(user));
        when(categoryRepository.findCategoryById(categoryId))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.findUsersEvents(userId, from, size);

        assertThrows(InvalidParamException.class, executable);
    }

    @Test
    void findUsersEvents_whenEventsNotFound_thenEmptyListReturned() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findUsersEvents(userId, from, size))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findUsersEvents(userId, from, size))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findUsersEvents(userId, from, size))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findUsersEvents(userId, from, size))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findUsersEvents(userId, from, size))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findUsersEvents(userId, from, size))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findUsersEvents(userId, from, size))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.findUserEventById(userId, eventId);

        assertThrows(InvalidParamException.class, executable);
    }

    @Test
    void findUserEventById_whenEventNotFound_thenNotExistsExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
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
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(new User()));
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
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
    void updateEventByUser_whenEventNotFound_thenNotExistsExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.updateEventByUser(userId, eventId, updateRequest);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void updateEventByUser_whenEventStatusIsPublished_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.PUBLISHED);
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        Executable executable = () -> eventService.updateEventByUser(userId, eventId, updateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateEventByUser_whenUpdateTitleNotNull_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setTitle("a".repeat(5));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getTitle(), equalTo(updateRequest.getTitle()));
    }

    @Test
    void updateEventByUser_whenUpdateDescriptionNotNull_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setDescription("a".repeat(21));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getDescription(), equalTo(updateRequest.getDescription()));
    }

    @Test
    void updateEventByUser_whenUpdateCategoryNotNullAndExist_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        int categoryId = 1;
        Category category = TestDataProvider.getValidCategoryToSave();
        category.setId(categoryId);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setCategory(categoryId);
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));
        when(categoryRepository.findCategoryById(categoryId))
                .thenReturn(Optional.of(category));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getCategory(), equalTo(category));
    }

    @Test
    void updateEventByUser_whenUpdateCategoryNotNullAndNotExist_thenInvalidParamExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        int categoryId = 1;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setCategory(categoryId);
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));
        when(categoryRepository.findCategoryById(categoryId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.updateEventByUser(userId, eventId, updateRequest);

        assertThrows(InvalidParamException.class, executable);
    }

    @Test
    void updateEventByUser_whenUpdateEventDate_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setEventDate(LocalDateTime.now().plusDays(1).withNano(0));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getEventDate(), equalTo(updateRequest.getEventDate()));
    }

    @Test
    void updateEventByUser_whenUpdateEventDateAndNotValid_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setEventDate(LocalDateTime.now().plusMinutes(1).withNano(0));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        Executable executable = () -> eventService.updateEventByUser(userId, eventId, updateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateEventByUser_whenUpdatePaid_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setPaid(false);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setPaid(true);
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().isPaid(), equalTo(updateRequest.getPaid()));
    }

    @Test
    void updateEventByUser_whenUpdateLocation_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setLocation(new Location(333.222f, 777.333f));
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getLocation(), equalTo(updateRequest.getLocation()));
    }

    @Test
    void updateEventByUser_whenUpdateRequestModeration_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setRequestModeration(false);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setRequestModeration(true);
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().isRequestModeration(), equalTo(updateRequest.getRequestModeration()));
    }

    @Test
    void updateEventByUser_whenUpdateParticipantLimit_thenUpdatedFieldPassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setParticipantLimit(0);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setParticipantLimit(100);
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getParticipantLimit(), equalTo(updateRequest.getParticipantLimit()));
    }

    @Test
    void updateEventByUser_whenUpdateStateAndActionIsCancelReview_thenCanceledStatePassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.PENDING);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setStateAction(UserRequestStateAction.CANCEL_REVIEW);
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getState(), equalTo(EventState.CANCELED));
    }

    @Test
    void updateEventByUser_whenUpdateStateAndActionIsSendToReview_thenPendingStatePassedToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.CANCELED);
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setStateAction(UserRequestStateAction.SEND_TO_REVIEW);
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getState(), equalTo(EventState.PENDING));
    }

    @Test
    void updateEventByUser_whenEventFound_thenParamsPassedToStatisticClient() {
        Long userId = 0L;
        Long eventId = 1L;
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(foundEvent));

        eventService.updateEventByUser(userId, eventId, updateRequest);

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
    void updateEventByUser_whenViewsFound_thenAddedToDto() {
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
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.updateEventByUser(userId, eventId, updateRequest);

        assertThat(foundEventDto.getViews(), equalTo(10L));
    }

    @Test
    void updateEventByUser_whenViewsNotFround_thenZeroViewsAddedToDto() {
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
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.updateEventByUser(userId, eventId, updateRequest);

        assertThat(foundEventDto.getViews(), equalTo(0L));
    }

    @Test
    void findEvents_whenInvoked_thenSearchTextFormatToLowerCase() {
        String ip = "1.1.1.1";
        String text = "TeXt";
        String expectedText = text.toLowerCase();
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .text(text)
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getText(), equalTo(expectedText));
    }

    @Test
    void findEvents_whenInvoked_thenSearchStateEqualsPublished() {
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder().build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getState(), equalTo(EventState.PUBLISHED));
    }

    @Test
    void findEvents_whenStartRangeIsNullButEndRangeIsNot_thenEndPassedToRepository() {
        String ip = "1.1.1.1";
        LocalDateTime endRange = LocalDateTime.now().plusDays(1).withNano(0);
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .rangeEnd(endRange)
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getRangeEnd(), equalTo(endRange));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(), equalTo(null));
    }

    @Test
    void findEvents_whenEndRangeIsNullButStartRangeIsNot_thenStarttPassedToRepository() {
        String ip = "1.1.1.1";
        LocalDateTime startRange = LocalDateTime.now().plusDays(1).withNano(0);
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .rangeStart(startRange)
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(), equalTo(startRange));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeEnd(), equalTo(null));
    }

    @Test
    void findEvents_whenEndRangeIsNullAndStartRangeIsNull_thenStartEqualsNowToRepository() {
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(),
                equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void findEvents_whenSortOptionIsNull_thenSortByEventDate() {
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .build();

        eventService.findEvents(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getSortOption(),
                equalTo(SearchSortOptionDto.EVENT_DATE));
    }

    @Test
    void findEvents_whenEventFound_thenParamsPassedToStatisticClient() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder().build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventsPublic(searchParams))
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
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder().build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri(String.format("/events/%d", eventId))
                .hits(10L)
                .build();
        when(eventRepository.findEventsPublic(searchParams))
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
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder().build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri("/events/999")
                .hits(10L)
                .build();
        when(eventRepository.findEventsPublic(searchParams))
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
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
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
        when(eventRepository.findEventsPublic(searchParams))
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
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
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
        when(eventRepository.findEventsPublic(searchParams))
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
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
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
        when(eventRepository.findEventsPublic(searchParams))
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
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventsPublic(searchParams))
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

    @Test
    void findEventsAdmin_whenEventsNotFound_thenEmptyListReturned() {
        AdminSearchParamsDto searchParams = AdminSearchParamsDto.builder().build();
        when(eventRepository.findEventsAdmin(searchParams))
                .thenReturn(List.of());

        List<EventFullDto> foundEvents = eventService.findEventsAdmin(searchParams);

        assertTrue(foundEvents.isEmpty());
    }

    @Test
    void findEventsAdmin_whenEventFound_thenParamsPassedToStatisticClient() {
        AdminSearchParamsDto searchParams = AdminSearchParamsDto.builder().build();
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventsAdmin(searchParams))
                .thenReturn(List.of(foundEvent));

        eventService.findEventsAdmin(searchParams);

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
    void findEventsAdmin_whenViewsFound_thenAddedToDto() {
        AdminSearchParamsDto searchParams = AdminSearchParamsDto.builder().build();
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri(String.format("/events/%d", eventId))
                .hits(10L)
                .build();
        when(eventRepository.findEventsAdmin(searchParams))
                .thenReturn(List.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToFullDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidFullDto(foundEvent.getId()));
        }

        List<EventFullDto> foundEventDtos = eventService.findEventsAdmin(searchParams);

        assertThat(foundEventDtos.get(0).getViews(), equalTo(10L));
    }

    @Test
    void findEventsAdmin_whenViewsNotFround_thenZeroViewsAddedToDto() {
        AdminSearchParamsDto searchParams = AdminSearchParamsDto.builder().build();
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri("/events/999")
                .hits(10L)
                .build();
        when(eventRepository.findEventsAdmin(searchParams))
                .thenReturn(List.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToFullDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidFullDto(foundEvent.getId()));
        }

        List<EventFullDto> foundEventDtos = eventService.findEventsAdmin(searchParams);

        assertThat(foundEventDtos.get(0).getViews(), equalTo(0L));
    }


    @Test
    void updateEventByAdmin_whenEventNotFound_thenNotExistsExceptionThrown() {
        Long eventId = 0L;
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.updateEventByAdmin(eventId, updateRequest);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void updateEventByAdmin_whenEventStatusIsPublishedAndReject_thenForbiddenExceptionThrown() {
        Long eventId = 0L;
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction(AdminRequestStateAction.REJECT_EVENT);
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.PUBLISHED);
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        Executable executable = () -> eventService.updateEventByAdmin(eventId, updateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateEventByAdmin_whenEventStatusIsCancelledAndPublish_thenForbiddenExceptionThrown() {
        Long eventId = 0L;
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction(AdminRequestStateAction.PUBLISH_EVENT);
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.CANCELED);
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        Executable executable = () -> eventService.updateEventByAdmin(eventId, updateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateEventByAdmin_whenUpdateTitleNotNull_thenUpdatedFieldPassedToRepository() {
        Long eventId = 0L;
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setTitle("a".repeat(5));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getTitle(), equalTo(updateRequest.getTitle()));
    }

    @Test
    void updateEventByAdmin_whenUpdateDescriptionNotNull_thenUpdatedFieldPassedToRepository() {
        Long eventId = 0L;
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setDescription("a".repeat(21));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getDescription(), equalTo(updateRequest.getDescription()));
    }

    @Test
    void updateEventByAdmin_whenUpdateCategoryNotNullAndExist_thenUpdatedFieldPassedToRepository() {
        Long eventId = 0L;
        int categoryId = 1;
        Category category = TestDataProvider.getValidCategoryToSave();
        category.setId(categoryId);
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setCategory(categoryId);
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));
        when(categoryRepository.findCategoryById(categoryId))
                .thenReturn(Optional.of(category));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getCategory(), equalTo(category));
    }

    @Test
    void updateEventByAdmin_whenUpdateCategoryNotNullAndNotExist_thenInvalidParamExceptionThrown() {
        Long eventId = 0L;
        int categoryId = 1;
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setCategory(categoryId);
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));
        when(categoryRepository.findCategoryById(categoryId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.updateEventByAdmin(eventId, updateRequest);

        assertThrows(InvalidParamException.class, executable);
    }

    @Test
    void updateEventByAdmin_whenUpdateEventDate_thenUpdatedFieldPassedToRepository() {
        Long eventId = 0L;
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setEventDate(LocalDateTime.now().plusDays(1).withNano(0));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getEventDate(), equalTo(updateRequest.getEventDate()));
    }

    @Test
    void updateEventByAdmin_whenUpdateEventDateAndNotValid_thenForbiddenExceptionThrown() {
        Long eventId = 0L;
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setEventDate(LocalDateTime.now().plusMinutes(1).withNano(0));
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        Executable executable = () -> eventService.updateEventByAdmin(eventId, updateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateEventByAdmin_whenUpdatePaid_thenUpdatedFieldPassedToRepository() {
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setPaid(false);
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setPaid(true);
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().isPaid(), equalTo(updateRequest.getPaid()));
    }

    @Test
    void updateEventByAdmin_whenUpdateLocation_thenUpdatedFieldPassedToRepository() {
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setLocation(new Location(333.222f, 777.333f));
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getLocation(), equalTo(updateRequest.getLocation()));
    }

    @Test
    void updateEventByAdmin_whenUpdateRequestModeration_thenUpdatedFieldPassedToRepository() {
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setRequestModeration(false);
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setRequestModeration(true);
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().isRequestModeration(), equalTo(updateRequest.getRequestModeration()));
    }

    @Test
    void updateEventByAdmin_whenUpdateParticipantLimit_thenUpdatedFieldPassedToRepository() {
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setParticipantLimit(0);
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setParticipantLimit(100);
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getParticipantLimit(), equalTo(updateRequest.getParticipantLimit()));
    }

    @Test
    void updateEventByAdmin_whenUpdateStateAndActionIsReject_thenCanceledStatePassedToRepository() {
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.PENDING);
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction(AdminRequestStateAction.REJECT_EVENT);
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getState(), equalTo(EventState.CANCELED));
    }

    @Test
    void updateEventByAdmin_whenUpdateStateAndActionIsPublish_thenPublishedStateAndPublishDatePassedToRepository() {
        Long eventId = 0L;
        Event updatedEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        updatedEvent.setState(EventState.PENDING);
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction(AdminRequestStateAction.PUBLISH_EVENT);
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(updatedEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

        verify(eventRepository, times(1))
                .updateEvent(eventArgumentCaptor.capture());

        assertThat(eventArgumentCaptor.getValue().getState(), equalTo(EventState.PUBLISHED));
        assertThat(eventArgumentCaptor.getValue().getPublishedOn(),
                equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void updateEventByAdmin_whenEventFound_thenParamsPassedToStatisticClient() {
        Long eventId = 1L;
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(foundEvent));

        eventService.updateEventByAdmin(eventId, updateRequest);

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
    void updateEventByAdmin_whenViewsFound_thenAddedToDto() {
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri(String.format("/events/%d", eventId))
                .hits(10L)
                .build();
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.updateEventByAdmin(eventId, updateRequest);

        assertThat(foundEventDto.getViews(), equalTo(10L));
    }

    @Test
    void updateEventByAdmin_whenViewsNotFround_thenZeroViewsAddedToDto() {
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        ViewStatsDto viewStatsDto = ViewStatsDto.builder()
                .app("app")
                .uri("/events/999")
                .hits(10L)
                .build();
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.updateEventByAdmin(eventId, updateRequest);

        assertThat(foundEventDto.getViews(), equalTo(0L));
    }

}