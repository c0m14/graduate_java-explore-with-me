package ru.practicum.ewm.main.category.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;


@Data
@NoArgsConstructor
public class CategoryDto {
    private int id;

    @Length(max = 50)
    @NotBlank
    private String name;

    public CategoryDto(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
