package ru.practicum.ewm.main.user.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
public class NewUserRequest {

    @NotNull
    @Email
    @Length(min = 6)
    private String email;

    @NotNull
    @Length(min = 2, max = 250)
    private String name;
}
