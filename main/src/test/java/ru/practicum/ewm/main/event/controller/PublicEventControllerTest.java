package ru.practicum.ewm.main.event.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.event.dto.searchrequest.PublicSearchParamsDto;
import ru.practicum.ewm.main.event.dto.searchrequest.SearchSortOptionDto;
import ru.practicum.ewm.main.event.service.EventService;
import ru.practicum.ewm.statistic.dto.Formats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicEventController.class)
class PublicEventControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private EventService eventService;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_PATTERN);
    @Captor
    private ArgumentCaptor<PublicSearchParamsDto> searchParamsArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> eventIdArgumentCaptor;

    @Test
    @SneakyThrows
    void findEvents_whenInvoked_thenStatusIsOkAndParamsPassToService() {
        String text = "text";
        String categories = "1";
        Boolean paid = true;
        String rangeStart = LocalDateTime.now().minusMonths(1).format(formatter);
        String rangeEnd = LocalDateTime.now().plusDays(1).format(formatter);
        Boolean onlyAvailable = true;
        String sort = "event_date";
        Integer from = 1;
        Integer size = 5;

        mvc.perform(get("/events?text={text}&categories={categories}&paid={paid}" +
                                "&rangeStart={rangeStart}&rangeEnd={rangeEnd}&onlyAvailable={onlyAvailable}" +
                                "&sort={sort}&from={from}&size={size}", text, categories, paid, rangeStart, rangeEnd,
                        onlyAvailable, sort, from, size))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .findEventsPublic(searchParamsArgumentCaptor.capture(), anyString());
        assertThat(searchParamsArgumentCaptor.getValue().getText(), equalTo(text));
        assertThat(searchParamsArgumentCaptor.getValue().getCategoriesIds(), equalTo(Set.of(Integer.valueOf(categories))));
        assertThat(searchParamsArgumentCaptor.getValue().getPaid(), equalTo(paid));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeStart(),
                equalTo(LocalDateTime.parse(rangeStart, formatter)));
        assertThat(searchParamsArgumentCaptor.getValue().getRangeEnd(),
                equalTo(LocalDateTime.parse(rangeEnd, formatter)));
        assertThat(searchParamsArgumentCaptor.getValue().getOnlyAvailable(), equalTo(onlyAvailable));
        assertThat(searchParamsArgumentCaptor.getValue().getSortOption(), equalTo(SearchSortOptionDto.EVENT_DATE));
        assertThat(searchParamsArgumentCaptor.getValue().getFrom(), equalTo(from));
        assertThat(searchParamsArgumentCaptor.getValue().getSize(), equalTo(size));

    }

    @Test
    @SneakyThrows
    void findEventById_whenInvoked_thenStatusIsOkAndParamsPassToService() {
        Long eventId = 0L;

        mvc.perform(get("/events/{id}", eventId))
                .andExpect(status().isOk());

        verify(eventService, times(1))
                .findEventByIdPublic(eventIdArgumentCaptor.capture(), anyString());

        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
    }
}