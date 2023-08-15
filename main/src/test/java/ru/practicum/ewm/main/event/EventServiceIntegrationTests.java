package ru.practicum.ewm.main.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;
import ru.practicum.ewm.main.event.dto.EventFullDto;
import ru.practicum.ewm.main.event.dto.NewEventDto;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EventServiceIntegrationTests {

    private final String host = "http://localhost:";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private TestRestTemplate restTemplate;
    @Value("${local.server.port}")
    private String port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("delete from event");
    }

    @Test
    void addEvent() {
        Long userId = userRepository.save(TestDataProvider.getValidUserToSave()).getId();
        int categoryId = categoryRepository.save(TestDataProvider.getValidCategoryToSave()).getId();
        NewEventDto newEventDto = TestDataProvider.getValidNewEventDto();
        newEventDto.setCategory(categoryId);

        EventFullDto returnedEvent = restTemplate.postForObject(
                URI.create(host + port + "/users/" + userId + "/events"),
                newEventDto,
                EventFullDto.class
        );

        Event savedEvent = jdbcTemplate.queryForObject("select e.event_id, e.title, e.annotation, e.description, " +
                "e.category_id, e.event_date, e.initiator_id, e.paid, e.latitude, e.longitude, e.participant_limit, " +
                "e.request_moderation, e.created_on, e.published_on, e.state, " +
                "c.category_id, c.category_name, " +
                "u.user_id, u.user_name " +
                "from event as e " +
                "inner join category as c on e.category_id = c.category_id " +
                "inner join users as u on e.initiator_id = u.user_id " +
                "where event_id = ?", this::mapRowToEventFull, returnedEvent.getId());
        assertThat(returnedEvent.getInitiator().getId(), equalTo(userId));
        assertThat(returnedEvent.getCategory().getId(), equalTo(categoryId));
        assertThat(returnedEvent.getState(), equalTo(EventState.PENDING));
        assertNotNull(returnedEvent.getCreatedOn());
        assertThat(savedEvent.getId(), equalTo(returnedEvent.getId()));
        assertThat(savedEvent.getInitiator().getId(), equalTo(userId));
        assertThat(savedEvent.getCategory().getId(), equalTo(categoryId));
        assertThat(savedEvent.getState(), equalTo(EventState.PENDING));
        assertNotNull(savedEvent.getCreatedOn());
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
