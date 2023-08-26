package ru.practicum.ewm.main.category.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.main.category.dto.CategoryDto;
import ru.practicum.ewm.main.category.service.CategoryService;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    public List<CategoryDto> getCategories(
            @PositiveOrZero @RequestParam(name = "from", required = false, defaultValue = "0") int from,
            @Positive @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        log.info("Start GET /categories with from: {}, size: {}", from, size);
        List<CategoryDto> foundCategories = categoryService.getCategories(from, size);
        log.info("Finish GET /categories with {}", foundCategories);
        return foundCategories;
    }

    @GetMapping("/categories/{catId}")
    public CategoryDto getCategoryById(@PathVariable("catId") int categoryId) {
        log.info("Start GET /categories/{catId} with categoryId: {}", categoryId);
        CategoryDto foundCategory = categoryService.getCategoryById(categoryId);
        log.info("Finish GET /categories/{catId} with {}", foundCategory);
        return foundCategory;
    }
}
