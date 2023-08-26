package ru.practicum.ewm.main.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;

    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public User(Long id) {
        this.id = id;
    }

    public Map<String, Object> mapToDb() {
        Map<String, Object> userFields = new HashMap<>(5);

        userFields.put("user_id", id);
        userFields.put("user_name", name);
        userFields.put("email", email);

        return userFields;
    }
}
