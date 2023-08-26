package ru.practicum.ewm.main.category.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.dto.NewCategoryDto;
import ru.practicum.ewm.main.category.service.CategoryService;

import javax.validation.Valid;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto addCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        log.info("Start POST /admin/categories with: {}", newCategoryDto);
        CategoryDto savedCategory = categoryService.addCategory(newCategoryDto);
        log.info("Finish POST /admin/categories with: {}", savedCategory);
        return savedCategory;
    }

    @DeleteMapping("/admin/categories/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable("catId") int categoryId) {
        log.info("Start DELETE /admin/categories/{catId} with categoryId: {}", categoryId);
        categoryService.deleteCategory(categoryId);
        log.info("Finish DELETE /admin/categories/{catId} with categoryId: {}", categoryId);
    }

    @PatchMapping("/admin/categories/{catId}")
    public CategoryDto updateCategory(
            @PathVariable("catId") int categoryId,
            @Valid @RequestBody CategoryDto updateCategoryRequest) {
        log.info("Start PATCH /admin/categories/{catId} with categoryId: {}, updateRequest: {}",
                categoryId, updateCategoryRequest);
        CategoryDto updatedCategory = categoryService.updateCategory(categoryId, updateCategoryRequest);
        log.info("Finish PATCH /admin/categories/{catId} with categoryId: {}, updatedCategory: {}",
                categoryId, updatedCategory);
        return updateCategoryRequest;
    }
}
