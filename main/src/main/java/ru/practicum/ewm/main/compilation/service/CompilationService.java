package ru.practicum.ewm.main.compilation.service;

import ru.practicum.ewm.main.compilation.dto.CompilationDto;
import ru.practicum.ewm.main.compilation.dto.NewCompilationDto;

import java.util.List;

public interface CompilationService {
    CompilationDto addCompilation(NewCompilationDto newCompilationDto);

    void deleteCompilation(Long compilationId);

    CompilationDto updateCompilation(Long compilationId, NewCompilationDto updateCompilationRequest);

    List<CompilationDto> findCompilations(boolean pinned, int from, int size);

    CompilationDto findById(Long compilationId);
}
