package ru.practicum.ewm.main.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.category.service.CategoryService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicCategoryController.class)
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private CategoryService categoryService;
    @Captor
    private ArgumentCaptor<Integer> fromArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> sizeArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> categoryIdArgumentCaptor;


    @Test
    @SneakyThrows
    void getCategories_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        int from = 1;
        int size = 5;

        mvc.perform(get("/categories?from={from}&size={size}", from, size))
                .andExpect(status().isOk());

        verify(categoryService, times(1))
                .getCategories(
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture()
                );

        assertThat(fromArgumentCaptor.getValue(), equalTo(from));
        assertThat(sizeArgumentCaptor.getValue(), equalTo(size));
    }

    @Test
    @SneakyThrows
    void getCategories_whenFromMissed_thenStatusIsOkAndDefaultPassedToService() {
        int defaultFrom = 0;
        int size = 5;

        mvc.perform(get("/categories?size={size}", size))
                .andExpect(status().isOk());

        verify(categoryService, times(1))
                .getCategories(
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture()
                );

        assertThat(fromArgumentCaptor.getValue(), equalTo(defaultFrom));
        assertThat(sizeArgumentCaptor.getValue(), equalTo(size));
    }

    @Test
    @SneakyThrows
    void getCategories_whenSizeMissed_thenStatusIsOkAndDefaultPassedToService() {
        int from = 1;
        int defaultSize = 10;

        mvc.perform(get("/categories?from={from}", from))
                .andExpect(status().isOk());

        verify(categoryService, times(1))
                .getCategories(
                        fromArgumentCaptor.capture(),
                        sizeArgumentCaptor.capture()
                );

        assertThat(fromArgumentCaptor.getValue(), equalTo(from));
        assertThat(sizeArgumentCaptor.getValue(), equalTo(defaultSize));
    }

    @Test
    @SneakyThrows
    void getCategories_whenFromNegative_thenStatusBadRequest() {
        int from = -1;
        int size = 10;

        mvc.perform(get("/categories?from={from}&size={size}", from, size))
                .andExpect(status().isBadRequest());

    }

    @Test
    @SneakyThrows
    void getCategories_whenSizeIsZero_thenStatusBadRequest() {
        int from = 0;
        int size = 0;

        mvc.perform(get("/categories?from={from}&size={size}", from, size))
                .andExpect(status().isBadRequest());

    }

    @Test
    @SneakyThrows
    void getCategories_whenSizeIsNegative_thenStatusBadRequest() {
        int from = 0;
        int size = -1;

        mvc.perform(get("/categories?from={from}&size={size}", from, size))
                .andExpect(status().isBadRequest());

    }

    @Test
    @SneakyThrows
    void getCategoryById_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        int categoryId = 1;

        mvc.perform(get("/categories/{catId}", categoryId))
                .andExpect(status().isOk());

        verify(categoryService, times(1))
                .getCategoryById(categoryIdArgumentCaptor.capture());

        assertThat(categoryIdArgumentCaptor.getValue(), equalTo(categoryId));
    }
}