package ru.practicum.ewm.main.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.validator.NullOrNotBlank;
import ru.practicum.ewm.main.validator.OnCreateValidation;
import ru.practicum.ewm.main.validator.OnUpdateValidation;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank(groups = OnCreateValidation.class)
    @NullOrNotBlank(groups = OnUpdateValidation.class)
    @Size(min = 3, max = 120)
    private String title;

    @NotBlank(groups = OnCreateValidation.class)
    @NullOrNotBlank(groups = OnUpdateValidation.class)
    @Size(min = 20, max = 2000)
    private String annotation;

    @NotBlank(groups = OnCreateValidation.class)
    @NullOrNotBlank(groups = OnUpdateValidation.class)
    @Size(min = 20, max = 7000)
    private String description;

    @NotNull(groups = OnCreateValidation.class)
    private Integer category;

    @NotNull(groups = OnCreateValidation.class)
    @Future
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private Boolean paid;

    @NotNull(groups = OnCreateValidation.class)
    private Location location;

    private Integer participantLimit;

    private Boolean requestModeration;
}
