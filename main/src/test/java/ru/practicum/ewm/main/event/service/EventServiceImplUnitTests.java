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
import ru.practicum.ewm.main.event.dto.searchrequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.SearchSortOptionDto;
import ru.practicum.ewm.main.event.dto.updaterequest.AdminRequestStateActionDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventAdminRequestDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventUserRequestDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UserRequestStateActionDto;
import ru.practicum.ewm.main.event.mapper.EventMapper;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.event.model.RateType;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.event.repository.RateRepository;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.InvalidParamException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.request.model.EventParticipationRequest;
import ru.practicum.ewm.main.request.model.RequestStatus;
import ru.practicum.ewm.main.request.repository.RequestRepository;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;
import ru.practicum.ewm.statistic.client.StatisticClient;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
    @Mock
    private RequestRepository requestRepository;
    @Mock
    private RateRepository rateDAO;
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
    private ArgumentCaptor<Long> userIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<EventState> eventStateArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> rateArgumentCaptor;

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
    void addEvent_whenUserNotFound_thenNotExistsExceptionThrown() {
        Long userId = 0L;
        NewEventDto newEventDto = TestDataProvider.getValidNewEventDto();
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.addEvent(userId, newEventDto);

        verify(eventRepository, times(0))
                .save(any());
        assertThrows(NotExistsException.class,
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
    void findUsersEvents_whenUserNoExists_thenNotExistsExceptionThrown() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        when(userRepository.userExists(userId))
                .thenReturn(false);

        Executable executable = () -> eventService.findUsersEvents(userId, from, size);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void findUsersEvents_whenEventsNotFound_thenEmptyListReturned() {
        int from = 0;
        int size = 10;
        Long userId = 0L;
        when(userRepository.userExists(userId))
                .thenReturn(true);
        when(eventRepository.findUserEvents(userId, from, size))
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
        when(userRepository.userExists(userId))
                .thenReturn(true);
        when(eventRepository.findUserEvents(userId, from, size))
                .thenReturn(List.of(event1, event2));

        try {
            eventService.findUsersEvents(userId, from, size);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(statisticClient, times(1))
                .getViewStats(startArgumentCaptor.capture(), any(), anyList(), anyBoolean());

        assertThat(startArgumentCaptor.getValue(), equalTo(event2CreatedOn.minusMinutes(1)));
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
        when(userRepository.userExists(userId))
                .thenReturn(true);
        when(eventRepository.findUserEvents(userId, from, size))
                .thenReturn(List.of(event1, event2));

        try {
            eventService.findUsersEvents(userId, from, size);
        } catch (Throwable e) {
            //capture argument to verify without full mocking
        }

        verify(statisticClient, times(1))
                .getViewStats(any(), endArgumentCaptor.capture(), anyList(), anyBoolean());

        assertThat(endArgumentCaptor.getValue().truncatedTo(ChronoUnit.SECONDS),
                equalTo(LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.SECONDS)));
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
        when(eventRepository.findUserEvents(userId, from, size))
                .thenReturn(List.of(event1, event2));
        when(userRepository.userExists(userId))
                .thenReturn(true);

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
        when(userRepository.userExists(userId))
                .thenReturn(true);
        when(eventRepository.findUserEvents(userId, from, size))
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
        when(userRepository.userExists(userId))
                .thenReturn(true);
        when(eventRepository.findUserEvents(userId, from, size))
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
        when(userRepository.userExists(userId))
                .thenReturn(true);
        when(eventRepository.findUserEvents(userId, from, size))
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
    void findUserEventById_whenUserNotExist_thenNotExistsExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        when(userRepository.userExists(userId))
                .thenReturn(false);

        Executable executable = () -> eventService.findUserEventById(userId, eventId);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void findUserEventById_whenEventNotFound_thenNotExistsExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        when(userRepository.userExists(userId))
                .thenReturn(true);
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
        when(userRepository.userExists(userId))
                .thenReturn(true);
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
        assertThat(startArgumentCaptor.getValue(),
                equalTo(foundEvent.getCreatedOn().minusMinutes(1)));
        assertThat(endArgumentCaptor.getValue(),
                equalTo(LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.SECONDS)));
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
        when(userRepository.userExists(userId))
                .thenReturn(true);
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
        when(userRepository.userExists(userId))
                .thenReturn(true);
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
        when(eventRepository.findEventByInitiatorIdAndEventId(userId, eventId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.updateEventByUser(userId, eventId, updateRequest);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void updateEventByUser_whenEventStatusIsPublished_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
        updateRequest.setStateAction(UserRequestStateActionDto.CANCEL_REVIEW);
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
        updateRequest.setStateAction(UserRequestStateActionDto.SEND_TO_REVIEW);
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        assertThat(startArgumentCaptor.getValue(),
                equalTo(foundEvent.getCreatedOn().minusMinutes(1)));
        assertThat(endArgumentCaptor.getValue(),
                equalTo(LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.SECONDS)));
        assertThat(urisArgumentCaptor.getValue().get(0), equalTo(String.format("/events/%d", eventId)));
        assertThat(uniqueArgumentCaptor.getValue(), equalTo(true));
    }

    @Test
    void updateEventByUser_whenViewsFound_thenAddedToDto() {
        Long userId = 0L;
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
        UpdateEventUserRequestDto updateRequest = new UpdateEventUserRequestDto();
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
    void findEventsPublic_whenInvoked_thenSearchTextFormatToLowerCase() {
        String ip = "1.1.1.1";
        String text = "TeXt";
        String expectedText = text.toLowerCase();
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .text(text)
                .build();

        eventService.findEventsPublic(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getText(), equalTo(expectedText));
    }

    @Test
    void findEventsPublic_whenInvoked_thenSearchStateEqualsPublished() {
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder().build();

        eventService.findEventsPublic(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getState(), equalTo(EventState.PUBLISHED));
    }

    @Test
    void findEventsPublic_whenStartRangeIsNullButEndRangeIsNot_thenEndPassedToRepository() {
        String ip = "1.1.1.1";
        LocalDateTime endRange = LocalDateTime.now().plusDays(1).withNano(0);
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .rangeEnd(endRange)
                .build();

        eventService.findEventsPublic(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getRangeEnd(), equalTo(endRange));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(), equalTo(null));
    }

    @Test
    void findEventsPublic_whenEndRangeIsNullButStartRangeIsNot_thenStartPassedToRepository() {
        String ip = "1.1.1.1";
        LocalDateTime startRange = LocalDateTime.now().plusDays(1).withNano(0);
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .rangeStart(startRange)
                .build();

        eventService.findEventsPublic(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(), equalTo(startRange));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeEnd(), equalTo(null));
    }

    @Test
    void findEventsPublic_whenEndRangeIsNullAndStartRangeIsNull_thenStartEqualsNowToRepository() {
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .build();

        eventService.findEventsPublic(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(),
                equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void findEventsPublic_whenSortOptionIsNull_thenSortByEventDate() {
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .build();

        eventService.findEventsPublic(searchParams, ip);

        verify(eventRepository, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getSortOption(),
                equalTo(SearchSortOptionDto.EVENT_DATE));
    }

    @Test
    void findEventsPublic_whenEventFound_thenParamsPassedToStatisticClient() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder().build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventsPublic(searchParams))
                .thenReturn(List.of(foundEvent));

        eventService.findEventsPublic(searchParams, ip);

        verify(statisticClient, times(1))
                .getViewStats(
                        startArgumentCaptor.capture(),
                        endArgumentCaptor.capture(),
                        urisArgumentCaptor.capture(),
                        uniqueArgumentCaptor.capture()
                );
        assertThat(startArgumentCaptor.getValue(),
                equalTo(foundEvent.getCreatedOn().minusMinutes(1)));
        assertThat(endArgumentCaptor.getValue(),
                equalTo(LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.SECONDS)));
        assertThat(urisArgumentCaptor.getValue().get(0), equalTo(String.format("/events/%d", eventId)));
        assertThat(uniqueArgumentCaptor.getValue(), equalTo(true));
    }

    @Test
    void findEventsPublic_whenViewsFound_thenAddedToDto() {
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

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.get(0).getViews(), equalTo(10L));
    }

    @Test
    void findEventsPublic_whenRatingFound_thenAddedToDto() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder().build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventsPublic(searchParams))
                .thenReturn(List.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());
        when(rateDAO.getRatingsForEvents(List.of(foundEvent.getId())))
                .thenReturn(Map.of(foundEvent.getId(), 50L));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.get(0).getRating(), equalTo(50L));
    }

    @Test
    void findEventsPublic_whenViewsNotFround_thenZeroViewsAddedToDto() {
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

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.get(0).getViews(), equalTo(0L));
    }

    @Test
    void findEventsPublic_whenRatingNotFound_thenZeroAddedToDto() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder().build();
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventsPublic(searchParams))
                .thenReturn(List.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());
        when(rateDAO.getRatingsForEvents(List.of(foundEvent.getId())))
                .thenReturn(Map.of());
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidShortDto(foundEvent.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.get(0).getRating(), equalTo(0L));
    }

    @Test
    void findEventsPublic_whenSortByViewsWithFromAndSize_thenSortedByViewsWithFromAndSizeLimit() {
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

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.size(), equalTo(1));
        assertThat(foundEvents.get(0).getViews(), equalTo(10L));
    }

    @Test
    void findEventsPublic_whenSortByRatingWithFromAndSize_thenSortedByViewsWithFromAndSizeLimit() {
        Long event1Id = 1L;
        Long event2Id = 2L;
        Long event3Id = 3L;
        Integer from = 1;
        Integer size = 1;
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .sortOption(SearchSortOptionDto.RATING)
                .from(from)
                .size(size)
                .build();
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event3 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setId(event1Id);
        event2.setId(event2Id);
        event3.setId(event3Id);
        when(eventRepository.findEventsPublic(searchParams))
                .thenReturn(List.of(event1, event2, event3));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());
        when(rateDAO.getRatingsForEvents(List.of(event1Id, event2Id, event3Id)))
                .thenReturn(Map.of(event1Id, 10L,
                        event2Id, 20L,
                        event3Id, 30L));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event1))
                    .thenReturn(TestDataProvider.getValidShortDto(event1.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event2))
                    .thenReturn(TestDataProvider.getValidShortDto(event2.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event3))
                    .thenReturn(TestDataProvider.getValidShortDto(event3.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.size(), equalTo(1));
        assertThat(foundEvents.get(0).getRating(), equalTo(20L));
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

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.size(), equalTo(2));
        assertThat(foundEvents.get(0).getViews(), equalTo(10L));
        assertThat(foundEvents.get(1).getViews(), equalTo(1L));
    }

    @Test
    void findEventsPublic_whenSortByRatingWithFrom_thenSortedByRatingWithFromAndSizeLimit() {
        Long event1Id = 1L;
        Long event2Id = 2L;
        Long event3Id = 3L;
        Integer from = 1;
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .sortOption(SearchSortOptionDto.RATING)
                .from(from)
                .build();
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event3 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setId(event1Id);
        event2.setId(event2Id);
        event3.setId(event3Id);
        when(eventRepository.findEventsPublic(searchParams))
                .thenReturn(List.of(event1, event2, event3));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());
        when(rateDAO.getRatingsForEvents(List.of(event1Id, event2Id, event3Id)))
                .thenReturn(Map.of(event1Id, 10L,
                        event2Id, 20L,
                        event3Id, 30L));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event1))
                    .thenReturn(TestDataProvider.getValidShortDto(event1.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event2))
                    .thenReturn(TestDataProvider.getValidShortDto(event2.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event3))
                    .thenReturn(TestDataProvider.getValidShortDto(event3.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.size(), equalTo(2));
        assertThat(foundEvents.get(0).getRating(), equalTo(20L));
        assertThat(foundEvents.get(1).getRating(), equalTo(10L));
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

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.size(), equalTo(1));
        assertThat(foundEvents.get(0).getViews(), equalTo(20L));
    }

    @Test
    void findEventsPublic_whenSortByRatingWithSize_thenSortedByRatingWithFromAndSizeLimit() {
        Long event1Id = 1L;
        Long event2Id = 2L;
        Long event3Id = 3L;
        Integer size = 1;
        String ip = "1.1.1.1";
        PublicSearchParamsDto searchParams = PublicSearchParamsDto.builder()
                .sortOption(SearchSortOptionDto.RATING)
                .size(size)
                .build();
        Event event1 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        Event event3 = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event1.setId(event1Id);
        event2.setId(event2Id);
        event3.setId(event3Id);
        when(eventRepository.findEventsPublic(searchParams))
                .thenReturn(List.of(event1, event2, event3));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());
        when(rateDAO.getRatingsForEvents(List.of(event1Id, event2Id, event3Id)))
                .thenReturn(Map.of(event1Id, 10L,
                        event2Id, 20L,
                        event3Id, 30L));
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event1))
                    .thenReturn(TestDataProvider.getValidShortDto(event1.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event2))
                    .thenReturn(TestDataProvider.getValidShortDto(event2.getId()));
            eventMapperMock.when(() -> EventMapper.mapToShortDto(event3))
                    .thenReturn(TestDataProvider.getValidShortDto(event3.getId()));
        }

        List<EventShortDto> foundEvents = eventService.findEventsPublic(searchParams, ip);

        assertThat(foundEvents.size(), equalTo(1));
        assertThat(foundEvents.get(0).getRating(), equalTo(30L));
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

        eventService.findEventsPublic(searchParams, ip);

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
        assertThat(startArgumentCaptor.getValue(),
                equalTo(foundEvent.getCreatedOn().minusMinutes(1)));
        assertThat(endArgumentCaptor.getValue(),
                equalTo(LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.SECONDS)));
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
    void findEventByIdPublic_whenRatingFound_thenAddedToDto() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventByIdAndState(eventId, EventState.PUBLISHED))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());
        when(rateDAO.getRatingForEvent(eventId))
                .thenReturn(50L);
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToFullDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidFullDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.findEventByIdPublic(eventId, ip);

        assertThat(foundEventDto.getRating(), equalTo(50L));
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
    void findEventByIdPublic_whenRatingNotFound_thenZeroAddedToDto() {
        Long eventId = 1L;
        String ip = "1.1.1.1";
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        when(eventRepository.findEventByIdAndState(eventId, EventState.PUBLISHED))
                .thenReturn(Optional.of(foundEvent));
        when(statisticClient.getViewStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());
        when(rateDAO.getRatingForEvent(eventId))
                .thenReturn(0L);
        try (MockedStatic<EventMapper> eventMapperMock = Mockito.mockStatic(EventMapper.class)) {
            eventMapperMock.when(() -> EventMapper.mapToFullDto(foundEvent))
                    .thenReturn(TestDataProvider.getValidFullDto(foundEvent.getId()));
        }

        EventFullDto foundEventDto = eventService.findEventByIdPublic(eventId, ip);

        assertThat(foundEventDto.getRating(), equalTo(0L));
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
        assertThat(startArgumentCaptor.getValue(),
                equalTo(foundEvent.getCreatedOn().minusMinutes(1)));
        assertThat(endArgumentCaptor.getValue(),
                equalTo(LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.SECONDS)));
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
        when(eventRepository.findEventById(eventId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.updateEventByAdmin(eventId, updateRequest);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void updateEventByAdmin_whenEventStatusIsPublishedAndReject_thenForbiddenExceptionThrown() {
        Long eventId = 0L;
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
        updateRequest.setStateAction(AdminRequestStateActionDto.REJECT_EVENT);
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
        updateRequest.setStateAction(AdminRequestStateActionDto.PUBLISH_EVENT);
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
        updateRequest.setStateAction(AdminRequestStateActionDto.REJECT_EVENT);
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
        updateRequest.setStateAction(AdminRequestStateActionDto.PUBLISH_EVENT);
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        assertThat(startArgumentCaptor.getValue(),
                equalTo(foundEvent.getCreatedOn().minusMinutes(1)));
        assertThat(endArgumentCaptor.getValue(),
                equalTo(LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.SECONDS)));
        assertThat(urisArgumentCaptor.getValue().get(0), equalTo(String.format("/events/%d", eventId)));
        assertThat(uniqueArgumentCaptor.getValue(), equalTo(true));
    }

    @Test
    void updateEventByAdmin_whenViewsFound_thenAddedToDto() {
        Long eventId = 1L;
        Event foundEvent = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        foundEvent.setId(eventId);
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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
        UpdateEventAdminRequestDto updateRequest = new UpdateEventAdminRequestDto();
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

    @Test
    void addRateToEvent_whenRaterNotFound_thenNotExistsExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        RateType rateType = RateType.LIKE;
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.addRateToEvent(userId, eventId, rateType);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void addRateToEvent_whenPublishedEventNotFound_thenNotExistsExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        RateType rateType = RateType.LIKE;
        EventState eventState = EventState.PUBLISHED;
        User rater = TestDataProvider.getValidUserToSave();
        rater.setId(userId);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(rater));
        when(eventRepository.findEventByIdAndState(eventId, eventState))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.addRateToEvent(userId, eventId, rateType);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void addRateToEvent_whenRaterIsEventInitiator_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        RateType rateType = RateType.LIKE;
        EventState eventState = EventState.PUBLISHED;
        User rater = TestDataProvider.getValidUserToSave();
        rater.setId(userId);
        Event event = TestDataProvider.getValidNotSavedEvent(rater, new Category());
        event.setId(eventId);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(rater));
        when(eventRepository.findEventByIdAndState(eventId, eventState))
                .thenReturn(Optional.of(event));

        Executable executable = () -> eventService.addRateToEvent(userId, eventId, rateType);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void addRateToEvent_whenRaterHasNoConfirmedRequestsForEvent_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        RateType rateType = RateType.LIKE;
        EventState eventState = EventState.PUBLISHED;
        RequestStatus requestStatus = RequestStatus.CONFIRMED;
        User rater = TestDataProvider.getValidUserToSave();
        rater.setId(userId);
        Event event = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event.setId(eventId);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(rater));
        when(eventRepository.findEventByIdAndState(eventId, eventState))
                .thenReturn(Optional.of(event));
        when(requestRepository.findByUserEventAndStatus(userId, eventId, requestStatus))
                .thenReturn(Optional.empty());

        Executable executable = () -> eventService.addRateToEvent(userId, eventId, rateType);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void addRateToEvent_whenLike_thenRateIsPositive1() {
        Long userId = 0L;
        Long eventId = 1L;
        RateType rateType = RateType.LIKE;
        EventState eventState = EventState.PUBLISHED;
        RequestStatus requestStatus = RequestStatus.CONFIRMED;
        User rater = TestDataProvider.getValidUserToSave();
        rater.setId(userId);
        Event event = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event.setId(eventId);
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(rater, event);
        request.setRequestStatus(requestStatus);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(rater));
        when(eventRepository.findEventByIdAndState(eventId, eventState))
                .thenReturn(Optional.of(event));
        when(requestRepository.findByUserEventAndStatus(userId, eventId, requestStatus))
                .thenReturn(Optional.of(request));

        eventService.addRateToEvent(userId, eventId, rateType);

        verify(rateDAO, times(1))
                .addRate(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        rateArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(rateArgumentCaptor.getValue(), equalTo(1));
    }

    @Test
    void addRateToEvent_whenDislike_thenRateIsNegative1() {
        Long userId = 0L;
        Long eventId = 1L;
        RateType rateType = RateType.DISLIKE;
        EventState eventState = EventState.PUBLISHED;
        RequestStatus requestStatus = RequestStatus.CONFIRMED;
        User rater = TestDataProvider.getValidUserToSave();
        rater.setId(userId);
        Event event = TestDataProvider.getValidNotSavedEvent(new User(), new Category());
        event.setId(eventId);
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(rater, event);
        request.setRequestStatus(requestStatus);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(rater));
        when(eventRepository.findEventByIdAndState(eventId, eventState))
                .thenReturn(Optional.of(event));
        when(requestRepository.findByUserEventAndStatus(userId, eventId, requestStatus))
                .thenReturn(Optional.of(request));

        eventService.addRateToEvent(userId, eventId, rateType);

        verify(rateDAO, times(1))
                .addRate(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        rateArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(rateArgumentCaptor.getValue(), equalTo(-1));
    }

    @Test
    void deleteRateFromEvent_whenLike_thenRateIsPositive1() {
        Long userId = 0L;
        Long eventId = 1L;
        RateType rateType = RateType.LIKE;

        eventService.deleteRateFromEvent(userId, eventId, rateType);

        verify(rateDAO, times(1))
                .deleteRate(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        rateArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(rateArgumentCaptor.getValue(), equalTo(1));
    }

    @Test
    void deleteRateFromEvent_whenDislike_thenRateIsNegative1() {
        Long userId = 0L;
        Long eventId = 1L;
        RateType rateType = RateType.DISLIKE;

        eventService.deleteRateFromEvent(userId, eventId, rateType);

        verify(rateDAO, times(1))
                .deleteRate(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        rateArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(rateArgumentCaptor.getValue(), equalTo(-1));
    }

}