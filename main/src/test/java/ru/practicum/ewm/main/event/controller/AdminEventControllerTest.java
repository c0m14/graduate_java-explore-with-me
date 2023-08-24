package ru.practicum.ewm.main.event.controller;

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
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.event.dto.searchrequest.AdminSearchParamsDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventAdminRequestDto;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.service.EventService;
import ru.practicum.ewm.statistic.dto.Formats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminEventController.class)
class AdminEventControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private EventService eventService;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_PATTERN);

    @Captor
    private ArgumentCaptor<AdminSearchParamsDto> searchParamsArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> eventIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<UpdateEventAdminRequestDto> updateRequestArgumentCaptor;


    @Test
    @SneakyThrows
    void findEvents_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long usersId = 0L;
        String state = "PUBLISHED";
        Integer categoryId = 1;
        String rangeStart = LocalDateTime.now().minusMonths(1).format(formatter);
        String rangeEnd = LocalDateTime.now().plusDays(1).format(formatter);
        Integer from = 2;
        Integer size = 5;

        mvc.perform(get("/admin/events?users={users}&states={states}&categories={categories}&" +
                                "rangeStart={rangeStart}&rangeEnd={rangeEnd}&from={from}&size={size}",
                        usersId, state, categoryId, rangeStart, rangeEnd, from, size))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .findEventsAdmin(searchParamsArgumentCaptor.capture());
        assertThat(searchParamsArgumentCaptor.getValue().getUsersIds(), equalTo(Set.of(usersId)));
        assertThat(searchParamsArgumentCaptor.getValue().getCategoriesIds(), equalTo(Set.of(categoryId)));
        assertThat(searchParamsArgumentCaptor.getValue().getStates(), equalTo(Set.of(EventState.from(state).get())));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(),
                equalTo(LocalDateTime.parse(rangeStart, formatter)));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeEnd(),
                equalTo(LocalDateTime.parse(rangeEnd, formatter)));
        assertThat(searchParamsArgumentCaptor.getValue().getFrom(), equalTo(from));
        assertThat(searchParamsArgumentCaptor.getValue().getSize(), equalTo(size));
    }

    @Test
    @SneakyThrows
    void findEvents_whenStateNotValid_thenStatusIsBadRequest() {
        String state = "NOT_VALID";

        mvc.perform(get("/admin/events?states={states}}", state))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void findEvents_whenFromIsAbsent_thenStatusIsOkAndDefaultValuePassedToService() {
        Integer defaultFrom = 0;

        mvc.perform(get("/admin/events"))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .findEventsAdmin(searchParamsArgumentCaptor.capture());

        assertThat(searchParamsArgumentCaptor.getValue().getFrom(), equalTo(defaultFrom));
    }

    @Test
    @SneakyThrows
    void findEvents_whenSizeIsAbsent_thenStatusIsOkAndDefaultValuePassedToService() {
        Integer defaultSize = 10;

        mvc.perform(get("/admin/events"))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .findEventsAdmin(searchParamsArgumentCaptor.capture());

        assertThat(searchParamsArgumentCaptor.getValue().getSize(), equalTo(defaultSize));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 0L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenTitleIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setTitle(null);

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenAnnotationIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setAnnotation(null);

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenDescriptionIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setDescription(null);

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenCategoryIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setCategory(null);

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenEventDateIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setEventDate(null);

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenPaidIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setPaid(null);

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenLocationIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setLocation(null);

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenRequestModerationIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setTitle(null);

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenStateActionIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setStateAction(null);

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByAdmin(
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEvent_whenTitleIsLessThan3_thenStatusIsBadRequest() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setTitle("t".repeat(2));

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEvent_whenTitleIsMoreThan120_thenStatusIsBadRequest() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setTitle("t".repeat(121));

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEvent_whenTitleIsBlank_thenStatusIsBadRequest() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setTitle(" ".repeat(5));

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEvent_whenAnnotationIsBlank_thenStatusIsBadRequest() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setAnnotation(" ".repeat(5));

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEvent_whenAnnotationIsLessThan20_thenStatusIsBadRequest() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setAnnotation("a".repeat(19));

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEvent_whenAnnotationIsMoreThan2000_thenStatusIsBadRequest() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setAnnotation("a".repeat(2001));

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEvent_whenDescriptionIsBlank_thenStatusIsBadRequest() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setDescription(" ".repeat(5));

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEvent_whenDescriptionIsMoreThan7000_thenStatusIsBadRequest() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setDescription("d".repeat(7001));

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEvent_whenEventDateNotInFuture_thenStatusIsBadRequest() {
        Long eventId = 2L;
        UpdateEventAdminRequestDto updateRequest = TestDataProvider.getValidUpdateEventAdminRequest();
        updateRequest.setEventDate(LocalDateTime.now().withNano(0));

        mvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

}