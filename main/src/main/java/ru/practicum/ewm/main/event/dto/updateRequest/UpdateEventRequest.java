package ru.practicum.ewm.main.event.dto.updateRequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Length;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.validator.NullOrNotBlank;

import javax.validation.constraints.Future;
import java.time.LocalDateTime;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class UpdateEventRequest {
    @NullOrNotBlank
    @Length(min = 3, max = 120)
    private String title;

    @NullOrNotBlank
    @Length(min = 20, max = 2000)
    private String annotation;

    @NullOrNotBlank
    @Length(min = 20, max = 7000)
    private String description;

    private Integer category;

    @Future
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private Integer participantLimit;
    private Boolean paid;
    private Location location;
    private Boolean requestModeration;
}
