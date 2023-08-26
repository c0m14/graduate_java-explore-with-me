package ru.practicum.ewm.main.compilation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.main.validator.NullOrNotBlank;
import ru.practicum.ewm.main.validator.OnCreateValidation;
import ru.practicum.ewm.main.validator.OnUpdateValidation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {

    private Set<Long> events;
    private Boolean pinned;

    @NotBlank(groups = OnCreateValidation.class)
    @NullOrNotBlank(groups = OnUpdateValidation.class)
    @Size(min = 1, max = 50)
    private String title;
}
