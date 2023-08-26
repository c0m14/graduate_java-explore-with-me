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
import ru.practicum.ewm.main.event.dto.NewEventDto;
import ru.practicum.ewm.main.event.dto.updaterequest.UpdateEventUserRequestDto;
import ru.practicum.ewm.main.event.service.EventService;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequestDto;
import ru.practicum.ewm.main.request.dto.RequestStatusUpdateDto;
import ru.practicum.ewm.main.request.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PrivateEventController.class)
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private EventService eventService;
    @MockBean
    private RequestService requestService;
    @Captor
    private ArgumentCaptor<NewEventDto> newEventDtoArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> userIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> fromArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> sizeArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> eventIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<UpdateEventUserRequestDto> updateRequestArgumentCaptor;
    @Captor
    private ArgumentCaptor<EventRequestStatusUpdateRequestDto> updateStatusesArgumentCaptor;

    @Test
    @SneakyThrows
    void addEvent_whenInvoked_thenStatusIsCreatedAndParamsPassedToService() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isCreated());

        verify(eventService, times(1))
                .addEvent(
                        userIdArgumentCaptor.capture(),
                        newEventDtoArgumentCaptor.capture()
                );

        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(newEventDtoArgumentCaptor.getValue().getTitle(), equalTo(newEvent.getTitle()));
        assertThat(newEventDtoArgumentCaptor.getValue().getAnnotation(), equalTo(newEvent.getAnnotation()));
        assertThat(newEventDtoArgumentCaptor.getValue().getDescription(), equalTo(newEvent.getDescription()));
        assertThat(newEventDtoArgumentCaptor.getValue().getCategory(), equalTo(newEvent.getCategory()));
        assertThat(newEventDtoArgumentCaptor.getValue().getLocation(), equalTo(newEvent.getLocation()));
        assertThat(newEventDtoArgumentCaptor.getValue().getPaid(), equalTo(newEvent.getPaid()));
        assertThat(newEventDtoArgumentCaptor.getValue().getParticipantLimit(), equalTo(newEvent.getParticipantLimit()));
        assertThat(newEventDtoArgumentCaptor.getValue().getRequestModeration(), equalTo(newEvent.getRequestModeration()));
    }

    @Test
    @SneakyThrows
    void addEvent_whenTitleIsNull_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setTitle(null);

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenTitleIsBlank_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setTitle(" ".repeat(100));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenTitleLessThan3_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setTitle("T".repeat(2));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenTitleMoreThan120_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setTitle("T".repeat(121));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenAnnotationIsNull_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setAnnotation(null);

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenAnnotationIsBlank_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setAnnotation(" ".repeat(20));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenAnnotationLessThan20_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setAnnotation("A".repeat(19));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenAnnotationMoreThan2000_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setAnnotation("A".repeat(2001));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenDescriptionIsNull_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setDescription(null);

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenDescriptionIsBlank_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setDescription(" ".repeat(21));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenDescriptionLessThan20_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setDescription("d".repeat(19));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenDescriptionMoreThan7000_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setDescription("d".repeat(7001));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenCategoryIsNull_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setCategory(null);

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenEventDateIsNull_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setEventDate(null);

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenLocationIsNull_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setLocation(null);

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenEventDateIsInThePast_thenStatusIsBadRequest() {
        Long userId = 1L;
        NewEventDto newEvent = TestDataProvider.getValidNewEventDto();
        newEvent.setEventDate(LocalDateTime.now().minusMinutes(1));

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addEvent_whenPaidIsAbsent_thenDefaultValuePassedToService() {
        Long userId = 1L;
        boolean defaultPaid = false;
        String newEventString = TestDataProvider.getValidNewEventDtoWithoutPaid();

        mvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newEventString))
                .andExpect(status().isCreated());

        verify(eventService, times(1))
                .addEvent(
                        userIdArgumentCaptor.capture(),
                        newEventDtoArgumentCaptor.capture()
                );

        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(newEventDtoArgumentCaptor.getValue().getRequestModeration(), equalTo(defaultPaid));
    }

    @Test
    @SneakyThrows
    void getUsersEvents_whenFromMissing_thenDefaultValuePassedToService() {
        Long userId = 0L;
        int fromDefault = 0;
        int size = 5;

        mvc.perform(get("/users/{userId}/events?size={size}",
                        userId, size))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .findUsersEvents(
                        userIdArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(fromArgumentCaptor.getValue(), equalTo(fromDefault));
        assertThat(sizeArgumentCaptor.getValue(), equalTo(size));
    }

    @Test
    @SneakyThrows
    void getUsersEvents_whenSizeMissing_thenDefaultValuePassedToService() {
        Long userId = 0L;
        int from = 1;
        int sizeDefault = 10;

        mvc.perform(get("/users/{userId}/events?from={from}",
                        userId, from))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .findUsersEvents(
                        userIdArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(fromArgumentCaptor.getValue(), equalTo(from));
        assertThat(sizeArgumentCaptor.getValue(), equalTo(sizeDefault));
    }

    @Test
    @SneakyThrows
    void getEventByOwnerAndId_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 0L;
        Long eventId = 0L;

        mvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .findUserEventById(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenTitleIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setTitle(null);

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenAnnotationIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setAnnotation(null);

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenDescriptionIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setDescription(null);

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenCategoryIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setCategory(null);

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenEventDateIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setEventDate(null);

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenPaidIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setPaid(null);

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenLocationIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setLocation(null);

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenRequestModerationIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setTitle(null);

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenStateActionIsNull_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setStateAction(null);

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .updateEventByUser(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateRequestArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateRequestArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenTitleIsLessThan3_thenStatusIsBadRequest() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setTitle("t".repeat(2));

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenTitleIsMoreThan120_thenStatusIsBadRequest() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setTitle("t".repeat(121));

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenTitleIsBlank_thenStatusIsBadRequest() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setTitle(" ".repeat(5));

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenAnnotationIsBlank_thenStatusIsBadRequest() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setAnnotation(" ".repeat(5));

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenAnnotationIsLessThan20_thenStatusIsBadRequest() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setAnnotation("a".repeat(19));

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenAnnotationIsMoreThan2000_thenStatusIsBadRequest() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setAnnotation("a".repeat(2001));

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenDescriptionIsBlank_thenStatusIsBadRequest() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setDescription(" ".repeat(5));

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenDescriptionIsMoreThan7000_thenStatusIsBadRequest() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setDescription("d".repeat(7001));

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateEventByOwner_whenEventDateNotInFuture_thenStatusIsBadRequest() {
        Long userId = 1L;
        Long eventId = 2L;
        UpdateEventUserRequestDto updateRequest = TestDataProvider.getValidUpdateEventUserRequest();
        updateRequest.setEventDate(LocalDateTime.now().withNano(0));

        mvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void getUserParticipationRequestsForEvent_whenInvoked_thenStatusIsOkAndParamsPassedToRequestService() {
        Long userId = 1L;
        Long eventId = 2L;

        mvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isOk());

        verify(requestService, times(1))
                .findRequestsToEvent(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
    }

    @Test
    @SneakyThrows
    void updateParticipationRequestForEventByOwner_whenInvoked_thenStatusIsOkAndParamsPassedToRequestService() {
        Long userId = 1L;
        Long eventId = 2L;
        EventRequestStatusUpdateRequestDto updateRequest = EventRequestStatusUpdateRequestDto.builder()
                .requestIds(List.of(1L))
                .status(RequestStatusUpdateDto.CONFIRMED)
                .build();


        mvc.perform(patch("/users/{userId}/events/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(requestService, times(1))
                .updateRequestsStatuses(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture(),
                        updateStatusesArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
        assertThat(updateStatusesArgumentCaptor.getValue(), equalTo(updateRequest));
    }
}