package ru.practicum.ewm.main;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.main.compilation.model.Compilation;
import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.dto.NewEventDto;
import ru.practicum.ewm.main.event.dto.updaterequest.AdminRequestStateActionDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventAdminRequestDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventUserRequestDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UserRequestStateActionDto;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.request.model.EventParticipationRequest;
import ru.practicum.ewm.main.request.model.RequestStatus;
import ru.practicum.ewm.main.user.dto.UserDto;
import ru.practicum.ewm.main.user.dto.UserShortDto;
import ru.practicum.ewm.main.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;

@UtilityClass
public class TestDataProvider {

    Random rnd = new Random();

    public NewEventDto getValidNewEventDto() {
        return NewEventDto.builder()
                .title("title")
                .annotation("annotation".repeat(10))
                .description("description".repeat(10))
                .category(1)
                .eventDate(LocalDateTime.now().plusHours(9L))
                .paid(false)
                .location(new Location(1.11f, 2.22f))
                .participantLimit(10)
                .requestModeration(true)
                .build();
    }

    public NewEventDto getNewEventDtoWithoutParticipantLimit() {
        return NewEventDto.builder()
                .title("title")
                .annotation("annotation".repeat(10))
                .description("description".repeat(10))
                .category(1)
                .eventDate(LocalDateTime.now().plusHours(9L))
                .paid(false)
                .location(new Location(1.11f, 2.22f))
                .requestModeration(true)
                .build();
    }

    public String getValidNewEventDtoWithoutRequestModeration() {
        return "{\"annotation\":\"Enim dolor quod ea eos nobis architecto quas accusantium. Sunt porro eum iste nam sapiente sit maxime consectetur. Est in est praesentium.\"," +
                "\"category\":31," +
                "\"description\":\"Consequuntur recusandae ipsa cupiditate sed. Quisquam fugit dolor voluptas ipsa in blanditiis consequatur voluptates incidunt. In perferendis est iusto reiciendis placeat doloribus saepe. Suscipit ullam facilis aliquid est et facilis consequuntur quasi. Sit repellat velit consectetur hic.\\n \\rEt soluta labore assumenda quaerat omnis amet dolor labore. Non delectus voluptatem dolorum ea. Eveniet temporibus ratione minus aut dolores maiores veniam incidunt. Aut facilis id quia culpa doloremque minus quis. Qui velit quaerat aut vel et numquam est in.\\n \\rBlanditiis et explicabo beatae numquam ut doloribus veniam voluptas. Labore laudantium totam voluptates reiciendis quibusdam distinctio nam at eum. Ipsa quas ut debitis est vitae quam. Iure perferendis error modi magnam.\"," +
                "\"eventDate\":\"2123-08-13 01:51:52\"," +
                "\"location\":{\"lat\":-21.8403,\"lon\":87.6001}," +
                "\"paid\":\"true\"," +
                "\"participantLimit\":\"239\"," +
                "\"title\":\"Illum quibusdam sed est.\"}";
    }

    public String getValidNewEventDtoWithoutPaid() {
        return "{\"annotation\":\"Enim dolor quod ea eos nobis architecto quas accusantium. Sunt porro eum iste nam sapiente sit maxime consectetur. Est in est praesentium.\"," +
                "\"category\":31," +
                "\"description\":\"Consequuntur recusandae ipsa cupiditate sed. Quisquam fugit dolor voluptas ipsa in blanditiis consequatur voluptates incidunt. In perferendis est iusto reiciendis placeat doloribus saepe. Suscipit ullam facilis aliquid est et facilis consequuntur quasi. Sit repellat velit consectetur hic.\\n \\rEt soluta labore assumenda quaerat omnis amet dolor labore. Non delectus voluptatem dolorum ea. Eveniet temporibus ratione minus aut dolores maiores veniam incidunt. Aut facilis id quia culpa doloremque minus quis. Qui velit quaerat aut vel et numquam est in.\\n \\rBlanditiis et explicabo beatae numquam ut doloribus veniam voluptas. Labore laudantium totam voluptates reiciendis quibusdam distinctio nam at eum. Ipsa quas ut debitis est vitae quam. Iure perferendis error modi magnam.\"," +
                "\"eventDate\":\"2123-08-13 01:51:52\"," +
                "\"location\":{\"lat\":-21.8403,\"lon\":87.6001}," +
                "\"requestModeration\":\"false\"," +
                "\"participantLimit\":\"239\"," +
                "\"title\":\"Illum quibusdam sed est.\"}";
    }

