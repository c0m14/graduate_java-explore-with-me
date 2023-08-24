package ru.practicum.ewm.main.compilation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.main.compilation.dto.CompilationDto;
import ru.practicum.ewm.main.compilation.service.CompilationService;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class PublicCompilationController {

    private final CompilationService compilationService;

    @GetMapping("/compilations")
    public List<CompilationDto> findCompilations(
            @RequestParam(name = "pinned", required = false, defaultValue = "false") boolean pinned,
            @PositiveOrZero @RequestParam(name = "from", required = false, defaultValue = "0") int from,
            @Positive @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
        log.info("Start GET /compilations with pinned: {}, from: {}, size: {}", pinned, from, size);
        List<CompilationDto> foundCompilations = compilationService.findCompilations(pinned, from, size);
        log.info("Finish GET /compilations with {}", foundCompilations);
        return foundCompilations;
    }

    @GetMapping("/compilations/{compId}")
    public CompilationDto findCompilationById(@PathVariable(name = "compId") Long compilationId) {
        log.info("Start GET /compilations/{compId} with id {}", compilationId);
        CompilationDto foundCompilation = compilationService.findById(compilationId);
        log.info("Finish GET /compilations/{compId} with {}", foundCompilation);
        return foundCompilation;
    }
}
