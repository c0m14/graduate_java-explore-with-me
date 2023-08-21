package ru.practicum.ewm.main.compilation.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.category.repository.CategoryRepository;
import ru.practicum.ewm.main.compilation.model.Compilation;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CompilationRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CompilationRepository compilationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("delete from compilation");
        jdbcTemplate.update("delete from compilations_events");
    }

    @Test
    void save_whenInvoked_thenCompilationAndLinkTableRecordCreated() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        Compilation compilationToSave = TestDataProvider.getValidCompilationToSave(List.of(event));

        Compilation savedCompilation = compilationRepository.save(compilationToSave);

        Compilation foundCompilation = jdbcTemplate.queryForObject(
                "select compilation_id, pinned, title " +
                        "from compilation " +
                        "where compilation_id = ?", this::mapRowToCompilationWithoutEvents, savedCompilation.getId()
        );
        List<Long> eventLinkedIds = jdbcTemplate.query(
                "select event_id " +
                        "from compilations_events " +
                        "where compilation_id = ?", this::mapToEventId, savedCompilation.getId()
        );
        assertThat(foundCompilation.getId(), equalTo(savedCompilation.getId()));
        assertTrue(eventLinkedIds.contains(event.getId()));
    }

    @Test
    void delete_whenInvoked_thenCompilationAndLinkTableRecordDeleted() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of(event));
        Compilation savedCompilation = compilationRepository.save(compilation);

        compilationRepository.deleteById(savedCompilation.getId());

        List<Compilation> foundCompilations = jdbcTemplate.query(
                "select compilation_id, pinned, title " +
                        "from compilation " +
                        "where compilation_id = ?", this::mapRowToCompilationWithoutEvents, savedCompilation.getId()
        );
        List<Long> eventLinkedIds = jdbcTemplate.query(
                "select event_id " +
                        "from compilations_events " +
                        "where compilation_id = ?", this::mapToEventId, savedCompilation.getId()
        );
        assertTrue(foundCompilations.isEmpty());
        assertTrue(eventLinkedIds.isEmpty());
    }

    @Test
    void addEventsRecordsForCompilation_whenInvoked_thenCompilationAndLinkTableRecordAdded() {
        User eventOwner = userRepository.save(TestDataProvider.getValidUserToSave());
        Category category = categoryRepository.save(TestDataProvider.getValidCategoryToSave());
        Event event = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of(event));
        Compilation savedCompilation = compilationRepository.save(compilation);
        Event newEvent = eventRepository.save(TestDataProvider.getValidNotSavedEvent(eventOwner, category));
        savedCompilation.setEvents(List.of(newEvent));

        compilationRepository.addEventsRecordsForCompilation(savedCompilation);

        List<Long> eventLinkedIds = jdbcTemplate.query(
                "select event_id " +
                        "from compilations_events " +
                        "where compilation_id = ?", this::mapToEventId, savedCompilation.getId()
        );
        assertTrue(eventLinkedIds.contains(newEvent.getId()));
    }

    @Test
    void update_whenInvoked_thenFieldsUpdated() {
        String newTitle = "new title";
        boolean newPinned = false;
        Compilation compilation = TestDataProvider.getValidCompilationToSave(List.of());
        compilation.setTitle("old title");
        compilation.setPinned(true);
        Compilation savedCompilation = compilationRepository.save(compilation);
        savedCompilation.setTitle(newTitle);
        savedCompilation.setPinned(newPinned);

        compilationRepository.update(savedCompilation);

        Compilation foundCompilation = jdbcTemplate.queryForObject(
                "select compilation_id, pinned, title " +
                        "from compilation " +
                        "where compilation_id = ?", this::mapRowToCompilationWithoutEvents, savedCompilation.getId()
        );
        assertThat(foundCompilation.isPinned(), equalTo(newPinned));
        assertThat(foundCompilation.getTitle(), equalTo(newTitle));
    }

    @Test
    void findByIdWithoutEvents_thenInvoked_whenCompilationFound() {
        Compilation compilation = compilationRepository.save(TestDataProvider.getValidCompilationToSave(List.of()));

        Compilation foundCompilation = compilationRepository.findByIdWithoutEvents(compilation.getId()).get();

        Compilation savedCompilation = jdbcTemplate.queryForObject(
                "select compilation_id, pinned, title " +
                        "from compilation " +
                        "where compilation_id = ?", this::mapRowToCompilationWithoutEvents, compilation.getId());
        assertThat(foundCompilation, equalTo(savedCompilation));
    }

    @Test
    void findCompilationsWithoutEvents_whenInvoked_thenCompilationsFoundWithPinnedFilter() {
        int from = 0;
        int size = 10;
        Compilation compilation1 = TestDataProvider.getValidCompilationToSave(List.of());
        Compilation compilation2 = TestDataProvider.getValidCompilationToSave(List.of());
        compilation1.setPinned(true);
        compilation2.setPinned(false);
        compilation1.setTitle("title1");
        compilation2.setTitle("title2");
        compilationRepository.save(compilation1);
        compilationRepository.save(compilation2);

        List<Compilation> foundCompilations =
                compilationRepository.findCompilationsWithoutEvents(true, from, size);

        assertThat(foundCompilations.size(), equalTo(1));
        assertThat(foundCompilations.get(0).getId(), equalTo(compilation1.getId()));
    }

    @Test
    void findCompilationsWithoutEvents_whenInvoked_thenFilteredByFrom() {
        int from = 1;
        int size = 10;
        Compilation compilation1 = TestDataProvider.getValidCompilationToSave(List.of());
        Compilation compilation2 = TestDataProvider.getValidCompilationToSave(List.of());
        compilation1.setPinned(true);
        compilation2.setPinned(true);
        compilation1.setTitle("title1");
        compilation2.setTitle("title2");
        compilationRepository.save(compilation1);
        compilationRepository.save(compilation2);

        List<Compilation> foundCompilations =
                compilationRepository.findCompilationsWithoutEvents(true, from, size);

        assertThat(foundCompilations.size(), equalTo(1));
        assertThat(foundCompilations.get(0).getId(), equalTo(compilation2.getId()));
    }

    @Test
    void findCompilationsWithoutEvents_whenInvoked_thenFilteredBySize() {
        int from = 0;
        int size = 1;
        Compilation compilation1 = TestDataProvider.getValidCompilationToSave(List.of());
        Compilation compilation2 = TestDataProvider.getValidCompilationToSave(List.of());
        compilation1.setPinned(true);
        compilation2.setPinned(true);
        compilation1.setTitle("title1");
        compilation2.setTitle("title2");
        compilationRepository.save(compilation1);
        compilationRepository.save(compilation2);

        List<Compilation> foundCompilations =
                compilationRepository.findCompilationsWithoutEvents(true, from, size);

        assertThat(foundCompilations.size(), equalTo(1));
        assertThat(foundCompilations.get(0).getId(), equalTo(compilation1.getId()));
    }


    private Compilation mapRowToCompilationWithoutEvents(ResultSet resultSet, int rowNum) throws SQLException {
        return Compilation.builder()
                .title(resultSet.getString("title"))
                .pinned(resultSet.getBoolean("pinned"))
                .id(resultSet.getLong("compilation_id"))
                .build();
    }

    private Long mapToEventId(ResultSet resultSet, int rowNum) throws SQLException {
        return resultSet.getLong("event_id");
    }
}