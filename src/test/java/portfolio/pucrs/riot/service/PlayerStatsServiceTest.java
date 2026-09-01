package portfolio.pucrs.riot.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import portfolio.pucrs.match.entity.Match;
import portfolio.pucrs.match.repository.MatchRepository;
import portfolio.pucrs.riot.entity.PlayerStats;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.repository.PlayerStatsRepository;
import portfolio.pucrs.season.entity.Season;
import portfolio.pucrs.season.repository.SeasonRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerStatsServiceTest {

    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final PlayerStatsRepository playerStatsRepository = mock(PlayerStatsRepository.class);
    private final SeasonRepository seasonRepository = mock(SeasonRepository.class);
    private final PlayerStatsService playerStatsService =
            new PlayerStatsService(matchRepository, playerStatsRepository, seasonRepository);

    private RiotAccount riotAccount() {
        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setId(1L);
        riotAccount.setPuuid("puuid-1");
        return riotAccount;
    }

    private Season activeSeason() {
        Season season = new Season();
        season.setId(10L);
        season.setName("Temporada Atual");
        season.setStartDate(LocalDate.now().minusMonths(1));
        season.setActive(true);
        return season;
    }

    private Match match(boolean win, int championId, String role) {
        Match match = new Match();
        match.setWin(win);
        match.setChampionId(championId);
        match.setRole(role);
        return match;
    }

    @Test
    void aggregatesStatsFromTheSeasonMatches() {
        RiotAccount riotAccount = riotAccount();
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason()));
        when(matchRepository.findAllByRiotAccountIdAndSeasonId(1L, 10L)).thenReturn(List.of(
                match(true, 103, "MIDDLE"),
                match(true, 103, "MIDDLE"),
                match(false, 22, "MIDDLE")));
        when(playerStatsRepository.findByRiotAccountId(1L)).thenReturn(Optional.empty());
        when(playerStatsRepository.save(any(PlayerStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        playerStatsService.recalculate(riotAccount);

        ArgumentCaptor<PlayerStats> captor = ArgumentCaptor.forClass(PlayerStats.class);
        verify(playerStatsRepository).save(captor.capture());
        PlayerStats saved = captor.getValue();
        assertEquals(3, saved.getTotalGames());
        assertEquals(2, saved.getWins());
        assertEquals(1, saved.getLosses());
        assertEquals(2.0 / 3.0, saved.getWinRate());
        assertEquals("MIDDLE", saved.getPrimaryRole());
        assertEquals(103, saved.getMostPlayedChampionId());
    }

    @Test
    void createsAZeroedPlayerStatsWhenThereAreNoMatchesYet() {
        RiotAccount riotAccount = riotAccount();
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason()));
        when(matchRepository.findAllByRiotAccountIdAndSeasonId(1L, 10L)).thenReturn(List.of());
        when(playerStatsRepository.findByRiotAccountId(1L)).thenReturn(Optional.empty());
        when(playerStatsRepository.save(any(PlayerStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        playerStatsService.recalculate(riotAccount);

        ArgumentCaptor<PlayerStats> captor = ArgumentCaptor.forClass(PlayerStats.class);
        verify(playerStatsRepository).save(captor.capture());
        PlayerStats saved = captor.getValue();
        assertEquals(0, saved.getTotalGames());
        assertEquals(0.0, saved.getWinRate());
        assertNull(saved.getPrimaryRole());
        assertNull(saved.getMostPlayedChampionId());
    }

    @Test
    void updatesTheExistingPlayerStatsInsteadOfCreatingANewRow() {
        RiotAccount riotAccount = riotAccount();
        PlayerStats existing = new PlayerStats();
        existing.setId(99L);
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason()));
        when(matchRepository.findAllByRiotAccountIdAndSeasonId(1L, 10L)).thenReturn(List.of(match(true, 1, "TOP")));
        when(playerStatsRepository.findByRiotAccountId(1L)).thenReturn(Optional.of(existing));
        when(playerStatsRepository.save(any(PlayerStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        playerStatsService.recalculate(riotAccount);

        ArgumentCaptor<PlayerStats> captor = ArgumentCaptor.forClass(PlayerStats.class);
        verify(playerStatsRepository).save(captor.capture());
        assertSame(existing, captor.getValue());
        assertEquals(99L, existing.getId());
    }

    @Test
    void requiresAnActiveSeasonToRecalculate() {
        RiotAccount riotAccount = riotAccount();
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> playerStatsService.recalculate(riotAccount));
    }
}
