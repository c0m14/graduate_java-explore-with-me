package ru.practicum.ewm.statistic.service.model;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EndpointHit {

    private Long id;
    private String app;
    private String uri;
    private String ip;
    private LocalDateTime timestamp;
}
