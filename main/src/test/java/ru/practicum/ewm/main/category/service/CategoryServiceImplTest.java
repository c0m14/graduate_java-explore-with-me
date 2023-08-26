package ru.practicum.ewm.main.category.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.category.repository.CategoryRepository;
import ru.practicum.ewm.main.exception.NotExistsException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void getCategoryById_whenOptionalEmpty_thenNotExistExceptionThrows() {
        int categoryId = 0;

        when(categoryRepository.findCategoryById(categoryId))
                .thenReturn(Optional.empty());

        assertThrows(NotExistsException.class,
                () -> categoryService.getCategoryById(categoryId));
    }
}