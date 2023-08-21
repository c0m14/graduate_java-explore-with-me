package ru.practicum.ewm.main.compilation.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.compilation.service.CompilationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicCompilationController.class)
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private CompilationService compilationService;

    @Captor
    private ArgumentCaptor<Boolean> pinnedArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> fromArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> sizeArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> compilationIdArgumentCaptor;

    @Test
    @SneakyThrows
    void findCompilations_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        boolean pinned = true;
        int from = 1;
        int size = 5;

        mvc.perform(get("/compilations?pinned={pinned}&from={from}&size={size}",
                        pinned, from, size))
                .andExpect(status().isOk());

        verify(compilationService, times(1))
                .findCompilations(
                        pinnedArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture()
                );
        assertThat(pinnedArgumentCaptor.getValue(), equalTo(pinned));
        assertThat(fromArgumentCaptor.getValue(), equalTo(from));
        assertThat(sizeArgumentCaptor.getValue(), equalTo(size));
    }

    @SneakyThrows
    @Test
    void findCompilations_whenPinnedMissing_thenStatusIsOkAndDefaultParamsPassedToService() {
        boolean defaultPinned = false;
        int from = 1;
        int size = 5;

        mvc.perform(get("/compilations?from={from}&size={size}",
                        from, size))
                .andExpect(status().isOk());

        verify(compilationService, times(1))
                .findCompilations(
                        pinnedArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture()
                );
        assertThat(pinnedArgumentCaptor.getValue(), equalTo(defaultPinned));
        assertThat(fromArgumentCaptor.getValue(), equalTo(from));
        assertThat(sizeArgumentCaptor.getValue(), equalTo(size));
    }

    @SneakyThrows
    @Test
    void findCompilations_whenFromMissing_thenStatusIsOkAndDefaultParamsPassedToService() {
        boolean pinned = true;
        int defaultFrom = 0;
        int size = 5;

        mvc.perform(get("/compilations?pinned={pinned}&size={size}",
                        pinned, size))
                .andExpect(status().isOk());

        verify(compilationService, times(1))
                .findCompilations(
                        pinnedArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture()
                );
        assertThat(pinnedArgumentCaptor.getValue(), equalTo(pinned));
        assertThat(fromArgumentCaptor.getValue(), equalTo(defaultFrom));
        assertThat(sizeArgumentCaptor.getValue(), equalTo(size));
    }

    @SneakyThrows
    @Test
    void findCompilations_whenSizeMissing_thenStatusIsOkAndDefaultParamsPassedToService() {
        boolean pinned = true;
        int from = 1;
        int defaultSize = 10;

        mvc.perform(get("/compilations?pinned={pinned}&from={from}",
                        pinned, from))
                .andExpect(status().isOk());

        verify(compilationService, times(1))
                .findCompilations(
                        pinnedArgumentCaptor.capture(),
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture()
                );
        assertThat(pinnedArgumentCaptor.getValue(), equalTo(pinned));
        assertThat(fromArgumentCaptor.getValue(), equalTo(from));
        assertThat(sizeArgumentCaptor.getValue(), equalTo(defaultSize));
    }

    @Test
    @SneakyThrows
    void findCompilationById_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        Long compilationId = 0L;

        mvc.perform(get("/compilations/{compId}", compilationId))
                .andExpect(status().isOk());

        verify(compilationService, times(1))
                .findById(compilationIdArgumentCaptor.capture());
        assertThat(compilationIdArgumentCaptor.getValue(), equalTo(compilationId));
    }
}