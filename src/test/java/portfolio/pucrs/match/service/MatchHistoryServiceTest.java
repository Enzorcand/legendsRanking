package portfolio.pucrs.match.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import portfolio.pucrs.match.dto.MatchHistoryEntryResponse;
import portfolio.pucrs.match.entity.Match;
import portfolio.pucrs.match.repository.MatchRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchHistoryServiceTest {

    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final MatchHistoryService matchHistoryService = new MatchHistoryService(matchRepository);

    private Match match(String riotMatchId, Instant gameStart) {
        Match match = new Match();
        match.setRiotMatchId(riotMatchId);
        match.setGameStart(gameStart);
        match.setGameDuration(1800);
        match.setWin(true);
        match.setChampionId(103);
        match.setRole("MIDDLE");
        match.setKills(8);
        match.setDeaths(2);
        match.setAssists(10);
        match.setCs(180);
        return match;
    }

    @Test
    void returnsThePagedAndConvertedMatchHistory() {
        Match newest = match("BR1_2", Instant.parse("2026-01-02T00:00:00Z"));
        Pageable pageable = PageRequest.of(0, 20);
        when(matchRepository.findAllByRiotAccountIdOrderByGameStartDesc(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(newest)));

        Page<MatchHistoryEntryResponse> page = matchHistoryService.getHistory(5L, pageable);

        assertEquals(1, page.getTotalElements());
        MatchHistoryEntryResponse entry = page.getContent().get(0);
        assertEquals("BR1_2", entry.matchId());
        assertEquals(103, entry.championId());
        assertEquals("MIDDLE", entry.lane());
        assertEquals(8, entry.kills());
        assertTrue(entry.win());
    }

    @Test
    void returnsAnEmptyPageWhenTheAccountHasNoMatches() {
        Pageable pageable = PageRequest.of(0, 20);
        when(matchRepository.findAllByRiotAccountIdOrderByGameStartDesc(5L, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<MatchHistoryEntryResponse> page = matchHistoryService.getHistory(5L, pageable);

        assertTrue(page.getContent().isEmpty());
    }
}
