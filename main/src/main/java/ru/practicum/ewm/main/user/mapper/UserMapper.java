package ru.practicum.ewm.main.user.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.user.dto.NewUserRequest;
import ru.practicum.ewm.main.user.dto.UserDto;
import ru.practicum.ewm.main.user.dto.UserShortDto;
import ru.practicum.ewm.main.user.model.User;

@UtilityClass
public class UserMapper {
    public User mapToEntity(NewUserRequest userRequest) {
        return new User(userRequest.getName(), userRequest.getEmail());
    }

    public UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public UserShortDto mapToShortDto(User user) {
        return new UserShortDto(user.getId(), user.getName());
    }
}
