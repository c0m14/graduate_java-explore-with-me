package ru.practicum.ewm.main.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.mapper.CategoryMapper;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;
import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.dto.NewEventDto;
import ru.practicum.ewm.main.event.dto.searchRequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchRequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchRequest.SearchSortOptionDto;
import ru.practicum.ewm.main.event.dto.updateRequest.*;
import ru.practicum.ewm.main.event.mapper.EventMapper;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.InvalidParamException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.user.dto.UserShortDto;
import ru.practicum.ewm.main.user.mapper.UserMapper;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;
import ru.practicum.ewm.statistic.client.StatisticClient;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatisticClient statisticClient;

    @Override
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        checkNewEventDate(newEventDto.getEventDate());
        User eventInitiator = getUserFromDb(userId);
        Category eventCategory = getCategoryFromDb(newEventDto.getCategory());

        Event event = EventMapper.mapToEntity(newEventDto, eventInitiator, eventCategory);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now().withNano(0));
        Event savedEvent = eventRepository.save(event);

        CategoryDto categoryDto = CategoryMapper.mapToCategoryDto(eventCategory);
        UserShortDto initiatorDto = UserMapper.mapToShortDto(eventInitiator);
        return EventMapper.mapToFullDto(savedEvent, categoryDto, initiatorDto, 0L, 0);
    }

    @Override
    public List<EventShortDto> findUsersEvents(Long userId, int from, int size) {
        checkIfUserExist(userId);

        List<Event> foundEvents = eventRepository.findUsersEvents(userId, from, size);
        if (foundEvents.isEmpty()) {
            return List.of();
        }

        return mapToShortDtoAndFetchViews(foundEvents);
    }

    @Override
    public EventFullDto findUserEventById(Long userId, Long eventId) {
        checkIfUserExist(userId);
        Event foundEvent = eventRepository.findEventByInitiatorIdAndEventId(userId, eventId).orElseThrow(
                () -> new NotExistsException(
                        "Event",
                        String.format("Event with id %d and initiator id %d not exists", eventId, userId)
                ));

        return mapToFullDtoAndFetchViews(foundEvent);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventRequest) {
        Event eventToUpdate = eventRepository.findEventById(eventId).orElseThrow(
                () -> new NotExistsException(
                        "Event",
                        String.format("Event with id %d not exists", eventId)
                )
        );
        checkIfAvailableForUpdate(eventToUpdate, updateEventRequest, UpdateType.ADMIN);
        updateEventFields(eventToUpdate, updateEventRequest, UpdateType.ADMIN);
        eventRepository.updateEvent(eventToUpdate);

        return mapToFullDtoAndFetchViews(eventToUpdate);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateEventRequest) {
        Event eventToUpdate = eventRepository.findEventByInitiatorIdAndEventId(userId, eventId).orElseThrow(
                () -> new NotExistsException(
                        "Event",
                        String.format("Event with id %d and initiator id %d not exists", eventId, userId)
                )
        );

        checkIfAvailableForUpdate(eventToUpdate, updateEventRequest, UpdateType.USER);
        updateEventFields(eventToUpdate, updateEventRequest, UpdateType.USER);
        eventRepository.updateEvent(eventToUpdate);

        return mapToFullDtoAndFetchViews(eventToUpdate);
    }

    @Override
    public List<EventShortDto> findEvents(PublicSearchParamsDto searchParams, String ip) {
        formatSearchTextToLowerCase(searchParams);
        searchParams.setState(EventState.PUBLISHED);
        defineSearchDatesRange(searchParams);
        defineSearchSort(searchParams);

        List<Event> foundEvents = eventRepository.findEventsPublic(searchParams);
        if (foundEvents.isEmpty()) {
            return List.of();
        }
        if (searchParams.getOnlyAvailable()) {
            foundEvents = foundEvents.stream().filter(this::ifParticipationLimitNotReached)
                    .collect(Collectors.toList());
        }

        List<EventShortDto> eventShortDtos = mapToShortDtoAndFetchViews(foundEvents);

        if (searchParams.getSortOption().equals(SearchSortOptionDto.VIEWS)) {
            eventShortDtos = sortByViews(eventShortDtos, searchParams.getFrom(), searchParams.getSize());
        }

        saveEndpointHit("/events", ip);
        return eventShortDtos;
    }

    @Override
    public EventFullDto findEventByIdPublic(Long eventId, String ip) {
        Event foundEvent = eventRepository.findEventByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotExistsException(
                                "Event",
                                String.format("Published event with id %d not exists", eventId)
                        )
                );

        EventFullDto eventFullDto = mapToFullDtoAndFetchViews(foundEvent);

        saveEndpointHit(String.format("/events/%d", eventId), ip);

        return eventFullDto;
    }

    @Override
    public List<EventFullDto> findEventsAdmin(AdminSearchParamsDto searchParams) {
        List<Event> foundEvents = eventRepository.findEventsAdmin(searchParams);
        if (foundEvents.isEmpty()) {
            return List.of();
        }

        return mapToFullDtoAndFetchViews(foundEvents);
    }

    private void checkIfAvailableForUpdate(
            Event eventToUpdate,
            UpdateEventRequest updatedRequest,
            UpdateType updateType) {

        if (updateType.equals(UpdateType.USER)) {
            checkEventStateUser(eventToUpdate.getState());
            checkEventDate(eventToUpdate, updatedRequest, updateType);
        }

        if (updateType.equals(UpdateType.ADMIN)) {
            if (((UpdateEventAdminRequest) updatedRequest).getStateAction() == null) {
                checkEventDate(eventToUpdate, updatedRequest, updateType);
            } else {
                AdminRequestStateAction stateAction = ((UpdateEventAdminRequest) updatedRequest).getStateAction();
                checkEventStateAdmin(eventToUpdate.getState(), stateAction);

                if (stateAction.equals(AdminRequestStateAction.PUBLISH_EVENT)) {
                    checkEventDate(eventToUpdate, updatedRequest, updateType);
                }
            }
        }
    }

    private void checkNewEventDate(LocalDateTime eventDate) {
        Long minimumIntervalBeforeEventStartInHours = 2L;

        if (eventDate.isBefore(LocalDateTime.now().plusHours(minimumIntervalBeforeEventStartInHours))) {
            throw new ForbiddenException(
                    "Event date",
                    String.format("Event date should be more than %d hours from now",
                            minimumIntervalBeforeEventStartInHours)
            );
        }
    }

    private void checkEventDate(Event eventToUpdate, UpdateEventRequest updatedRequest, UpdateType updateType) {
        LocalDateTime checkedDate;
        if (updatedRequest != null && updatedRequest.getEventDate() != null) {
            checkedDate = updatedRequest.getEventDate();
        } else {
            checkedDate = eventToUpdate.getEventDate();
        }

        Long minimumIntervalBeforeEventStartInHours = null;
        if (updateType.equals(UpdateType.USER)) {
            minimumIntervalBeforeEventStartInHours = 2L;
        }
        if (updateType.equals(UpdateType.ADMIN)) {
            minimumIntervalBeforeEventStartInHours = 1L;
        }

        if (checkedDate.isBefore(LocalDateTime.now().plusHours(minimumIntervalBeforeEventStartInHours))) {
            throw new ForbiddenException(
                    "Event date",
                    String.format("Event date should be more than %d hours from now",
                            minimumIntervalBeforeEventStartInHours)
            );
        }
    }

    private void checkEventStateUser(EventState currentState) {
        if (currentState.equals(EventState.PUBLISHED)) {
            throw new ForbiddenException(
                    "Forbidden",
                    String.format("Not allowed to update event with status %s", currentState)
            );
        }
    }

    private void checkEventStateAdmin(EventState currentState, AdminRequestStateAction stateAction) {
        if (stateAction.equals(AdminRequestStateAction.REJECT_EVENT)) {
            if (currentState.equals(EventState.PUBLISHED)) {
                throw new ForbiddenException(
                        "Forbidden",
                        String.format("Not allowed to reject event with status %s", currentState)
                );
            }
        }
        if (stateAction.equals(AdminRequestStateAction.PUBLISH_EVENT)) {
            if (!currentState.equals((EventState.PENDING))) {
                throw new ForbiddenException(
                        "Forbidden",
                        String.format("Not allowed to publish event with status %s", currentState)
                );
            }
        }
    }


    private User getUserFromDb(Long userId) {
        return userRepository.findUserById(userId).orElseThrow(
                () -> new InvalidParamException(
                        "User id",
                        String.format("User with id %d not exists", userId)
                )
        );
    }

    private void checkIfUserExist(Long userId) {
        getUserFromDb(userId);
    }

    private Category getCategoryFromDb(int categoryId) {
        return categoryRepository.findCategoryById(categoryId).orElseThrow(
                () -> new InvalidParamException(
                        "Category id",
                        String.format("Category with id %d not exists", categoryId)
                )
        );
    }

    private Map<Long, Long> getEventsViews(List<Event> events) {
        List<Event> sortedByCreatedASC = events.stream()
                .sorted(Comparator.comparing(Event::getCreatedOn))
                .collect(Collectors.toList());
        LocalDateTime earliestDate = sortedByCreatedASC.get(0).getCreatedOn();
        LocalDateTime latestDate = LocalDateTime.now().withNano(0);
        List<String> uris = events.stream()
                .map(event -> String.format("/events/%d", event.getId()))
                .collect(Collectors.toList());
        boolean unique = true;

        List<ViewStatsDto> stats = statisticClient.getViewStats(
                earliestDate,
                latestDate,
                uris,
                unique
        );

        return stats.stream()
                .collect(Collectors.toMap(viewStatsDto -> {
                    String uri = viewStatsDto.getUri();
                    String[] uriElements = uri.split("/");

                    return Long.parseLong(uriElements[uriElements.length - 1]);
                }, ViewStatsDto::getHits));
    }

    private void setViewsToEvents(List<? extends EventShortDto> eventDtos, Map<Long, Long> eventsViews) {
        eventDtos.forEach(eventShortDto -> eventShortDto.setViews(
                eventsViews.get(eventShortDto.getId()) != null ? eventsViews.get(eventShortDto.getId()) : 0));
    }

    private void updateEventFields(Event eventToUpdate, UpdateEventRequest updateRequest, UpdateType updateType) {
        if (updateRequest.getTitle() != null) {
            eventToUpdate.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getAnnotation() != null) {
            eventToUpdate.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            eventToUpdate.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getCategory() != null) {
            Category newCategory = getCategoryFromDb(updateRequest.getCategory());
            eventToUpdate.setCategory(newCategory);
        }
        if (updateRequest.getEventDate() != null) {
            eventToUpdate.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getPaid() != null) {
            eventToUpdate.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getLocation() != null) {
            eventToUpdate.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getRequestModeration() != null) {
            eventToUpdate.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getParticipantLimit() != null) {
            eventToUpdate.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateType.equals(UpdateType.USER)) {
            UserRequestStateAction stateAction = ((UpdateEventUserRequest) updateRequest).getStateAction();
            if (stateAction != null) {
                switch (stateAction) {
                    case CANCEL_REVIEW:
                        eventToUpdate.setState(EventState.CANCELED);
                        break;
                    case SEND_TO_REVIEW:
                        eventToUpdate.setState(EventState.PENDING);
                        break;
                }
            }
        } else if (updateType.equals(UpdateType.ADMIN)) {
            AdminRequestStateAction stateAction = ((UpdateEventAdminRequest) updateRequest).getStateAction();
            if (stateAction != null) {
                switch (stateAction) {
                    case REJECT_EVENT:
                        eventToUpdate.setState(EventState.CANCELED);
                        break;
                    case PUBLISH_EVENT:
                        eventToUpdate.setPublishedOn(LocalDateTime.now().withNano(0));
                        eventToUpdate.setState(EventState.PUBLISHED);
                        break;
                }
            }
        }
    }

    private void formatSearchTextToLowerCase(PublicSearchParamsDto searchParams) {
        if (searchParams.getText() != null && !searchParams.getText().isBlank()) {
            String formattedText = searchParams.getText().toLowerCase();
            searchParams.setText(formattedText);
        }
    }

    private void defineSearchDatesRange(PublicSearchParamsDto searchParams) {
        if (searchParams.getRangeStart() == null && searchParams.getRangeEnd() == null) {
            searchParams.setRangeStart(LocalDateTime.now().withNano(0));
        }
    }

    private void defineSearchSort(PublicSearchParamsDto searchParams) {
        if (searchParams.getSortOption() == null) {
            searchParams.setSortOption(SearchSortOptionDto.EVENT_DATE);
        }
    }

    private void saveEndpointHit(String url, String ip) {
        statisticClient.saveEndpointHit(EndpointHitDto.builder()
                .app("Ewm-main")
                .uri(url)
                .ip(ip)
                .timestamp(LocalDateTime.now().withNano(0))
                .build());
    }

    private List<EventShortDto> sortByViews(List<EventShortDto> originalEvents, Integer from, Integer size) {
        Stream<EventShortDto> eventsStream = originalEvents.stream()
                .sorted(Comparator.comparing(EventShortDto::getViews).reversed());
        if (from != null) {
            eventsStream = eventsStream.skip(from);
        }
        if (size != null) {
            eventsStream = eventsStream.limit(size);
        }
        return eventsStream.collect(Collectors.toList());
    }

    private List<EventShortDto> mapToShortDtoAndFetchViews(List<Event> events) {
        Map<Long, Long> eventsViews = getEventsViews(events);

        List<EventShortDto> eventShortDtos = events.stream()
                .map(EventMapper::mapToShortDto)
                .collect(Collectors.toList());

        setViewsToEvents(eventShortDtos, eventsViews);

        return eventShortDtos;
    }

    private EventFullDto mapToFullDtoAndFetchViews(Event event) {
        Map<Long, Long> eventViews = getEventsViews(List.of(event));

        EventFullDto eventFullDto = EventMapper.mapToFullDto(event);

        setViewsToEvents(List.of(eventFullDto), eventViews);

        return eventFullDto;
    }

    private List<EventFullDto> mapToFullDtoAndFetchViews(List<Event> events) {
        Map<Long, Long> eventsViews = getEventsViews(events);

        List<EventFullDto> eventFullDtos = events.stream()
                .map(EventMapper::mapToFullDto)
                .collect(Collectors.toList());

        setViewsToEvents(eventFullDtos, eventsViews);

        return eventFullDtos;
    }

    private boolean ifParticipationLimitNotReached(Event event) {
        return event.getParticipantLimit() == 0 || event.getConfirmedRequests() >= event.getParticipantLimit();
    }

}
