package portfolio.pucrs.riot.service;

import org.junit.jupiter.api.Test;
import portfolio.pucrs.riot.client.RiotApiClient;
import portfolio.pucrs.riot.client.dto.LeagueEntryDto;
import portfolio.pucrs.riot.entity.RankedStats;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.entity.Tier;
import portfolio.pucrs.riot.repository.RankedStatsRepository;
import portfolio.pucrs.riot.repository.RiotAccountRepository;
import portfolio.pucrs.season.entity.Season;
import portfolio.pucrs.season.repository.SeasonRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RankedSyncServiceTest {

    private final RiotApiClient riotApiClient = mock(RiotApiClient.class);
    private final RankedStatsRepository rankedStatsRepository = mock(RankedStatsRepository.class);
    private final RiotAccountRepository riotAccountRepository = mock(RiotAccountRepository.class);
    private final SeasonRepository seasonRepository = mock(SeasonRepository.class);
    private final RankedSyncService rankedSyncService =
            new RankedSyncService(riotApiClient, rankedStatsRepository, riotAccountRepository, seasonRepository);

    private RiotAccount riotAccount() {
        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setId(1L);
        riotAccount.setPuuid("puuid-1");
        return riotAccount;
    }

    private Season activeSeason() {
        Season season = new Season();
        season.setId(1L);
        season.setName("Temporada Atual");
        season.setStartDate(LocalDate.now());
        season.setActive(true);
        return season;
    }

    @Test
    void createsRankedStatsWhenThereIsASoloDuoEntry() {
        RiotAccount riotAccount = riotAccount();
        when(riotApiClient.getLeagueEntriesByPuuid("puuid-1")).thenReturn(List.of(
                new LeagueEntryDto("puuid-1", "RANKED_SOLO_5x5", "GOLD", "II", 55, 40, 30)));
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason()));
        when(rankedStatsRepository.save(any(RankedStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rankedSyncService.syncRankedStats(riotAccount);

        assertEquals(Tier.GOLD, riotAccount.getRankedStats().getTier());
        assertEquals("II", riotAccount.getRankedStats().getDivision());
        assertEquals(55, riotAccount.getRankedStats().getLeaguePoints());
        assertEquals(40, riotAccount.getRankedStats().getWins());
        assertEquals(30, riotAccount.getRankedStats().getLosses());
        verify(riotAccountRepository).save(riotAccount);
    }

    @Test
    void ignoresEntriesFromOtherQueues() {
        RiotAccount riotAccount = riotAccount();
        when(riotApiClient.getLeagueEntriesByPuuid("puuid-1")).thenReturn(List.of(
                new LeagueEntryDto("puuid-1", "RANKED_FLEX_SR", "GOLD", "II", 55, 40, 30)));

        rankedSyncService.syncRankedStats(riotAccount);

        assertNull(riotAccount.getRankedStats());
        verify(rankedStatsRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenThePlayerIsUnranked() {
        RiotAccount riotAccount = riotAccount();
        when(riotApiClient.getLeagueEntriesByPuuid("puuid-1")).thenReturn(List.of());

        rankedSyncService.syncRankedStats(riotAccount);

        assertNull(riotAccount.getRankedStats());
        verify(rankedStatsRepository, never()).save(any());
    }

    @Test
    void updatesTheExistingRankedStatsInsteadOfCreatingANewRow() {
        RiotAccount riotAccount = riotAccount();
        RankedStats existing = new RankedStats();
        existing.setId(9L);
        riotAccount.setRankedStats(existing);

        when(riotApiClient.getLeagueEntriesByPuuid("puuid-1")).thenReturn(List.of(
                new LeagueEntryDto("puuid-1", "RANKED_SOLO_5x5", "PLATINUM", "I", 10, 50, 45)));
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason()));
        when(rankedStatsRepository.save(any(RankedStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rankedSyncService.syncRankedStats(riotAccount);

        assertSame(existing, riotAccount.getRankedStats());
        assertEquals(Tier.PLATINUM, existing.getTier());
        assertEquals(9L, existing.getId());
    }
}