    public Event getValidNotSavedEvent(User user, Category category) {
        return Event.builder()
                .title("title")
                .annotation("annotation".repeat(10))
                .description("description".repeat(10))
                .category(category)
                .initiator(user)
                .eventDate(LocalDateTime.now().plusHours(9L).withNano(0))
                .createdOn(LocalDateTime.now().minusHours(2L).withNano(0))
                .paid(false)
                .location(new Location(1.11f, 2.22f))
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING)
                .build();
    }

    public User getValidUserToSave() {
        return new User("name", String.format("%d@email.ru", rnd.nextInt()));
    }

    public Category getValidCategoryToSave() {
        return new Category(String.format("%d category name", rnd.nextInt()));
    }

    public EventShortDto getValidShortDto(Long eventId) {
        return EventShortDto.builder()
                .id(eventId)
                .title("title")
                .annotation("annotation")
                .category(new CategoryDto())
                .eventDate(LocalDateTime.now().plusDays(1L))
                .initiator(new UserShortDto())
                .paid(true)
                .build();
    }

    public UpdateEventUserRequestDto getValidUpdateEventUserRequest() {
        return UpdateEventUserRequestDto.builder()
                .title("title")
                .annotation("annotation".repeat(10))
                .description("description".repeat(10))
                .category(0)
                .eventDate(LocalDateTime.now().plusDays(1).withNano(0))
                .paid(true)
                .location(new Location(123.123f, 124.124f))
                .requestModeration(true)
                .stateAction(UserRequestStateActionDto.SEND_TO_REVIEW)
                .build();
    }

    public EventFullDto getValidFullDto(Long eventId) {
        return EventFullDto.builder()
                .id(eventId)
                .title("title")
                .annotation("annotation")
                .description("description")
                .participantLimit(10)
                .requestModeration(true)
                .category(new CategoryDto())
                .eventDate(LocalDateTime.now().plusDays(1L))
                .createdOn(LocalDateTime.now().withNano(0))
                .publishedOn(LocalDateTime.now().plusHours(1).withNano(0))
                .initiator(new UserShortDto())
                .paid(true)
                .state(EventState.PUBLISHED)
                .build();
    }

    public UpdateEventAdminRequestDto getValidUpdateEventAdminRequest() {
        return UpdateEventAdminRequestDto.builder()
                .title("title")
                .annotation("annotation".repeat(10))
                .description("description".repeat(10))
                .category(0)
                .eventDate(LocalDateTime.now().plusDays(1).withNano(0))
                .paid(true)
                .location(new Location(123.123f, 124.124f))
                .requestModeration(true)
                .stateAction(AdminRequestStateActionDto.PUBLISH_EVENT)
                .build();
    }

    public NewCompilationDto getValidNewCompilationDto() {
        return NewCompilationDto.builder()
                .pinned(true)
                .title("tittle")
                .events(Set.of(0L))
                .build();
    }

    public EventParticipationRequest getValidRequestToSave(User user, Event event) {
        return EventParticipationRequest.builder()
                .requester(user)
                .requestStatus(RequestStatus.CONFIRMED)
                .event(event)
                .created(LocalDateTime.now().withNano(0))
                .build();
    }

    public Compilation getValidCompilationToSave(List<Event> events) {
        return Compilation.builder()
                .title("title")
                .pinned(false)
                .events(events)
                .build();
    }

    public UserDto getValidUserDto(Long id) {
        return UserDto.builder()
                .id(id)
                .email("email@email.ru")
                .name("name")
                .build();
    }

}
