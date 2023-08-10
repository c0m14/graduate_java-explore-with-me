package ru.practicum.ewm.main.category.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    private int id;
    private String name;

    public Category(String name) {
        this.name = name;
    }

    public Map<String, Object> mapToDb() {
        Map<String, Object> categoryFields = new HashMap<>();

        categoryFields.put("category_id", id);
        categoryFields.put("category_name", name);

        return categoryFields;
    }
}
