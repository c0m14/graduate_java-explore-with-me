package ru.practicum.ewm.main.compilation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.main.event.model.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Compilation {

    private Long id;
    private boolean pinned;
    private String title;
    private List<Event> events = new ArrayList<>();

    public Map<String, Object> mapToDb() {
        Map<String, Object> compilationFields = new HashMap<>();

        compilationFields.put("pinned", pinned);
        compilationFields.put("title", title);

        return compilationFields;
    }
}
