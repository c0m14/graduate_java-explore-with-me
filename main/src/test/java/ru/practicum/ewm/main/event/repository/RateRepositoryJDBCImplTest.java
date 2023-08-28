package ru.practicum.ewm.main.event.repository;

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
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RateRepositoryJDBCImplTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private RateRepository rateDAO;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("delete from event");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from category");
        jdbcTemplate.update("delete from user_event_rate");
    }

    @Test
    void addRate_whenInvoked_thenRateAddToDb() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));

        rateDAO.addRate(rater.getId(), event.getId(), 1);

        Long rate = jdbcTemplate.queryForObject("select rate from user_event_rate " +
                "where event_id = ? AND user_id = ?", Long.class, event.getId(), rater.getId());
        assertThat(rate, equalTo(1L));
    }

    @Test
    void addRate_whenAddRateWithSameUserAndEvent_thenForbiddenExceptionThrown() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        rateDAO.addRate(rater.getId(), event.getId(), 1);

        Executable executable = () -> rateDAO.addRate(rater.getId(), event.getId(), -1);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void getRateInfoForEvent_whenInvoked_thenSumOfRatingsReturned() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater1 = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater2 = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater3 = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        rateDAO.addRate(rater1.getId(), event.getId(), 1);
        rateDAO.addRate(rater2.getId(), event.getId(), 1);
        rateDAO.addRate(rater3.getId(), event.getId(), -1);

        Long rate = rateDAO.getRatingForEvent(event.getId());

        assertThat(rate, equalTo(1L));
    }

    @Test
    void getRateInfoForEvent_whenNoRating_thenZeroReturned() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));

        Long rate = rateDAO.getRatingForEvent(event.getId());

        assertThat(rate, equalTo(0L));
    }

    @Test
    void getRateInfoForEvents_whenInvoked_thenMapOfEventIdsWithRatingReturned() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater1 = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater2 = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater3 = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event1 = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        Event event2 = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        Event event3 = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        rateDAO.addRate(rater1.getId(), event1.getId(), 1);
        rateDAO.addRate(rater2.getId(), event1.getId(), 1);
        rateDAO.addRate(rater3.getId(), event3.getId(), -1);

        Map<Long, Long> rates = rateDAO.getRatingsForEvents(List.of(event1.getId(), event2.getId(), event3.getId()));

        assertThat(rates.size(), equalTo(2));
        assertThat(rates.get(event1.getId()), equalTo(2L));
        assertThat(rates.get(event3.getId()), equalTo(-1L));
        assertNull(rates.get(event3));
    }

    @Test
    void deleteRate_whenInvoked_thenLikeDeleteFromDB() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        rateDAO.addRate(rater.getId(), event.getId(), 1);

        rateDAO.deleteRate(rater.getId(), event.getId(), 1);

        Long rating = jdbcTemplate.queryForObject("select SUM(rate) from user_event_rate " +
                "where event_id = ?", Long.class, event.getId());
        assertNull(rating);
    }

    @Test
    void deleteRate_whenInvoked_thenDislikeDeleteFromDB() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        rateDAO.addRate(rater.getId(), event.getId(), -1);

        rateDAO.deleteRate(rater.getId(), event.getId(), -1);

        Long rating = jdbcTemplate.queryForObject("select SUM(rate) from user_event_rate " +
                "where event_id = ?", Long.class, event.getId());
        assertNull(rating);
    }

    @Test
    void deleteRate_whenRateNotFound_thenNotExistsExceptionThrown() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        User rater = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));

        Executable executable = () -> rateDAO.deleteRate(rater.getId(), event.getId(), -1);

        assertThrows(NotExistsException.class, executable);
    }
}