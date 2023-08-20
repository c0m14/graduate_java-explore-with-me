package ru.practicum.ewm.main.request.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.request.service.RequestService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PrivateRequestController.class)
class PrivateRequestControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private RequestService requestService;

    @Captor
    private ArgumentCaptor<Long> userIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> eventIdArgumentCaptor;

    @Test
    @SneakyThrows
    void addRequestFromUser_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 0L;
        Long eventId = 1L;

        mvc.perform(post("/users/{userId}/requests?eventId={eventId}", userId, eventId))
                .andExpect(status().isCreated());

        verify(requestService, times(1))
                .addRequest(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
    }

    @Test
    @SneakyThrows
    void addRequestFromUser_whenEventIdMissing_thenStatusIsBadRequest() {
        Long userId = 0L;

        mvc.perform(post("/users/{userId}/requests", userId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void findUsersRequests_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 0L;

        mvc.perform(get("/users/{userId}/requests", userId))
                .andExpect(status().isOk());

        verify(requestService, times(1))
                .findUserRequests(userIdArgumentCaptor.capture());
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
    }

    @Test
    @SneakyThrows
    void cancelRequestByUser_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long userId = 0L;
        Long eventId = 1L;

        mvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", userId, eventId))
                .andExpect(status().isOk());

        verify(requestService, times(1))
                .cancelRequest(
                        userIdArgumentCaptor.capture(),
                        eventIdArgumentCaptor.capture()
                );
        assertThat(userIdArgumentCaptor.getValue(), equalTo(userId));
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
    }

}