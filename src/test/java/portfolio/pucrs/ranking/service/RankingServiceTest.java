package portfolio.pucrs.ranking.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import portfolio.pucrs.ranking.dto.RankingEntryResponse;
import portfolio.pucrs.ranking.dto.RankingFilter;
import portfolio.pucrs.riot.entity.PlayerStats;
import portfolio.pucrs.riot.entity.RankedStats;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.entity.Tier;
import portfolio.pucrs.riot.repository.RiotAccountRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RankingServiceTest {

    private final RiotAccountRepository riotAccountRepository = mock(RiotAccountRepository.class);
    private final RankingService rankingService = new RankingService(riotAccountRepository);

    private RiotAccount account(String gameName, Tier tier, String division, int leaguePoints, int wins, int losses) {
        return account(null, gameName, tier, division, leaguePoints, wins, losses);
    }

    private RiotAccount account(Long id, String gameName, Tier tier, String division, int leaguePoints, int wins, int losses) {
        RankedStats rankedStats = new RankedStats();
        rankedStats.setTier(tier);
        rankedStats.setDivision(division);
        rankedStats.setLeaguePoints(leaguePoints);
        rankedStats.setWins(wins);
        rankedStats.setLosses(losses);

        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setId(id);
        riotAccount.setGameName(gameName);
        riotAccount.setTagLine("BR1");
        riotAccount.setRankedStats(rankedStats);
        return riotAccount;
    }

    @Test
    void ordersByTierThenDivisionThenLeaguePoints() {
        RiotAccount gold = account("Gold", Tier.GOLD, "I", 50, 10, 5);
        RiotAccount diamond = account("Diamond", Tier.DIAMOND, "IV", 0, 10, 5);
        RiotAccount platinum = account("Platinum", Tier.PLATINUM, "I", 80, 10, 5);
        when(riotAccountRepository.findRankingCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(gold, diamond, platinum));

        Page<RankingEntryResponse> page = rankingService.getRanking(emptyFilter(), PageRequest.of(0, 20));

        assertEquals(List.of("Diamond", "Platinum", "Gold"),
                page.getContent().stream().map(RankingEntryResponse::nickname).toList());
    }

    @Test
    void breaksTiesByWinRateThenByWins() {
        RiotAccount higherWinRate = account("Better", Tier.GOLD, "I", 50, 8, 2);
        RiotAccount lowerWinRate = account("Worse", Tier.GOLD, "I", 50, 5, 5);
        when(riotAccountRepository.findRankingCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(lowerWinRate, higherWinRate));

        Page<RankingEntryResponse> page = rankingService.getRanking(emptyFilter(), PageRequest.of(0, 20));

        assertEquals(List.of("Better", "Worse"),
                page.getContent().stream().map(RankingEntryResponse::nickname).toList());
    }

    @Test
    void calculatesThePositionAccordingToThePageOffset() {
        RiotAccount first = account("First", Tier.CHALLENGER, "I", 999, 10, 0);
        RiotAccount second = account("Second", Tier.GRANDMASTER, "I", 500, 10, 0);
        RiotAccount third = account("Third", Tier.MASTER, "I", 100, 10, 0);
        when(riotAccountRepository.findRankingCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(first, second, third));

        Page<RankingEntryResponse> page = rankingService.getRanking(emptyFilter(), PageRequest.of(1, 2));

        assertEquals(1, page.getContent().size());
        assertEquals("Third", page.getContent().get(0).nickname());
        assertEquals(3, page.getContent().get(0).position());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void exposesLaneAndChampionFromPlayerStatsWhenPresent() {
        RiotAccount riotAccount = account("WithStats", Tier.GOLD, "I", 50, 10, 5);
        PlayerStats playerStats = new PlayerStats();
        playerStats.setPrimaryRole("MIDDLE");
        playerStats.setMostPlayedChampionId(103);
        riotAccount.setPlayerStats(playerStats);
        when(riotAccountRepository.findRankingCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(riotAccount));

        Page<RankingEntryResponse> page = rankingService.getRanking(emptyFilter(), PageRequest.of(0, 20));

        RankingEntryResponse entry = page.getContent().get(0);
        assertEquals("MIDDLE", entry.lane());
        assertEquals(103, entry.mostPlayedChampionId());
    }

    @Test
    void returnsNullLaneAndChampionWhenThereIsNoPlayerStatsYet() {
        RiotAccount riotAccount = account("NoStats", Tier.GOLD, "I", 50, 10, 5);
        when(riotAccountRepository.findRankingCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(riotAccount));

        Page<RankingEntryResponse> page = rankingService.getRanking(emptyFilter(), PageRequest.of(0, 20));

        RankingEntryResponse entry = page.getContent().get(0);
        assertEquals(null, entry.lane());
        assertEquals(null, entry.mostPlayedChampionId());
    }

    @Test
    void passesEveryFilterThroughToTheRepositoryQuery() {
        when(riotAccountRepository.findRankingCandidates(any(), any(), any(), any(), any())).thenReturn(List.of());

        RankingFilter filter = new RankingFilter(" raposa ", 1L, "MID", Tier.GOLD, 103);
        rankingService.getRanking(filter, PageRequest.of(0, 20));

        verify(riotAccountRepository).findRankingCandidates(eq("%raposa%"), eq(1L), eq(Tier.GOLD), eq("MID"), eq(103));
    }

    @Test
    void normalizesABlankSearchToAMatchEverythingPattern() {
        when(riotAccountRepository.findRankingCandidates(any(), any(), any(), any(), any())).thenReturn(List.of());

        RankingFilter filter = new RankingFilter("   ", null, null, null, null);
        rankingService.getRanking(filter, PageRequest.of(0, 20));

        verify(riotAccountRepository).findRankingCandidates(eq("%%"), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void findPositionMatchesTheSameOrderingAsGetRanking() {
        RiotAccount first = account(10L, "First", Tier.CHALLENGER, "I", 999, 10, 0);
        RiotAccount second = account(20L, "Second", Tier.GOLD, "I", 50, 10, 0);
        when(riotAccountRepository.findRankingCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(second, first));

        assertEquals(java.util.Optional.of(1), rankingService.findPosition(10L));
        assertEquals(java.util.Optional.of(2), rankingService.findPosition(20L));
    }

    @Test
    void findPositionReturnsEmptyWhenTheAccountIsNotAmongTheCandidates() {
        RiotAccount other = account(10L, "Other", Tier.GOLD, "I", 50, 10, 0);
        when(riotAccountRepository.findRankingCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(other));

        assertEquals(java.util.Optional.empty(), rankingService.findPosition(999L));
    }

    private RankingFilter emptyFilter() {
        return new RankingFilter(null, null, null, null, null);
    }
}
