package ru.practicum.ewm.main.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.event.repository.RateRepository;
import ru.practicum.ewm.main.user.dto.UserDto;
import ru.practicum.ewm.main.user.mapper.UserMapper;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private RateRepository rateDAO;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getUsers_RatingFound_thenAddedToDto() {
        Long userId = 1L;
        Long event1Id = 2L;
        Long event2Id = 3L;
        User user = TestDataProvider.getValidUserToSave();
        user.setId(userId);
        Event event1 = TestDataProvider.getValidNotSavedEvent(user, new Category());
        Event event2 = TestDataProvider.getValidNotSavedEvent(user, new Category());
        event1.setId(event1Id);
        event2.setId(event2Id);
        when(userRepository.findUsers(List.of(userId), 0, 10))
                .thenReturn(List.of(user));
        try (MockedStatic<UserMapper> userMapperMock = Mockito.mockStatic(UserMapper.class)) {
            userMapperMock.when(() -> UserMapper.mapToUserDto((user)))
                    .thenReturn(TestDataProvider.getValidUserDto(userId));
        }
        when(eventRepository.findUsersEventsWithoutCategoryAndRequest(List.of(userId)))
                .thenReturn(List.of(event1, event2));
        when(rateDAO.getRatingsForEvents(List.of(event1Id, event2Id)))
                .thenReturn(Map.of(
                        event1Id, 10L,
                        event2Id, -5L));

        List<UserDto> foundUsers = userService.getUsers(List.of(userId), 0, 10);

        assertThat(foundUsers.get(0).getRating(), equalTo(5L));
    }

    @Test
    void getUsers_UserHaveNoEvents_thenRatingIsNull() {
        Long userId = 1L;
        User user = TestDataProvider.getValidUserToSave();
        user.setId(userId);
        when(userRepository.findUsers(List.of(userId), 0, 10))
                .thenReturn(List.of(user));
        try (MockedStatic<UserMapper> userMapperMock = Mockito.mockStatic(UserMapper.class)) {
            userMapperMock.when(() -> UserMapper.mapToUserDto((user)))
                    .thenReturn(TestDataProvider.getValidUserDto(userId));
        }
        when(eventRepository.findUsersEventsWithoutCategoryAndRequest(List.of(userId)))
                .thenReturn(List.of());

        List<UserDto> foundUsers = userService.getUsers(List.of(userId), 0, 10);

        assertNull(foundUsers.get(0).getRating());
    }
}