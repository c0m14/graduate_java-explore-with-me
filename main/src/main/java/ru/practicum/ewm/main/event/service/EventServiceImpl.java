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
import ru.practicum.ewm.main.event.dto.searchrequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.SearchSortOptionDto;
import ru.practicum.ewm.main.event.dto.updaterequest.*;
import ru.practicum.ewm.main.event.mapper.EventMapper;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.model.RateType;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.event.repository.RateRepository;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.InvalidParamException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.request.model.RequestStatus;
import ru.practicum.ewm.main.request.repository.RequestRepository;
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatisticClient statisticClient;
    private final RequestRepository requestRepository;
    private final RateRepository rateDAO;

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        checkNewEventDate(newEventDto.getEventDate());
        User eventInitiator = getUserFromDb(userId);
        Category eventCategory = getCategoryFromDb(newEventDto.getCategory());
        defineDefaultFields(newEventDto);

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

        List<Event> foundEvents = eventRepository.findUserEvents(userId, from, size);
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

        return mapToFullDtoAndFetchViewsAndRating(foundEvent);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequestDto updateEventRequest) {
        Event eventToUpdate = eventRepository.findEventById(eventId).orElseThrow(
                () -> new NotExistsException(
                        "Event",
                        String.format("Event with id %d not exists", eventId)
                )
        );
        checkIfAvailableForUpdate(eventToUpdate, updateEventRequest, UpdateType.ADMIN);
        updateEventFields(eventToUpdate, updateEventRequest, UpdateType.ADMIN);
        eventRepository.updateEvent(eventToUpdate);

        return mapToFullDtoAndFetchViewsAndRating(eventToUpdate);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequestDto updateEventRequest) {
        Event eventToUpdate = eventRepository.findEventByInitiatorIdAndEventId(userId, eventId).orElseThrow(
                () -> new NotExistsException(
                        "Event",
                        String.format("Event with id %d and initiator id %d not exists", eventId, userId)
                )
        );

        checkIfAvailableForUpdate(eventToUpdate, updateEventRequest, UpdateType.USER);
        updateEventFields(eventToUpdate, updateEventRequest, UpdateType.USER);
        eventRepository.updateEvent(eventToUpdate);

        return mapToFullDtoAndFetchViewsAndRating(eventToUpdate);
    }

    @Override
    public List<EventShortDto> findEventsPublic(PublicSearchParamsDto searchParams, String ip) {
        checkSearchDates(searchParams);
        formatSearchTextToLowerCase(searchParams);
        searchParams.setState(EventState.PUBLISHED);
        defineSearchDatesRange(searchParams);
        defineSearchSort(searchParams);

        List<Event> foundEvents = eventRepository.findEventsPublic(searchParams);
        if (foundEvents.isEmpty()) {
            return List.of();
        }

        List<EventShortDto> eventShortDtos = mapToShortDtoAndFetchViews(foundEvents);

        if (searchParams.getSortOption().equals(SearchSortOptionDto.VIEWS)) {
            eventShortDtos = sortByViews(eventShortDtos, searchParams.getFrom(), searchParams.getSize());
        }
        if (searchParams.getSortOption().equals(SearchSortOptionDto.RATING)) {
            eventShortDtos = sortByRating(eventShortDtos, searchParams.getFrom(), searchParams.getSize());
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

        EventFullDto eventFullDto = mapToFullDtoAndFetchViewsAndRating(foundEvent);

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

    @Override
    @Transactional
    public void addRateToEvent(Long userId, Long eventId, RateType rateType) {
        User rater = getUserFromDb(userId);
        Event event = getEventFromDbByIdAndState(eventId, EventState.PUBLISHED);
        checkRater(rater, event);

        int rate = defineRate(rateType);
        rateDAO.addRate(userId, eventId, rate);
    }

    @Override
    @Transactional
    public void deleteRateFromEvent(Long userId, Long eventId, RateType rateType) {
        int rate = defineRate(rateType);
        rateDAO.deleteRate(userId, eventId, rate);
    }

    private void checkIfAvailableForUpdate(
            Event eventToUpdate,
            NewEventDto updatedRequest,
            UpdateType updateType) {

        if (updateType.equals(UpdateType.USER)) {
            checkEventStateUser(eventToUpdate.getState());
            checkEventDate(eventToUpdate, updatedRequest, updateType);
        }

        if (updateType.equals(UpdateType.ADMIN)) {
            if (((UpdateEventAdminRequestDto) updatedRequest).getStateAction() == null) {
                checkEventDate(eventToUpdate, updatedRequest, updateType);
            } else {
                AdminRequestStateActionDto stateAction = ((UpdateEventAdminRequestDto) updatedRequest).getStateAction();
                checkEventStateAdmin(eventToUpdate.getState(), stateAction);

                if (stateAction.equals(AdminRequestStateActionDto.PUBLISH_EVENT)) {
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

    private void checkEventDate(Event eventToUpdate, NewEventDto updatedRequest, UpdateType updateType) {
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

    private void checkEventStateAdmin(EventState currentState, AdminRequestStateActionDto stateAction) {
        if (stateAction.equals(AdminRequestStateActionDto.REJECT_EVENT)) {
            if (currentState.equals(EventState.PUBLISHED)) {
                throw new ForbiddenException(
                        "Forbidden",
                        String.format("Not allowed to reject event with status %s", currentState)
                );
            }
        }
        if (stateAction.equals(AdminRequestStateActionDto.PUBLISH_EVENT)) {
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
                () -> new NotExistsException(
                        "User",
                        String.format("User with id %d not exists", userId)
                )
        );
    }

    private void checkIfUserExist(Long userId) {
        if (!userRepository.userExists(userId)) {
            throw new NotExistsException(
                    "User",
                    String.format("User with id %d not exists", userId)
            );
        }
    }

    private Category getCategoryFromDb(int categoryId) {
        return categoryRepository.findCategoryById(categoryId).orElseThrow(
                () -> new InvalidParamException(
                        "Category id",
                        String.format("Category with id %d not exists", categoryId)
                )
        );
    }

    private Event getEventFromDbByIdAndState(Long eventId, EventState state) {
        return eventRepository.findEventByIdAndState(eventId, state).orElseThrow(
                () -> new NotExistsException(
                        "Event",
                        String.format("No events with id %d and state %s exist", eventId, state)
                )
        );
    }

    private Map<Long, Long> getEventsViews(List<Event> events) {
        List<Event> sortedByCreatedASC = events.stream()
                .sorted(Comparator.comparing(Event::getCreatedOn))
                .collect(Collectors.toList());
        LocalDateTime earliestDate = sortedByCreatedASC.get(0).getCreatedOn().minusMinutes(1);
        LocalDateTime latestDate = LocalDateTime.now().withNano(0).plusMinutes(1);
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

    private void setViewsToEventsDtos(List<? extends EventShortDto> eventDtos, Map<Long, Long> eventsViews) {
        eventDtos.forEach(eventShortDto -> eventShortDto.setViews(
                eventsViews.get(eventShortDto.getId()) != null ? eventsViews.get(eventShortDto.getId()) : 0));
    }

    private void updateEventFields(Event eventToUpdate, NewEventDto updateRequest, UpdateType updateType) {
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
            UserRequestStateActionDto stateAction = ((UpdateEventUserRequestDto) updateRequest).getStateAction();
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
            AdminRequestStateActionDto stateAction = ((UpdateEventAdminRequestDto) updateRequest).getStateAction();
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

    private List<EventShortDto> sortByRating(List<EventShortDto> originalEvents, Integer from, Integer size) {
        Stream<EventShortDto> eventsStream = originalEvents.stream()
                .sorted(Comparator.comparing(EventShortDto::getRating).reversed());
        if (from != null) {
            eventsStream = eventsStream.skip(from);
        }
        if (size != null) {
            eventsStream = eventsStream.limit(size);
        }
        return eventsStream.collect(Collectors.toList());
    }

    private List<EventShortDto> mapToShortDtoAndFetchViews(List<Event> events) {
        List<EventShortDto> eventShortDtos = events.stream()
                .map(EventMapper::mapToShortDto)
                .collect(Collectors.toList());

        Map<Long, Long> eventsViews = getEventsViews(events);
        setViewsToEventsDtos(eventShortDtos, eventsViews);

        Map<Long, Long> eventsRatings = getEventsRatings(events);
        setRatingsToEventsDtos(eventShortDtos, eventsRatings);


        return eventShortDtos;
    }

    private EventFullDto mapToFullDtoAndFetchViewsAndRating(Event event) {
        EventFullDto eventFullDto = mapToFullDtoAndFetchViews(List.of(event)).get(0);
        setRatingToEvent(eventFullDto);

        return eventFullDto;
    }

    private List<EventFullDto> mapToFullDtoAndFetchViews(List<Event> events) {
        Map<Long, Long> eventsViews = getEventsViews(events);

        List<EventFullDto> eventFullDtos = events.stream()
                .map(EventMapper::mapToFullDto)
                .collect(Collectors.toList());

        setViewsToEventsDtos(eventFullDtos, eventsViews);

        return eventFullDtos;
    }

    private void checkSearchDates(PublicSearchParamsDto searchParams) {
        if (searchParams.getRangeEnd() != null && searchParams.getRangeStart() != null &&
                searchParams.getRangeEnd().isBefore(searchParams.getRangeStart())) {
            throw new InvalidParamException(
                    "Search dates",
                    String.format("StartRange: %s must not be later than EndRange:%s",
                            searchParams.getRangeStart(), searchParams.getRangeEnd()
                    )
            );
        }
    }

    private void checkRater(User rater, Event event) {
        if (Objects.equals(rater.getId(), event.getInitiator().getId())) {
            throw new ForbiddenException(
                    "Forbidden",
                    "User can not rate his own event"
            );
        }

        requestRepository.findByUserEventAndStatus(
                rater.getId(), event.getId(), RequestStatus.CONFIRMED).orElseThrow(
                () -> new ForbiddenException(
                        "Forbidden",
                        String.format("There are no confirmed requests from user with id %d to event with id %d",
                                rater.getId(), event.getId()))
        );
    }

    private int defineRate(RateType rateType) {
        switch (rateType) {
            case LIKE:
                return 1;
            case DISLIKE:
                return -1;
            default:
                throw new ForbiddenException("Rate type", String.format("Rate type %s incorrect", rateType.toString()));
        }
    }

    private void setRatingToEvent(EventFullDto eventFullDto) {
        Long rating = rateDAO.getRatingForEvent(eventFullDto.getId());
        eventFullDto.setRating(rating);
    }

    private Map<Long, Long> getEventsRatings(List<Event> events) {
        List<Long> eventsIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        return rateDAO.getRatingsForEvents(eventsIds);
    }

    private void setRatingsToEventsDtos(List<? extends EventShortDto> eventDtos, Map<Long, Long> eventsRatings) {
        eventDtos.forEach(eventShortDto -> eventShortDto.setRating(
                eventsRatings.get(eventShortDto.getId()) != null ? eventsRatings.get(eventShortDto.getId()) : 0));
    }

    private void defineDefaultFields(NewEventDto newEventDto) {
        if (newEventDto.getPaid() == null) {
            newEventDto.setPaid(false);
        }
        if (newEventDto.getParticipantLimit() == null) {
            newEventDto.setParticipantLimit(0);
        }
        if (newEventDto.getRequestModeration() == null) {
            newEventDto.setRequestModeration(true);
        }
    }

}
