package ru.practicum.ewm.main.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Length;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.validator.NullOrNotBlank;
import ru.practicum.ewm.main.validator.ValidationMarker;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank(groups = ValidationMarker.OnCreate.class)
    @NullOrNotBlank(groups = ValidationMarker.OnUpdate.class)
    @Length(min = 3, max = 120)
    private String title;

    @NotBlank(groups = ValidationMarker.OnCreate.class)
    @NullOrNotBlank(groups = ValidationMarker.OnUpdate.class)
    @Length(min = 20, max = 2000)
    private String annotation;

    @NotBlank(groups = ValidationMarker.OnCreate.class)
    @NullOrNotBlank(groups = ValidationMarker.OnUpdate.class)
    @Length(min = 20, max = 7000)
    private String description;

    @NotNull(groups = ValidationMarker.OnCreate.class)
    private Integer category;

    @NotNull(groups = ValidationMarker.OnCreate.class)
    @Future
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private Boolean paid;

    @NotNull(groups = ValidationMarker.OnCreate.class)
    private Location location;

    private Integer participantLimit;

    private Boolean requestModeration;
}
