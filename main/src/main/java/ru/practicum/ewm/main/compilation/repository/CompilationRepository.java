package ru.practicum.ewm.main.compilation.repository;

import ru.practicum.ewm.main.compilation.model.Compilation;

import java.util.List;
import java.util.Optional;

public interface CompilationRepository {

    Compilation save(Compilation compilation);

    void deleteById(Long compilationId);

    Optional<Compilation> findByIdWithoutEvents(Long compilationId);

    void clearEventsRecordsForCompilation(Long compilationId);

    void addEventsRecordsForCompilation(Compilation compilation);

    void update(Compilation compilation);

    List<Compilation> findCompilationsWithoutEvents(boolean pinned, int offset, int size);
}
