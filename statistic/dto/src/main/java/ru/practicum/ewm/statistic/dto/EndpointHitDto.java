package ru.practicum.ewm.statistic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.statistic.dto.validator.ValidateIP;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {

    @NotBlank
    private String app;

    @NotBlank
    private String uri;

    @ValidateIP
    private String ip;

    @NotNull
    @Past
    @JsonFormat(pattern = Formats.DATE_TIME_PATTERN)
    private LocalDateTime timestamp;
}
