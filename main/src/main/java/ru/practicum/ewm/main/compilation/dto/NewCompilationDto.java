package ru.practicum.ewm.main.compilation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import ru.practicum.ewm.main.validator.NullOrNotBlank;
import ru.practicum.ewm.main.validator.ValidationMarker;

import javax.validation.constraints.NotBlank;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {

    private Set<Long> events;
    private Boolean pinned;

    @NotBlank(groups = ValidationMarker.OnCreate.class)
    @NullOrNotBlank(groups = ValidationMarker.OnUpdate.class)
    @Length(max = 50)
    private String title;
}
