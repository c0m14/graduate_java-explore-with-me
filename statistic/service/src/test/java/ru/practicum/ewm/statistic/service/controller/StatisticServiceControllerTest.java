package ru.practicum.ewm.statistic.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.statistic.dto.EndpointHitDto;
import ru.practicum.ewm.statistic.dto.Formats;
import ru.practicum.ewm.statistic.service.service.StatisticService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatisticServiceController.class)
class StatisticServiceControllerTest {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_PATTERN);
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private StatisticService statisticService;
    @Captor
    private ArgumentCaptor<EndpointHitDto> endpointHitDtoArgumentCaptor;
    @Captor
    private ArgumentCaptor<LocalDateTime> startArgumentCaptor;
    @Captor
    private ArgumentCaptor<LocalDateTime> endArgumentCaptor;
    @Captor
    private ArgumentCaptor<List<String>> urisArgumentCaptor;
    @Captor
    private ArgumentCaptor<Boolean> uniqueArgumentCaptor;

    @SneakyThrows
    @Test
    void saveEndpointHit_whenInvoked_thenStatusIsOkAndDtoPassedToService() {
        LocalDateTime timestamp = LocalDateTime.parse("2022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("app")
                .uri("/uri")
                .ip("1.1.1.1")
                .timestamp(timestamp)
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isCreated());

        verify(statisticService, times(1))
                .saveEndpointHit(endpointHitDtoArgumentCaptor.capture());
        assertEquals(endpointHitDto, endpointHitDtoArgumentCaptor.getValue(),
                "Invalid EnpointHitDto passed to service");
    }

    @SneakyThrows
    @Test
    void saveEndpointHit_whenAppFieldIsNull_thenStatusIsBadRequest() {
        LocalDateTime timestamp = LocalDateTime.parse("2022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .uri("/uri")
                .ip("1.1.1.1")
                .timestamp(timestamp)
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void saveEndpointHit_whenAppFieldIsBlank_thenStatusIsBadRequest() {
        LocalDateTime timestamp = LocalDateTime.parse("2022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app(" ")
                .uri("/uri")
                .ip("1.1.1.1")
                .timestamp(timestamp)
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void saveEndpointHit_whenUriFieldIsNull_thenStatusIsBadRequest() {
        LocalDateTime timestamp = LocalDateTime.parse("2022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("app")
                .ip("1.1.1.1")
                .timestamp(timestamp)
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void saveEndpointHit_whenUriFieldIsBlank_thenStatusIsBadRequest() {
        LocalDateTime timestamp = LocalDateTime.parse("2022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("app")
                .uri(" ")
                .ip("1.1.1.1")
                .timestamp(timestamp)
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void saveEndpointHit_whenIpFieldIsNull_thenStatusIsBadRequest() {
        LocalDateTime timestamp = LocalDateTime.parse("2022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("app")
                .uri("/uri")
                .timestamp(timestamp)
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void saveEndpointHit_whenIpFieldIsBlank_thenStatusIsBadRequest() {
        LocalDateTime timestamp = LocalDateTime.parse("2022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("app")
                .uri("/uri")
                .ip(" ")
                .timestamp(timestamp)
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void saveEndpointHit_whenIpFieldIsWrongFormat_thenStatusIsBadRequest() {
        LocalDateTime timestamp = LocalDateTime.parse("2022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("app")
                .uri("/uri")
                .ip("11111")
                .timestamp(timestamp)
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void saveEndpointHit_whenTimestampFieldIsNull_thenStatusIsBadRequest() {
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("app")
                .uri("/uri")
                .ip("1.1.1.1")
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void saveEndpointHit_whenTimestampFieldIsInFuture_thenStatusIsBadRequest() {
        LocalDateTime timestamp = LocalDateTime.parse("3022-09-06 11:00:23", formatter);
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("app")
                .uri("/uri")
                .ip("1.1.1.1")
                .timestamp(timestamp)
                .build();

        mvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(endpointHitDto)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void getViewStats_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        String start = "2023-01-01 00:00:00";
        String end = "2024-01-01 00:00:00";
        String uri1 = "/endpoint1";
        String uri2 = "/endpoint2";
        boolean unique = false;

        mvc.perform(get("/stats?start={start}&end={end}&uris={uris1}&uris={uris2}&unique={unique}",
                        start, end, uri1, uri2, String.valueOf(unique)))
                .andExpect(status().isOk());

        verify(statisticService, times(1))
                .getViewStats(
                        startArgumentCaptor.capture(),
                        endArgumentCaptor.capture(),
                        urisArgumentCaptor.capture(),
                        uniqueArgumentCaptor.capture()
                );
        assertEquals(LocalDateTime.parse(start, formatter), startArgumentCaptor.getValue(),
                "Invalid start param passed to service");
        assertEquals(LocalDateTime.parse(end, formatter), endArgumentCaptor.getValue(),
                "Invalid end param passed to service");
        assertEquals(uri1, urisArgumentCaptor.getValue().get(0),
                "Invalid first uri param passed to service");
        assertEquals(uri2, urisArgumentCaptor.getValue().get(1),
                "Invalid second uri param passed to service");
        assertEquals(unique, uniqueArgumentCaptor.getValue(),
                "Invalid unique param passed to service");
    }

    @SneakyThrows
    @Test
    void getViewStats_whenUrisAbsent_thenStatusIsOkAndParamsPassedToService() {
        String start = "2023-01-01 00:00:00";
        String end = "2024-01-01 00:00:00";
        boolean unique = false;

        mvc.perform(get("/stats?start={start}&end={end}&unique={unique}",
                        start, end, String.valueOf(unique)))
                .andExpect(status().isOk());

        verify(statisticService, times(1))
                .getViewStats(
                        startArgumentCaptor.capture(),
                        endArgumentCaptor.capture(),
                        urisArgumentCaptor.capture(),
                        uniqueArgumentCaptor.capture()
                );
        assertEquals(LocalDateTime.parse(start, formatter), startArgumentCaptor.getValue(),
                "Invalid start param passed to service");
        assertEquals(LocalDateTime.parse(end, formatter), endArgumentCaptor.getValue(),
                "Invalid end param passed to service");
        assertEquals(unique, uniqueArgumentCaptor.getValue(),
                "Invalid unique param passed to service");
    }

    @SneakyThrows
    @Test
    void getViewStats_whenUniqueAbsent_thenStatusIsOkAndDefaultValuePassedToService() {
        String start = "2023-01-01 00:00:00";
        String end = "2024-01-01 00:00:00";
        String uri1 = "/endpoint1";
        String uri2 = "/endpoint2";
        boolean uniqueDefault = false;

        mvc.perform(get("/stats?start={start}&end={end}&uris={uris1}&uris={uris2}",
                        start, end, uri1, uri2))
                .andExpect(status().isOk());

        verify(statisticService, times(1))
                .getViewStats(
                        startArgumentCaptor.capture(),
                        endArgumentCaptor.capture(),
                        urisArgumentCaptor.capture(),
                        uniqueArgumentCaptor.capture()
                );

        assertEquals(uniqueDefault, uniqueArgumentCaptor.getValue(),
                "Invalid unique param passed to service");
    }

    @SneakyThrows
    @Test
    void getViewStats_whenStartAbsent_thenStatusIsBadRequest() {
        String end = "2024-01-01 00:00:00";
        String uri1 = "/endpoint1";
        String uri2 = "/endpoint2";
        boolean unique = false;

        mvc.perform(get("/stats?end={end}&uris={uris1}&uris={uris2}&unique={unique}",
                        end, uri1, uri2, String.valueOf(unique)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void getViewStats_whenEndAbsent_thenStatusIsBadRequest() {
        String start = "2023-01-01 00:00:00";
        String uri1 = "/endpoint1";
        String uri2 = "/endpoint2";
        boolean unique = false;

        mvc.perform(get("/stats?start={start}&uris={uris1}&uris={uris2}&unique={unique}",
                        start, uri1, uri2, String.valueOf(unique)))
                .andExpect(status().isBadRequest());
    }
}