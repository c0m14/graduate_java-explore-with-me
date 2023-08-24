package ru.practicum.ewm.main.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.dto.NewEventDto;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.user.dto.UserShortDto;
import ru.practicum.ewm.main.user.model.User;

@UtilityClass
public class EventMapper {

    public Event mapToEntity(NewEventDto newEventDto, User initiator, Category category) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(newEventDto.getLocation())
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .title(newEventDto.getTitle())
                .initiator(initiator)
                .requestModeration(newEventDto.getRequestModeration())
                .build();
    }

    public EventFullDto mapToFullDto(
            Event event,
            CategoryDto categoryDto,
            UserShortDto initiatorDto,
            Long views,
            int confirmedRequests) {
        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .category(categoryDto)
                .eventDate(event.getEventDate())
                .initiator(initiatorDto)
                .paid(event.isPaid())
                .location(event.getLocation())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.isRequestModeration())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .state(event.getState())
                .views(views)
                .confirmedRequests(confirmedRequests)
                .build();
    }

    public EventFullDto mapToFullDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .category(new CategoryDto(
                        event.getCategory().getId(),
                        event.getCategory().getName()))
                .eventDate(event.getEventDate())
                .initiator(new UserShortDto(
                        event.getInitiator().getId(),
                        event.getInitiator().getName()))
                .paid(event.isPaid())
                .location(event.getLocation())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.isRequestModeration())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .state(event.getState())
                .confirmedRequests(event.getConfirmedRequests())
                .build();
    }

    public EventShortDto mapToShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(new CategoryDto(
                        event.getCategory().getId(),
                        event.getCategory().getName()))
                .eventDate(event.getEventDate())
                .initiator(new UserShortDto(
                        event.getInitiator().getId(),
                        event.getInitiator().getName()))
                .paid(event.isPaid())
                .build();
        //TODO confirmedRequests
    }
}
