package ru.practicum.ewm.main.category.controller;

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
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.dto.NewCategoryDto;
import ru.practicum.ewm.main.category.service.CategoryService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCategoryController.class)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;
    @Captor
    private ArgumentCaptor<NewCategoryDto> newCategoryDtoArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> categoryIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<CategoryDto> categoryDtoArgumentCaptor;

    @Test
    @SneakyThrows
    void addCategory_whenInvoked_thenStatusIsCreatedAndParamsPassedToService() {
        NewCategoryDto newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("name");

        mvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategoryDto)))
                .andExpect(status().isCreated());

        verify(categoryService, times(1))
                .addCategory(newCategoryDtoArgumentCaptor.capture());

        assertThat(newCategoryDtoArgumentCaptor.getValue(), equalTo(newCategoryDto));
    }

    @Test
    @SneakyThrows
    void addCategory_whenNameMissing_thenStatusIsBadRequest() {
        NewCategoryDto newCategoryDto = new NewCategoryDto();

        mvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategoryDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addCategory_whenNameBlank_thenStatusIsBadRequest() {
        NewCategoryDto newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName(" ");

        mvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategoryDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addCategory_whenNameMoreThan50_thenStatusIsBadRequest() {
        NewCategoryDto newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("n".repeat(51));

        mvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategoryDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void deleteCategory_whenInvoked_thenStatusIsNoContentAndParamPassedToService() {
        int categoryId = 1;

        mvc.perform(delete("/admin/categories/{catId}", categoryId))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1))
                .deleteCategory(categoryIdArgumentCaptor.capture());

        assertThat(categoryIdArgumentCaptor.getValue(), equalTo(categoryId));
    }

    @Test
    @SneakyThrows
    void updateCategory_whenInvoked_thenStatusIsOkAndParamsPassedToService() {
        CategoryDto updateRequestDto = new CategoryDto();
        int categoryId = 1;
        updateRequestDto.setName("new name");

        mvc.perform(patch("/admin/categories/{catId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestDto)))
                .andExpect(status().isOk());

        verify(categoryService, times(1))
                .updateCategory(
                        categoryIdArgumentCaptor.capture(),
                        categoryDtoArgumentCaptor.capture());

        assertThat(categoryIdArgumentCaptor.getValue(), equalTo(categoryId));
        assertThat(categoryDtoArgumentCaptor.getValue(), equalTo(updateRequestDto));
    }

    @Test
    @SneakyThrows
    void updateCategory_whenNameMissing_thenStatusIsBadRequest() {
        CategoryDto updateRequestDto = new CategoryDto();
        int categoryId = 1;

        mvc.perform(patch("/admin/categories/{catId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateCategory_whenNameIsBlank_thenStatusIsBadRequest() {
        CategoryDto updateRequestDto = new CategoryDto();
        int categoryId = 1;
        updateRequestDto.setName(" ");

        mvc.perform(patch("/admin/categories/{catId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void updateCategory_whenNameIsMoreThan50_thenStatusIsBadRequest() {
        CategoryDto updateRequestDto = new CategoryDto();
        int categoryId = 1;
        updateRequestDto.setName("a".repeat(51));

        mvc.perform(patch("/admin/categories/{catId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestDto)))
                .andExpect(status().isBadRequest());
    }
}