package portfolio.pucrs.match.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import portfolio.pucrs.match.entity.Match;
import portfolio.pucrs.match.repository.MatchRepository;
import portfolio.pucrs.riot.client.RiotApiClient;
import portfolio.pucrs.riot.client.dto.MatchDto;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.season.entity.Season;
import portfolio.pucrs.season.repository.SeasonRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchSyncServiceTest {

    private final RiotApiClient riotApiClient = mock(RiotApiClient.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final SeasonRepository seasonRepository = mock(SeasonRepository.class);
    private final MatchSyncService matchSyncService =
            new MatchSyncService(riotApiClient, matchRepository, seasonRepository);

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
        season.setStartDate(LocalDate.now().minusMonths(1));
        season.setActive(true);
        return season;
    }

    private MatchDto soloDuoMatch(String matchId, boolean win) {
        MatchDto.ParticipantDto participant = new MatchDto.ParticipantDto(
                "puuid-1", 103, "MIDDLE", win, 8, 2, 10, 180, 5);
        MatchDto.MatchInfoDto info = new MatchDto.MatchInfoDto(1700000000000L, 1800, 420, List.of(participant));
        return new MatchDto(new MatchDto.MatchMetadataDto(matchId), info);
    }

    @Test
    void savesANewSoloDuoMatchWithTheParticipantData() {
        RiotAccount riotAccount = riotAccount();
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason()));
        when(riotApiClient.getMatchIdsByPuuid(eq("puuid-1"), anyInt(), anyInt(), anyInt(), anyLong()))
                .thenReturn(List.of("BR1_111"));
        when(matchRepository.existsByRiotMatchId("BR1_111")).thenReturn(false);
        when(riotApiClient.getMatchById("BR1_111")).thenReturn(soloDuoMatch("BR1_111", true));

        matchSyncService.syncMatches(riotAccount);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());
        Match saved = captor.getValue();
        assertEquals("BR1_111", saved.getRiotMatchId());
        assertEquals(103, saved.getChampionId());
        assertEquals("MIDDLE", saved.getRole());
        assertEquals(8, saved.getKills());
        assertEquals(2, saved.getDeaths());
        assertEquals(10, saved.getAssists());
        assertEquals(185, saved.getCs());
        assertTrue(saved.isWin());
    }

    @Test
    void ignoresMatchesThatAreNotRankedSoloDuo() {
        RiotAccount riotAccount = riotAccount();
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason()));
        when(riotApiClient.getMatchIdsByPuuid(eq("puuid-1"), anyInt(), anyInt(), anyInt(), anyLong()))
                .thenReturn(List.of("BR1_222"));
        when(matchRepository.existsByRiotMatchId("BR1_222")).thenReturn(false);

        MatchDto.ParticipantDto participant = new MatchDto.ParticipantDto(
                "puuid-1", 103, "MIDDLE", true, 8, 2, 10, 180, 5);
        MatchDto.MatchInfoDto flexInfo = new MatchDto.MatchInfoDto(1700000000000L, 1800, 440, List.of(participant));
        when(riotApiClient.getMatchById("BR1_222")).thenReturn(new MatchDto(new MatchDto.MatchMetadataDto("BR1_222"), flexInfo));

        matchSyncService.syncMatches(riotAccount);

        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void stopsAtTheFirstAlreadyKnownMatch() {
        RiotAccount riotAccount = riotAccount();
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason()));
        when(riotApiClient.getMatchIdsByPuuid(eq("puuid-1"), anyInt(), anyInt(), anyInt(), anyLong()))
                .thenReturn(List.of("BR1_NEW", "BR1_OLD_KNOWN", "BR1_OLDER"));
        when(matchRepository.existsByRiotMatchId("BR1_NEW")).thenReturn(false);
        when(matchRepository.existsByRiotMatchId("BR1_OLD_KNOWN")).thenReturn(true);
        when(riotApiClient.getMatchById("BR1_NEW")).thenReturn(soloDuoMatch("BR1_NEW", true));

        matchSyncService.syncMatches(riotAccount);

        verify(riotApiClient).getMatchById("BR1_NEW");
        verify(riotApiClient, never()).getMatchById("BR1_OLD_KNOWN");
        verify(riotApiClient, never()).getMatchById("BR1_OLDER");
        verify(matchRepository, times(1)).save(any(Match.class));
    }

    @Test
    void doesNothingWhenThereAreNoMatchIds() {
        RiotAccount riotAccount = riotAccount();
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason()));
        when(riotApiClient.getMatchIdsByPuuid(eq("puuid-1"), anyInt(), anyInt(), anyInt(), anyLong()))
                .thenReturn(List.of());

        matchSyncService.syncMatches(riotAccount);

        verify(matchRepository, never()).save(any(Match.class));
        verify(riotApiClient, never()).getMatchById(anyString());
    }

    @Test
    void requiresAnActiveSeasonToSync() {
        RiotAccount riotAccount = riotAccount();
        when(seasonRepository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> matchSyncService.syncMatches(riotAccount));
    }
}
