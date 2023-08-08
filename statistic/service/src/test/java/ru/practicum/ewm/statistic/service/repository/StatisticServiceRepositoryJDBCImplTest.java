package ru.practicum.ewm.statistic.service.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.ewm.statistic.dto.Formats;
import ru.practicum.ewm.statistic.dto.ViewStatsDto;
import ru.practicum.ewm.statistic.service.model.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest
class StatisticServiceRepositoryJDBCImplTest {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_PATTERN);
    @Autowired
    private StatisticServiceRepository statisticRepository;

    @BeforeEach
    public void beforeEach() {
        statisticRepository.deleteAll();
    }

    @Test
    void getViewStatsWithUniqueIpHits() {
        String ip = "1.1.1.1";
        EndpointHit firstHit = getEndpointHitWithUriAndIp("/uri", ip);
        EndpointHit secondHit = getEndpointHitWithUriAndIp("/uri", ip);
        statisticRepository.save(firstHit);
        statisticRepository.save(secondHit);

        List<ViewStatsDto> stat = statisticRepository.getViewStats(
                LocalDateTime.parse("2023-01-01 00:00:00", formatter),
                LocalDateTime.parse("2024-01-01 00:00:00", formatter),
                List.of(),
                true
        );

        assertThat(stat.size(), equalTo(1));
    }

    @Test
    void getViewStatsFilteredByStartDate() {
        EndpointHit endpointHit = getDefaultEndpointHit();
        endpointHit.setTimestamp(LocalDateTime.parse("2022-01-01 00:00:00", formatter));
        statisticRepository.save(endpointHit);

        List<ViewStatsDto> stat = statisticRepository.getViewStats(
                LocalDateTime.parse("2023-01-01 00:00:00", formatter),
                LocalDateTime.parse("2024-01-01 00:00:00", formatter),
                List.of(),
                true
        );

        assertThat(stat.size(), equalTo(0));
    }

    @Test
    void getViewStatsFilteredByEndDate() {
        EndpointHit endpointHit = getDefaultEndpointHit();
        endpointHit.setTimestamp(LocalDateTime.parse("2023-01-03 00:00:00", formatter));
        statisticRepository.save(endpointHit);

        List<ViewStatsDto> stat = statisticRepository.getViewStats(
                LocalDateTime.parse("2023-01-01 00:00:00", formatter),
                LocalDateTime.parse("2023-01-02 00:00:00", formatter),
                List.of(),
                true
        );

        assertThat(stat.size(), equalTo(0));
    }

    @Test
    void getViewStatsSortByHitsCountDesc() {
        String ip1 = "1.1.1.1";
        String uri1 = "/uri1";
        String ip2 = "2.2.2.2";
        String uri2 = "/uri2";
        EndpointHit firstHit = getEndpointHitWithUriAndIp(uri1, ip1);
        EndpointHit secondHit = getEndpointHitWithUriAndIp(uri1, ip1);
        EndpointHit thirdHit = getEndpointHitWithUriAndIp(uri2, ip2);
        statisticRepository.save(firstHit);
        statisticRepository.save(secondHit);
        statisticRepository.save(thirdHit);

        List<ViewStatsDto> stat = statisticRepository.getViewStats(
                LocalDateTime.parse("2023-01-01 00:00:00", formatter),
                LocalDateTime.parse("2024-01-01 00:00:00", formatter),
                List.of(),
                false
        );

        assertThat(stat.get(0).getHits(), equalTo(2L));
        assertThat(stat.get(1).getHits(), equalTo(1L));
    }

    private EndpointHit getEndpointHitWithUriAndIp(String uri, String ip) {
        LocalDateTime timestamp = LocalDateTime.parse("2023-07-01 12:00:00", formatter);
        return EndpointHit.builder()
                .app("app")
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();
    }

    private EndpointHit getDefaultEndpointHit() {
        LocalDateTime timestamp = LocalDateTime.parse("2023-07-01 12:00:00", formatter);
        return EndpointHit.builder()
                .app("app")
                .uri("/uri")
                .ip("1.1.1.1")
                .timestamp(timestamp)
                .build();
    }
}