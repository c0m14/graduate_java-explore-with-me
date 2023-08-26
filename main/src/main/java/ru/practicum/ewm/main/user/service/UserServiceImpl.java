package ru.practicum.ewm.main.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.user.dto.NewUserRequestDto;
import ru.practicum.ewm.main.user.dto.UserDto;
import ru.practicum.ewm.main.user.mapper.UserMapper;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;
import ru.practicum.ewm.main.event.repository.RateDAO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RateDAO rateDAO;

    @Override
    @Transactional
    public UserDto addUser(NewUserRequestDto userRequest) {
        User user = UserMapper.mapToEntity(userRequest);

        User savedUser = userRepository.save(user);

        return UserMapper.mapToUserDto(savedUser);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {

        List<User> foundUsers = userRepository.findUsers(ids, from, size);

        List<UserDto> userDtos = foundUsers.stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());

        fetchRatingsToUsersDtos(userDtos);

        return userDtos;
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteUser(userId);
    }

    private void fetchRatingsToUsersDtos(List<UserDto> userDtos) {
        List<Long> usersIds = userDtos.stream()
                .map(UserDto::getId)
                .collect(Collectors.toList());

        List<Event> usersEvents = eventRepository.findUsersEventsWithoutCategoryAndRequest(usersIds);
        if (usersEvents.isEmpty()) {
            return;
        }

        List<Long> eventsIds = usersEvents.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        Map<Long, Long> eventsRatings = rateDAO.getRatingsForEvents(eventsIds);

        userDtos.forEach(userDto -> {
            AtomicReference<Long> userRating = new AtomicReference<>(0L);
            usersEvents.stream()
                    .filter(event -> event.getInitiator().getId().equals(userDto.getId()))
                    .map(Event::getId)
                    .forEach(id -> {
                        userRating.updateAndGet(v -> v + eventsRatings.get(id));
                    });
            userDto.setRating(userRating.get());
        });

    }
}
