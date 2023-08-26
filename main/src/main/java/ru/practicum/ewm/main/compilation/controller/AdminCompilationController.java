package ru.practicum.ewm.main.compilation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.compilation.dto.CompilationDto;
import ru.practicum.ewm.main.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.main.compilation.service.CompilationService;
import ru.practicum.ewm.main.validator.OnCreateValidation;
import ru.practicum.ewm.main.validator.OnUpdateValidation;

import javax.validation.Valid;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class AdminCompilationController {

    private final CompilationService compilationService;

    @PostMapping("/admin/compilations")
    @ResponseStatus(HttpStatus.CREATED)
    @Validated(OnCreateValidation.class)
    public CompilationDto addCompilation(@Valid @RequestBody NewCompilationDto newCompilation) {
        log.info("Start POST /admin/compilations with {}", newCompilation);
        CompilationDto addedCompilation = compilationService.addCompilation(newCompilation);
        log.info("Finish POST /admin/compilations with {}", addedCompilation);
        return addedCompilation;
    }

    @DeleteMapping("/admin/compilations/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable(name = "compId") Long compilationId) {
        log.info("Start DELETE /admin/compilations/{compId} with id: {}", compilationId);
        compilationService.deleteCompilation(compilationId);
        log.info("Finish DELETE /admin/compilations/{compId} with id: {}", compilationId);
    }

    @PatchMapping("/admin/compilations/{compId}")
    @Validated(OnUpdateValidation.class)
    public CompilationDto updateCompilation(@PathVariable(name = "compId") Long compilationId,
                                            @Valid @RequestBody NewCompilationDto updateRequest) {
        log.info("Start PATCH /admin/compilations/{compId} with id: {}, updateRequest: {}",
                compilationId, updateRequest);
        CompilationDto updatedCompilation = compilationService.updateCompilation(compilationId, updateRequest);
        log.info("Finish PATCH /admin/compilations/{compId} with {}", updatedCompilation);
        return updatedCompilation;
    }
}
