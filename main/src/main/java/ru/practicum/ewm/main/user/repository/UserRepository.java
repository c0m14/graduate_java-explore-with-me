package ru.practicum.ewm.main.user.repository;

import ru.practicum.ewm.main.user.model.User;

import java.util.List;

public interface UserRepository {
    User save(User user);

    List<User> getUsers(List<Long> ids, int offset, int size);

    void deleteUser(Long userId);
}
