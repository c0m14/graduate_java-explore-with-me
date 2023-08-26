package ru.practicum.ewm.main.user.controller;

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
import ru.practicum.ewm.main.user.dto.NewUserRequestDto;
import ru.practicum.ewm.main.user.service.UserService;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private UserService userService;
    @Captor
    private ArgumentCaptor<NewUserRequestDto> newUserRequestArgumentCaptor;
    @Captor
    private ArgumentCaptor<List<Long>> idsArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> fromArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> sizeArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> userIdArgumentCaptor;

    @Test
    @SneakyThrows
    void addUser_whenInvoked_thenStatusIsCreatedAndRequestPassedToService() {
        NewUserRequestDto newUser = new NewUserRequestDto();
        newUser.setName("name");
        newUser.setEmail("email@email.ru");

        mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated());

        verify(userService, times(1))
                .addUser(newUserRequestArgumentCaptor.capture());

        assertThat(newUser, equalTo(newUserRequestArgumentCaptor.getValue()));
    }

    @Test
    @SneakyThrows
    void addUser_whenBodyMissing_thenStatusIsBadRequest() {

        mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

    }

    @Test
    @SneakyThrows
    void addUser_whenEmailMissing_thenStatusIsBadRequest() {
        NewUserRequestDto newUser = new NewUserRequestDto();
        newUser.setName("name");

        mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addUser_whenEmailWrongFormat_thenStatusIsBadRequest() {
        NewUserRequestDto newUser = new NewUserRequestDto();
        newUser.setName("name");
        newUser.setEmail("email");

        mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addUser_whenEmailLengthLessThan6_thenStatusIsBadRequest() {
        NewUserRequestDto newUser = new NewUserRequestDto();
        newUser.setName("name");
        newUser.setEmail("e@e.r");

        mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addUser_whenNameMissing_thenStatusIsBadRequest() {
        NewUserRequestDto newUser = new NewUserRequestDto();
        newUser.setEmail("email@email.ru");

        mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addUser_whenNameLengthLessThan2_thenStatusIsBadRequest() {
        NewUserRequestDto newUser = new NewUserRequestDto();
        newUser.setEmail("email@email.ru");
        newUser.setName("n");

        mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addUser_whenNameLengthMoreThan250_thenStatusIsBadRequest() {
        NewUserRequestDto newUser = new NewUserRequestDto();
        newUser.setEmail("email@email.ru");
        newUser.setName("n".repeat(251));

        mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void getUsers_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long id1 = 1L;
        Long id2 = 2L;
        List<Long> ids = List.of(id1, id2);
        int from = 1;
        int size = 5;

        mvc.perform(get("/admin/users?ids={id1}&ids={id2}&from={from}&size={size}",
                        id1, id2, from, size))
                .andExpect(status().isOk());

        verify(userService, times(1))
                .getUsers(
                        idsArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture());

        assertThat(ids, equalTo(idsArgumentCaptor.getValue()));
        assertThat(from, equalTo(fromArgumentCaptor.getValue()));
        assertThat(size, equalTo(sizeArgumentCaptor.getValue()));
    }

    @Test
    @SneakyThrows
    void getUsers_whenIdsMissing_thenStatusIsOkAndParamsPassedToService() {
        int from = 1;
        int size = 5;

        mvc.perform(get("/admin/users?from={from}&size={size}",
                        from, size))
                .andExpect(status().isOk());

        verify(userService, times(1))
                .getUsers(
                        idsArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture());

        assertThat(from, equalTo(fromArgumentCaptor.getValue()));
        assertThat(size, equalTo(sizeArgumentCaptor.getValue()));
    }

    @Test
    @SneakyThrows
    void getUsers_whenFromMissing_thenStatusIsOkAndDefaultParamPassedToService() {
        Long id1 = 1L;
        Long id2 = 2L;
        List<Long> ids = List.of(id1, id2);
        int fromDefault = 0;
        int size = 5;

        mvc.perform(get("/admin/users?ids={id1}&ids={id2}&size={size}",
                        id1, id2, size))
                .andExpect(status().isOk());

        verify(userService, times(1))
                .getUsers(
                        idsArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture());

        assertThat(ids, equalTo(idsArgumentCaptor.getValue()));
        assertThat(fromDefault, equalTo(fromArgumentCaptor.getValue()));
        assertThat(size, equalTo(sizeArgumentCaptor.getValue()));
    }

    @Test
    @SneakyThrows
    void getUsers_whenSizeMissing_thenStatusIsOkAndDefaultParamPassedToService() {
        Long id1 = 1L;
        Long id2 = 2L;
        List<Long> ids = List.of(id1, id2);
        int from = 1;
        int sizeDefault = 10;

        mvc.perform(get("/admin/users?ids={id1}&ids={id2}&from={from}",
                        id1, id2, from))
                .andExpect(status().isOk());

        verify(userService, times(1))
                .getUsers(
                        idsArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture());

        assertThat(ids, equalTo(idsArgumentCaptor.getValue()));
        assertThat(from, equalTo(fromArgumentCaptor.getValue()));
        assertThat(sizeDefault, equalTo(sizeArgumentCaptor.getValue()));
    }

    @Test
    @SneakyThrows
    void deleteUser_whenInvoked_thenStatusIsNoContentAndParamPassedToService() {
        Long id = 1L;

        mvc.perform(delete("/admin/users/{userId}",
                        id))
                .andExpect(status().isNoContent());

        verify(userService, times(1))
                .deleteUser(userIdArgumentCaptor.capture());

        assertThat(id, equalTo(userIdArgumentCaptor.getValue()));
    }


}