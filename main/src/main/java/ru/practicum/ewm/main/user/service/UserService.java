package ru.practicum.ewm.main.user.service;

import ru.practicum.ewm.main.user.dto.NewUserRequestDto;
import ru.practicum.ewm.main.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto addUser(NewUserRequestDto userRequest);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);
}
