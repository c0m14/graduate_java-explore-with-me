package ru.practicum.ewm.main.user.repository;

import ru.practicum.ewm.main.user.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    List<User> getUsers(List<Long> ids, int offset, int size);

    void deleteUser(Long userId);

    Optional<User> getUserById(Long userId);
}
