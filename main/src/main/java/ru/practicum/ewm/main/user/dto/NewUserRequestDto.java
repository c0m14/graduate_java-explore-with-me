package ru.practicum.ewm.main.user.dto;

import lombok.Data;
import ru.practicum.ewm.main.validator.NullOrNotBlank;
import ru.practicum.ewm.main.validator.OnCreateValidation;
import ru.practicum.ewm.main.validator.OnUpdateValidation;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class NewUserRequestDto {

    @NotBlank(groups = OnCreateValidation.class)
    @NullOrNotBlank(groups = OnUpdateValidation.class)
    @Email
    @Size(min = 6, max = 254)
    private String email;

    @NotBlank(groups = OnCreateValidation.class)
    @NullOrNotBlank(groups = OnUpdateValidation.class)
    @Size(min = 2, max = 250)
    private String name;
}
