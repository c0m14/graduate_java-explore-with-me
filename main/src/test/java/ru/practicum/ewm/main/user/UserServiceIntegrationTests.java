package ru.practicum.ewm.main.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.ewm.main.user.dto.NewUserRequestDto;
import ru.practicum.ewm.main.user.dto.UserDto;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserServiceIntegrationTests {

    private final String host = "http://localhost:";
    @Autowired
    private TestRestTemplate restTemplate;
    @Value(value = "${local.server.port}")
    private String port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void addUser() {
        URI uri = URI.create(host + port + "/admin/users");
        NewUserRequestDto newUser = new NewUserRequestDto();
        newUser.setName("name");
        newUser.setEmail("email@email.com");

        UserDto returnedUser = restTemplate.postForEntity(uri, newUser, UserDto.class).getBody();

        User savedUser = jdbcTemplate.query("select user_id, user_name, email " +
                        "from users where user_id = ?", this::mapRowToUser, returnedUser.getId())
                .get(0);
        assertThat(returnedUser.getName(), equalTo(newUser.getName()));
        assertThat(returnedUser.getEmail(), equalTo(newUser.getEmail()));
        assertThat(savedUser.getName(), equalTo(newUser.getName()));
        assertThat(savedUser.getEmail(), equalTo(newUser.getEmail()));
    }

    @Test
    void getUsers() {
        User firstUser = new User("name1", "1@email.ru");
        User secondUser = new User("name2", "2@email.ru");
        Long firstUserId = userRepository.save(firstUser).getId();
        userRepository.save(secondUser);
        Map<String, Object> queryParams = Map.of(
                "ids", String.valueOf(firstUserId),
                "from", "0",
                "size", "2"
        );

        List<UserDto> foundUsers = restTemplate.exchange(
                host + port + "/admin/users" + "?ids={ids}&from={from}&size={size}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<UserDto>>() {
                },
                queryParams
        ).getBody();

        assertThat(foundUsers.size(), equalTo(1));
        assertThat(foundUsers.get(0).getName(), equalTo(firstUser.getName()));
        assertThat(foundUsers.get(0).getEmail(), equalTo(firstUser.getEmail()));
    }

    @Test
    void deleteUser() {
        User user = new User("name", "email@email.ru");
        Long userId = userRepository.save(user).getId();

        restTemplate.exchange(
                host + port + "/admin/users/" + userId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        List<User> foundUsers = jdbcTemplate.query("select user_id, user_name, email " +
                "from users " +
                "where user_id = ?", this::mapRowToUser, userId);
        assertThat(foundUsers.size(), equalTo(0));
    }

    private User mapRowToUser(ResultSet resultSet, int rowNum) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("user_id"));
        user.setName(resultSet.getString("user_name"));
        user.setEmail(resultSet.getString("email"));

        return user;
    }
}