package ru.practicum.ewm.main.compilation.controller;

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
import ru.practicum.ewm.main.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.main.compilation.service.CompilationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCompilationController.class)
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private CompilationService compilationService;

    @Captor
    private ArgumentCaptor<NewCompilationDto> newCompilationDtoArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> compilationIdArgumentCaptor;

    @Test
    @SneakyThrows
    void addCompilation_whenInvoked_thenStatusIsCreatedAndParamsPassedToService() {
        NewCompilationDto newCompilationDto = TestDataProvider.getValidNewCompilationDto();

        mvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCompilationDto)))
                .andExpect(status().isCreated());

        verify(compilationService, times(1))
                .addCompilation(newCompilationDtoArgumentCaptor.capture());
        assertThat(newCompilationDtoArgumentCaptor.getValue(), equalTo(newCompilationDto));
    }

    @Test
    @SneakyThrows
    void addCompilation_whenTitleIsBlank_thenStatusIsBadRequest() {
        NewCompilationDto newCompilationDto = TestDataProvider.getValidNewCompilationDto();
        newCompilationDto.setTitle(" ");

        mvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCompilationDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addCompilation_whenTitleIsNull_thenStatusIsBadRequest() {
        NewCompilationDto newCompilationDto = TestDataProvider.getValidNewCompilationDto();
        newCompilationDto.setTitle(null);

        mvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCompilationDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addCompilation_whenTitleIsMoreThan50_thenStatusIsBadRequest() {
        NewCompilationDto newCompilationDto = TestDataProvider.getValidNewCompilationDto();
        newCompilationDto.setTitle("t".repeat(51));

        mvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCompilationDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void deleteCompilation_whenInvoked_thenStatusIsNoContentAndParamsPassedToService() {
        Long compilationId = 0L;

        mvc.perform(delete("/admin/compilations/{compId}", compilationId))
                .andExpect(status().isNoContent());

        verify(compilationService, times(1))
                .deleteCompilation(compilationIdArgumentCaptor.capture());
        assertThat(compilationIdArgumentCaptor.getValue(), equalTo(compilationId));
    }

    @Test
    @SneakyThrows
    void updateCompilation_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long compilationId = 0L;
        NewCompilationDto updateRequest = NewCompilationDto.builder().build();

        mvc.perform(patch("/admin/compilations/{compId}", compilationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(compilationService, times(1))
                .updateCompilation(
                        compilationIdArgumentCaptor.capture(),
                        newCompilationDtoArgumentCaptor.capture()
                );
        assertThat(compilationIdArgumentCaptor.getValue(), equalTo(compilationId));
        assertThat(newCompilationDtoArgumentCaptor.getValue(), equalTo(updateRequest));
    }

    @Test
    @SneakyThrows
    void updateCompilation_whenTitleIsBlank_thenStatusIsBadRequest() {
        Long compilationId = 0L;
        NewCompilationDto updateRequest = NewCompilationDto.builder()
                .title(" ")
                .build();

        mvc.perform(patch("/admin/compilations/{compId}", compilationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateCompilation_whenTitleIsMoreThan50_thenStatusIsBadRequest() {
        Long compilationId = 0L;
        NewCompilationDto updateRequest = NewCompilationDto.builder()
                .title("t".repeat(51))
                .build();

        mvc.perform(patch("/admin/compilations/{compId}", compilationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }
}