package ru.practicum.ewm.main.event.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class EventRepositoryTests {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("delete from event");
    }

    @Test
    void getUsersEvents_whenInvoked_thenAllEventsBelongsToUser() {
        int offset = 0;
        int size = 10;
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        User otherUser = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event1ToSave = TestDataProvider.getValidNotSavedEvent(owner, category);
        Long event1Id = eventRepository.save(event1ToSave).getId();
        Event event2ToSave = TestDataProvider.getValidNotSavedEvent(otherUser, category);
        eventRepository.save(event2ToSave);

        List<Event> foundEvents = eventRepository.getUsersEvents(owner.getId(), offset, size);

        assertThat(foundEvents.size(), equalTo(1));
        assertThat(foundEvents.get(0).getId(), equalTo(event1Id));
        assertThat(foundEvents.get(0).getInitiator().getId(), equalTo(owner.getId()));
    }

    @Test
    void getUsersEvents_whenEventsNotFound_thenEmptyListReturned() {
        int offset = 0;
        int size = 10;
        Long userId = 0L;

        List<Event> foundEvents = eventRepository.getUsersEvents(userId, offset, size);

        assertTrue(foundEvents.isEmpty());
    }

    @Test
    void getUsersEvents_whenInvoked_thenCategoryAddToEvent() {
        int offset = 0;
        int size = 10;
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event1ToSave = TestDataProvider.getValidNotSavedEvent(owner, category);
        eventRepository.save(event1ToSave);

        List<Event> foundEvents = eventRepository.getUsersEvents(owner.getId(), offset, size);

        assertThat(foundEvents.get(0).getCategory(), equalTo(category));
    }

    @Test
    void getUsersEvents_whenInvoked_thenInitiatorAddToEvent() {
        int offset = 0;
        int size = 10;
        User initiator = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event1ToSave = TestDataProvider.getValidNotSavedEvent(initiator, category);
        eventRepository.save(event1ToSave);

        List<Event> foundEvents = eventRepository.getUsersEvents(initiator.getId(), offset, size);

        assertThat(foundEvents.get(0).getInitiator().getId(), equalTo(initiator.getId()));
        assertThat(foundEvents.get(0).getInitiator().getName(), equalTo(initiator.getName()));
    }

    @Test
    void getUsersEvents_whenInvoked_thenFoundEventsOrderedByEventDateDESC() {
        int offset = 0;
        int size = 10;
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event1ToSave = TestDataProvider.getValidNotSavedEvent(owner, category);
        event1ToSave.setEventDate(LocalDateTime.now().plusDays(2).withNano(0));
        eventRepository.save(event1ToSave);
        Event event2ToSave = TestDataProvider.getValidNotSavedEvent(owner, category);
        event2ToSave.setEventDate(LocalDateTime.now().plusDays(1).withNano(0));
        eventRepository.save(event2ToSave);

        List<Event> foundEvents = eventRepository.getUsersEvents(owner.getId(), offset, size);

        assertThat(foundEvents.get(0).getId(), equalTo(event1ToSave.getId()));
        assertThat(foundEvents.get(1).getId(), equalTo(event2ToSave.getId()));
    }

    @Test
    void getUsersEvents_whenInvoked_thenResultListLimitedByOffset() {
        int offset = 1;
        int size = 10;
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event1ToSave = TestDataProvider.getValidNotSavedEvent(owner, category);
        event1ToSave.setEventDate(LocalDateTime.now().plusDays(2).withNano(0));
        eventRepository.save(event1ToSave);
        Event event2ToSave = TestDataProvider.getValidNotSavedEvent(owner, category);
        event2ToSave.setEventDate(LocalDateTime.now().plusDays(1).withNano(0));
        eventRepository.save(event2ToSave);

        List<Event> foundEvents = eventRepository.getUsersEvents(owner.getId(), offset, size);

        assertThat(foundEvents.size(), equalTo(1));
        assertThat(foundEvents.get(0).getId(), equalTo(event2ToSave.getId()));
    }

    @Test
    void getUsersEvents_whenInvoked_thenResultListLimitedBySize() {
        int offset = 0;
        int size = 1;
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event1ToSave = TestDataProvider.getValidNotSavedEvent(owner, category);
        event1ToSave.setEventDate(LocalDateTime.now().plusDays(2).withNano(0));
        eventRepository.save(event1ToSave);
        Event event2ToSave = TestDataProvider.getValidNotSavedEvent(owner, category);
        event2ToSave.setEventDate(LocalDateTime.now().plusDays(1).withNano(0));
        eventRepository.save(event2ToSave);

        List<Event> foundEvents = eventRepository.getUsersEvents(owner.getId(), offset, size);

        assertThat(foundEvents.size(), equalTo(1));
        assertThat(foundEvents.get(0).getId(), equalTo(event1ToSave.getId()));
    }

    @Test
    void getByInitiatorIdAndEventId_whenInvoked_thenEventWithPassedUserAndEventIdsReturned() {
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(owner, category));

        Optional<Event> foundEvent = eventRepository.getByInitiatorIdAndEventId(owner.getId(), event.getId());

        assertThat(foundEvent.get().getId(), equalTo(event.getId()));
        assertThat(foundEvent.get().getInitiator().getId(), equalTo(owner.getId()));
    }

    @Test
    void getByInitiatorIdAndEventId_whenUserNotOwner_thenEventNotReturned() {
        User notOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(owner, category));

        Optional<Event> foundEvent = eventRepository.getByInitiatorIdAndEventId(notOwner.getId(), event.getId());

        assertTrue(foundEvent.isEmpty());
    }

    @Test
    void getByInitiatorIdAndEventId_whenEventFound_thenReturnedWithCategory() {
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(owner, category));

        Optional<Event> foundEvent = eventRepository.getByInitiatorIdAndEventId(owner.getId(), event.getId());

        assertThat(foundEvent.get().getCategory(), equalTo(category));
    }

    @Test
    void getByInitiatorIdAndEventId_whenEventFound_thenReturnedWithInitiator() {
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(owner, category));

        Optional<Event> foundEvent = eventRepository.getByInitiatorIdAndEventId(owner.getId(), event.getId());

        assertThat(foundEvent.get().getInitiator().getId(), equalTo(owner.getId()));
        assertThat(foundEvent.get().getInitiator().getName(), equalTo(owner.getName()));
    }

    @Test
    void updateEvent_updateAllEventFieldsThatAvailableByUpdateRequest() {
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        User owner = userRepository.save(TestDataProvider.getValidUserToSave());
        Event oldEvent = eventRepository.save(TestDataProvider.getValidNotSavedEvent(owner, category));
        Category newCategory = categoryRepository.save(new Category("newName"));
        Event newEvent = Event.builder()
                .title("newTitle")
                .annotation("newAnnotation".repeat(3))
                .description("newDescription".repeat(3))
                .category(newCategory)
                .eventDate(LocalDateTime.now().plusDays(5).withNano(0))
                .paid(false)
                .location(new Location(444.444f, 777.888f))
                .requestModeration(false)
                .state(EventState.CANCELED)
                .participantLimit(100)
                //Неизменяемые поля
                .id(oldEvent.getId())
                .createdOn(oldEvent.getCreatedOn())
                .initiator(oldEvent.getInitiator())
                .publishedOn(oldEvent.getPublishedOn())
                .build();

        eventRepository.updateEvent(newEvent);

        Event savedEvent = jdbcTemplate.query("SELECT e.event_id, e.title, e.annotation, e.description, e.category_id, e.event_date, " +
                "e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, e.request_moderation, e.created_on, " +
                "e.published_on, e.state, " +
                "c.category_id, c.category_name, " +
                "u.user_id, u.user_name " +
                "FROM event AS e " +
                "INNER JOIN category AS c ON e.category_id = c.category_id " +
                "INNER JOIN users AS u ON e.initiator_id = u.user_id " +
                "WHERE e.event_id = ? " +
                "AND e.initiator_id = ?", this::mapRowToEventFull, oldEvent.getId(), owner.getId()).get(0);
        assertThat(savedEvent.getId(), equalTo(newEvent.getId()));
        assertThat(savedEvent.getInitiator().getId(), equalTo(newEvent.getInitiator().getId()));
        assertThat(savedEvent.getTitle(), equalTo(newEvent.getTitle()));
        assertThat(savedEvent.getAnnotation(), equalTo(newEvent.getAnnotation()));
        assertThat(savedEvent.getDescription(), equalTo(newEvent.getDescription()));
        assertThat(savedEvent.getCategory(), equalTo(newEvent.getCategory()));
        assertThat(savedEvent.getEventDate(), equalTo(newEvent.getEventDate()));
        assertThat(savedEvent.isPaid(), equalTo(newEvent.isPaid()));
        assertThat(savedEvent.isRequestModeration(), equalTo(newEvent.isRequestModeration()));
        assertThat(savedEvent.getState(), equalTo(newEvent.getState()));
        assertThat(savedEvent.getParticipantLimit(), equalTo(newEvent.getParticipantLimit()));
    }

    private Event mapRowToEventShort(ResultSet resultSet, int rowNum) throws SQLException {
        return getEventBuilderWithBaseFields(resultSet, rowNum).build();
    }

    private Event mapRowToEventFull(ResultSet resultSet, int rowNum) throws SQLException {
        return getEventBuilderWithBaseFields(resultSet, rowNum)
                .location(new Location(
                        resultSet.getFloat("latitude"),
                        resultSet.getFloat("longitude")
                ))
                .state(EventState.valueOf(resultSet.getString("state")))
                .build();
    }

    private Event.EventBuilder getEventBuilderWithBaseFields(ResultSet resultSet, int rowNum) throws SQLException {
        return Event.builder()
                .id(resultSet.getLong("event_id"))
                .title(resultSet.getString("title"))
                .annotation(resultSet.getString("annotation"))
                .description(resultSet.getString("description"))
                .category(new Category(
                        resultSet.getInt("category_id"),
                        resultSet.getString("category_name")))
                .eventDate(LocalDateTime.parse(resultSet.getString("event_date"), formatter))
                .initiator(new User(
                        resultSet.getLong("user_id"),
                        resultSet.getString("user_name")))
                .paid(resultSet.getBoolean("paid"))
                .location(null)
                .participantLimit(resultSet.getInt("participant_limit"))
                .requestModeration(resultSet.getBoolean("request_moderation"))
                .createdOn(LocalDateTime.parse(resultSet.getString("created_on"), formatter))
                .publishedOn(resultSet.getString("published_on") == null ?
                        null : LocalDateTime.parse(resultSet.getString("published_on")))
                .state(null);
    }

}
