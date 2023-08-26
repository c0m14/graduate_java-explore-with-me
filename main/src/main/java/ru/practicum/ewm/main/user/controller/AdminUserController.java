package ru.practicum.ewm.main.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.user.dto.NewUserRequestDto;
import ru.practicum.ewm.main.user.dto.UserDto;
import ru.practicum.ewm.main.user.service.UserService;
import ru.practicum.ewm.main.validator.OnCreateValidation;
import ru.practicum.ewm.main.validator.OnUpdateValidation;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class AdminUserController {
    private final UserService userService;

    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Validated(OnCreateValidation.class)
    public UserDto addUser(@Valid @RequestBody NewUserRequestDto userRequest) {
        log.info("Start POST /admin/users with {}", userRequest);
        UserDto savedUser = userService.addUser(userRequest);
        log.info("End POST /admin/users with {}", savedUser);
        return savedUser;
    }

    @GetMapping("/admin/users")
    @Validated(OnUpdateValidation.class)
    public List<UserDto> getUsers(
            @RequestParam(name = "ids", required = false) List<Long> ids,
            @PositiveOrZero @RequestParam(name = "from", required = false, defaultValue = "0") int from,
            @Positive @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        log.info("Start GET /admin/users with ids: {}, from: {}, size: {}", ids, from, size);
        List<UserDto> users = userService.getUsers(ids, from, size);
        log.info("End GET /admin/users with {}", users);
        return users;
    }

    @DeleteMapping("/admin/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable(name = "userId") Long userId) {
        log.info("Start DELETE /admin/users/{userId} with userId: {}", userId);
        userService.deleteUser(userId);
        log.info("Finish DELETE /admin/users/{userId} with userId: {}", userId);
    }
}
