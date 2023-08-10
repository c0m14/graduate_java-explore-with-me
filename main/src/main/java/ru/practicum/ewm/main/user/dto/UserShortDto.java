package ru.practicum.ewm.main.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserShortDto {
    @NotNull
    private Long id;
    @NotBlank
    private String name;
}
