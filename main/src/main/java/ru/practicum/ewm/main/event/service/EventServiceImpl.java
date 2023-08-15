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
import ru.practicum.ewm.main.event.dto.UpdateEventUserRequest;
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
import ru.practicum.ewm.statistic.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatisticClient statisticClient;

    @Override
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        checkEventDate(newEventDto.getEventDate());
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

        List<Event> foundEvents = eventRepository.getUsersEvents(userId, from, size);
        if (foundEvents.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> eventsViews = getEventsViews(foundEvents);

        List<EventShortDto> eventShortDtos = foundEvents.stream()
                .map(EventMapper::mapToShortDto)
                .collect(Collectors.toList());

        setViewsToEvents(eventShortDtos, eventsViews);
        //TODO add confirmed requests

        return eventShortDtos;
    }

    @Override
    public EventFullDto findUserEventById(Long userId, Long eventId) {
        checkIfUserExist(userId);
        Event foundEvent = eventRepository.getByInitiatorIdAndEventId(userId, eventId).orElseThrow(
                () -> new NotExistsException(
                        "Event",
                        String.format("Event with id %d and initiator id %d not exists", eventId, userId)
                ));

        Map<Long, Long> eventViews = getEventsViews(List.of(foundEvent));

        EventFullDto eventFullDto = EventMapper.mapToFullDto(foundEvent);

        setViewsToEvents(List.of(eventFullDto), eventViews);

        //TODO add confirmed requests
        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        Event eventToUpdate = eventRepository.getByInitiatorIdAndEventId(userId, eventId).orElseThrow(
                () -> new NotExistsException(
                        "Event",
                        String.format("Event with id %d and initiator id %d not exists", eventId, userId)
                )
        );
        checkIfAvailableForUpdate(eventToUpdate);

        updateEventFields(eventToUpdate, updateEventUserRequest);
        eventRepository.updateEvent(eventToUpdate);

        Map<Long, Long> eventViews = getEventsViews(List.of(eventToUpdate));

        EventFullDto eventFullDto = EventMapper.mapToFullDto(eventToUpdate);

        setViewsToEvents(List.of(eventFullDto), eventViews);

        //TODO add confirmed requests
        return eventFullDto;
    }

    private void checkEventDate(LocalDateTime startTime) {
        long minimumIntervalBeforeEventStartInHours = 2L;
        if (startTime.isBefore(LocalDateTime.now().plusHours(minimumIntervalBeforeEventStartInHours))) {
            throw new InvalidParamException(
                    "Event date",
                    String.format("Event date should be more than %d hours from now",
                            minimumIntervalBeforeEventStartInHours)
            );
        }
    }

    private User getUserFromDb(Long userId) {
        return userRepository.getUserById(userId).orElseThrow(
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
        return categoryRepository.getCategoryById(categoryId).orElseThrow(
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

    private void checkIfAvailableForUpdate(Event eventToUpdate) {
        if (eventToUpdate.getState().equals(EventState.PUBLISHED)) {
            throw new ForbiddenException(
                    "Forbidden",
                    String.format("Not allowed to update event with status %s", eventToUpdate.getState())
            );
        }
    }

    private void updateEventFields(Event eventToUpdate, UpdateEventUserRequest updateRequest) {
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
            checkEventDate(updateRequest.getEventDate());
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
        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case CANCEL_REVIEW:
                    eventToUpdate.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    eventToUpdate.setState(EventState.PENDING);
                    break;
            }
        }
    }

}
