package ru.practicum.ewm.main.category.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class NewCategoryDto {
    @NotBlank
    @Length(max = 50)
    private String name;

}
