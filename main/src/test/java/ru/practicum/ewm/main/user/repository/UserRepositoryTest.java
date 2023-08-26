package ru.practicum.ewm.main.user.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void save_whenEmailExists_thenForbiddenExceptionThrown() {
        User firstUser = new User();
        firstUser.setName("name");
        firstUser.setEmail("email@email.ru");
        User secondUser = new User();
        secondUser.setName("name");
        secondUser.setEmail("email@email.ru");
        userRepository.save(firstUser);

        assertThrows(ForbiddenException.class,
                () -> userRepository.save(secondUser));
    }

    @Test
    void getUsersFilteredByUsersId() {
        User user1 = new User("name1", "1@email.ru");
        User user2 = new User("name2", "2@email.ru");
        User user3 = new User("name3", "3@email.ru");
        Long user1Id = userRepository.save(user1).getId();
        user1.setId(user1Id);
        Long user2Id = userRepository.save(user2).getId();
        user2.setId(user2Id);
        Long user3Id = userRepository.save(user3).getId();
        user3.setId(user3Id);
        List<Long> ids = List.of(user1Id, user2Id);

        List<User> foundUsers = userRepository.findUsers(ids, 0, 10);

        assertThat(foundUsers.size(), equalTo(ids.size()));
        assertTrue(foundUsers.contains(user1));
        assertTrue(foundUsers.contains(user2));
        assertFalse(foundUsers.contains(user3));
    }

    @Test
    void getUsersSortedByUserIdASC() {
        User user1 = new User("name1", "1@email.ru");
        User user2 = new User("name2", "2@email.ru");
        User user3 = new User("name3", "3@email.ru");
        Long user1Id = userRepository.save(user1).getId();
        user1.setId(user1Id);
        Long user2Id = userRepository.save(user2).getId();
        user2.setId(user2Id);
        Long user3Id = userRepository.save(user3).getId();
        user3.setId(user3Id);
        List<Long> ids = List.of(user1Id, user2Id);

        List<User> foundUsers = userRepository.findUsers(ids, 0, 10);

        assertThat(foundUsers.size(), equalTo(ids.size()));
        assertThat(foundUsers.get(0), equalTo(user1));
        assertThat(foundUsers.get(1), equalTo(user2));
    }

    @Test
    void getUsersReturnedWithOffsetLimit() {
        User user1 = new User("name1", "1@email.ru");
        User user2 = new User("name2", "2@email.ru");
        User user3 = new User("name3", "3@email.ru");
        Long user1Id = userRepository.save(user1).getId();
        user1.setId(user1Id);
        Long user2Id = userRepository.save(user2).getId();
        user2.setId(user2Id);
        Long user3Id = userRepository.save(user3).getId();
        user3.setId(user3Id);
        List<Long> ids = List.of();

        List<User> foundUsers = userRepository.findUsers(ids, 1, 10);

        assertThat(foundUsers.size(), equalTo(2));
        assertThat(foundUsers.get(0), equalTo(user2));
        assertThat(foundUsers.get(1), equalTo(user3));
    }

    @Test
    void getUsersReturnedWithSizeLimit() {
        User user1 = new User("name1", "1@email.ru");
        User user2 = new User("name2", "2@email.ru");
        User user3 = new User("name3", "3@email.ru");
        Long user1Id = userRepository.save(user1).getId();
        user1.setId(user1Id);
        Long user2Id = userRepository.save(user2).getId();
        user2.setId(user2Id);
        Long user3Id = userRepository.save(user3).getId();
        user3.setId(user3Id);
        List<Long> ids = List.of();

        List<User> foundUsers = userRepository.findUsers(ids, 2, 1);

        assertThat(foundUsers.size(), equalTo(1));
        assertThat(foundUsers.get(0), equalTo(user3));
    }

    @Test
    void getUsersReturnedAllIfIdsNull() {
        User user1 = new User("name1", "1@email.ru");
        User user2 = new User("name2", "2@email.ru");
        User user3 = new User("name3", "3@email.ru");
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        List<User> foundUsers = userRepository.findUsers(null, 0, 10);

        assertThat(foundUsers.size(), equalTo(3));
    }

    @Test
    void getUsersReturnEmptyListIfNoUsersFound() {
        User user1 = new User("name1", "1@email.ru");
        userRepository.save(user1);
        List<Long> ids = List.of(-1L);

        List<User> foundUsers = userRepository.findUsers(ids, 0, 10);

        assertThat(foundUsers.size(), equalTo(0));
    }

    @Test
    void deleteUserById() {
        User user1 = new User("name1", "1@email.ru");
        Long userId = userRepository.save(user1).getId();

        userRepository.deleteUser(userId);

        List<User> foundUsers = userRepository.findUsers(List.of(userId), 0, 10);
        assertThat(foundUsers.size(), equalTo(0));
    }

    @Test
    void deleteUserNotExistExceptionThrownIfNotFound() {
        assertThrows(NotExistsException.class,
                () -> userRepository.deleteUser(999_999_999L));
    }

    @Test
    void getUserByIdReturnedOptionalWithUserIfFound() {
        User user = new User("name", "email@email.com");

        Long userId = userRepository.save(user).getId();

        Optional<User> foundUserOptional = userRepository.findUserById(userId);

        assertFalse(foundUserOptional.isEmpty());
        assertThat(foundUserOptional.get().getName(), equalTo(user.getName()));
        assertThat(foundUserOptional.get().getEmail(), equalTo(user.getEmail()));
        assertThat(foundUserOptional.get().getId(), equalTo(userId));
    }

    @Test
    void getUserByIdReturnedOptionalEmptyIdNotFound() {
        Long userId = 0L;

        Optional<User> foundUserOptional = userRepository.findUserById(userId);

        assertTrue(foundUserOptional.isEmpty());
    }
}