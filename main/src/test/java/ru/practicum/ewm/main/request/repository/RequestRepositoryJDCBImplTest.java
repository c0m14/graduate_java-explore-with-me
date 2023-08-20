package ru.practicum.ewm.main.request.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.request.model.EventParticipationRequest;
import ru.practicum.ewm.main.request.model.RequestStatus;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;
import ru.practicum.ewm.statistic.dto.Formats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RequestRepositoryJDCBImplTest {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_PATTERN);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("delete from event_participation_request");
    }

    @Test
    void save_whenInvoked_thenRequestSavedToDb() {
        User requester = TestDataProvider.getValidUserToSave();
        User savedRequester = userRepository.save(requester);
        User eventOwner = TestDataProvider.getValidUserToSave();
        User savedEventOwner = userRepository.save(eventOwner);
        Category category = TestDataProvider.getValidCategoryToSave();
        Category savedCategory = categoryRepository.save(category);
        Event event = TestDataProvider.getValidNotSavedEvent(savedEventOwner, savedCategory);
        Event savedEvent = eventRepository.save(event);
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(savedRequester, savedEvent);

        EventParticipationRequest savedRequest = requestRepository.save(request);

        EventParticipationRequest foundRequest = jdbcTemplate.queryForObject(
                "select r.request_id, r.created, r.event_id, r.requester_id, r.request_status " +
                        "from event_participation_request r " +
                        "where r.request_id = ?", this::mapRowToRequestShort, savedRequest.getId()
        );
        assertThat(foundRequest.getRequester().getId(), equalTo(savedRequester.getId()));
        assertThat(foundRequest.getEvent().getId(), equalTo(savedEvent.getId()));
    }

    @Test
    void save_whenSaveWithSameRequesterAndEvent_thenForbiddenExceptionThrown() {
        User requester = TestDataProvider.getValidUserToSave();
        User savedRequester = userRepository.save(requester);
        User eventOwner = TestDataProvider.getValidUserToSave();
        User savedEventOwner = userRepository.save(eventOwner);
        Category category = TestDataProvider.getValidCategoryToSave();
        Category savedCategory = categoryRepository.save(category);
        Event event = TestDataProvider.getValidNotSavedEvent(savedEventOwner, savedCategory);
        Event savedEvent = eventRepository.save(event);
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(savedRequester, savedEvent);
        EventParticipationRequest savedRequest = requestRepository.save(request);

        Executable executable = () -> requestRepository.save(request);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void findByUserId_whenNoRequestFound_thenEmptyListReturned() {
        Long userId = 0L;

        List<EventParticipationRequest> foundRequests = requestRepository.findByUserId(userId);

        assertTrue(foundRequests.isEmpty());
    }

    @Test
    void findByUserId_whenInvoked_thenFoundByUserId() {
        User savedRequester = userRepository.save(TestDataProvider.getValidUserToSave());
        User savedOtherUser = userRepository.save(TestDataProvider.getValidUserToSave());
        User savedEventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category savedCategory = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event savedEvent = eventRepository.save(TestDataProvider.getValidNotSavedEvent(savedEventOwner, savedCategory));
        EventParticipationRequest request1 = TestDataProvider.getValidRequestToSave(savedRequester, savedEvent);
        EventParticipationRequest request2 = TestDataProvider.getValidRequestToSave(savedOtherUser, savedEvent);
        EventParticipationRequest savedRequest1 = requestRepository.save(request1);
        EventParticipationRequest savedRequest2 = requestRepository.save(request2);

        List<EventParticipationRequest> foundRequests = requestRepository.findByUserId(savedRequester.getId());

        assertThat(foundRequests.size(), equalTo(1));
        assertThat(foundRequests.get(0).getId(), equalTo(savedRequest1.getId()));
    }

    @Test
    void update_whenInvoked_thenStatusUpdatedInDb() {
        User savedRequester = userRepository.save(TestDataProvider.getValidUserToSave());
        User savedEventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category savedCategory = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event savedEvent = eventRepository.save(TestDataProvider.getValidNotSavedEvent(savedEventOwner, savedCategory));
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(savedRequester, savedEvent);
        request.setRequestStatus(RequestStatus.PENDING);
        EventParticipationRequest savedRequest = requestRepository.save(request);
        EventParticipationRequest newRequest = TestDataProvider.getValidRequestToSave(savedRequester, savedEvent);
        newRequest.setRequestStatus(RequestStatus.CANCELED);
        newRequest.setId(request.getId());

        EventParticipationRequest returnedRequest = requestRepository.update(newRequest);

        EventParticipationRequest updatedRequest = jdbcTemplate.queryForObject(
                "select request_id, created, event_id, requester_id, request_status " +
                        "from event_participation_request " +
                        "where requester_id = ? " +
                        "and request_id = ?", this::mapRowToRequestShort, savedRequester.getId(), savedRequest.getId());
        assertThat(updatedRequest.getRequestStatus(), equalTo(RequestStatus.CANCELED));
        assertThat(returnedRequest.getRequestStatus(), equalTo(RequestStatus.CANCELED));
    }

    @Test
    void findRequestsForEvent_whenNoRequest_whenEmptyListReturned() {
        Long eventId = 2L;

        List<EventParticipationRequest> foundRequests = requestRepository.findRequestsForEvent(eventId);

        assertTrue(foundRequests.isEmpty());
    }

    @Test
    void findRequestsForEvent_whenInvoked_thenRequestsFound() {
        User savedRequester = userRepository.save(TestDataProvider.getValidUserToSave());
        User savedEventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category savedCategory = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event savedEvent = eventRepository.save(TestDataProvider.getValidNotSavedEvent(savedEventOwner, savedCategory));
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(savedRequester, savedEvent);
        EventParticipationRequest savedRequest = requestRepository.save(request);

        List<EventParticipationRequest> foundRequests =
                requestRepository.findRequestsForEvent(savedEvent.getId());

        assertThat(foundRequests.size(), equalTo(1));
        assertThat(foundRequests.get(0).getId(), equalTo(savedRequest.getId()));
        assertThat(foundRequests.get(0).getEvent().getId(), equalTo(savedEvent.getId()));
    }

    private EventParticipationRequest mapRowToRequestShort(ResultSet resultSet, int rowNum) throws SQLException {
        return getRequestBuilderWithBaseFields(resultSet, rowNum)
                .event(Event.builder().id(resultSet.getLong("event_id")).build())
                .requester(new User(resultSet.getLong("requester_id")))
                .build();
    }

    private EventParticipationRequest.EventParticipationRequestBuilder getRequestBuilderWithBaseFields(
            ResultSet resultSet, int rowNum) throws SQLException {
        return EventParticipationRequest.builder()
                .id(resultSet.getLong("request_id"))
                .requestStatus(RequestStatus.valueOf(resultSet.getString("request_status")))
                .created(LocalDateTime.parse(resultSet.getString("created"), formatter));
    }
}