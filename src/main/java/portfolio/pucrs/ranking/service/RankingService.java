package portfolio.pucrs.ranking.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.match.calculator.WinRateCalculator;
import portfolio.pucrs.ranking.calculator.RankCalculator;
import portfolio.pucrs.ranking.dto.RankingEntryResponse;
import portfolio.pucrs.ranking.dto.RankingFilter;
import portfolio.pucrs.riot.entity.PlayerStats;
import portfolio.pucrs.riot.entity.RankedStats;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.repository.RiotAccountRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RankingService {

    private final RiotAccountRepository riotAccountRepository;

    public RankingService(RiotAccountRepository riotAccountRepository) {
        this.riotAccountRepository = riotAccountRepository;
    }

    public Page<RankingEntryResponse> getRanking(RankingFilter filter, Pageable pageable) {
        List<RiotAccount> candidates = fetchSortedCandidates(filter);

        int total = candidates.size();
        int fromIndex = Math.min((int) pageable.getOffset(), total);
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        List<RiotAccount> pageSlice = candidates.subList(fromIndex, toIndex);

        List<RankingEntryResponse> content = new ArrayList<>();
        for (int i = 0; i < pageSlice.size(); i++) {
            content.add(toResponse(fromIndex + i + 1, pageSlice.get(i)));
        }

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Same ranking used by {@link #getRanking}, unfiltered and unpaged, to find where a
     * specific RiotAccount currently lands — used by player profiles (etapa 15).
     */
    public Optional<Integer> findPosition(Long riotAccountId) {
        List<RiotAccount> candidates = fetchSortedCandidates(
                new RankingFilter(null, null, null, null, null));

        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).getId().equals(riotAccountId)) {
                return Optional.of(i + 1);
            }
        }
        return Optional.empty();
    }

    private List<RiotAccount> fetchSortedCandidates(RankingFilter filter) {
        List<RiotAccount> candidates = new ArrayList<>(riotAccountRepository.findRankingCandidates(
                searchPattern(filter.search()),
                filter.courseId(),
                filter.tier(),
                filter.role(),
                filter.championId()));

        candidates.sort(rankingComparator());
        return candidates;
    }

    private Comparator<RiotAccount> rankingComparator() {
        return Comparator
                .comparing(RiotAccount::getRankedStats, RankCalculator.bestFirst())
                .thenComparing(Comparator.comparingDouble(this::winRate).reversed())
                .thenComparing(Comparator.comparingInt((RiotAccount ra) -> ra.getRankedStats().getWins()).reversed());
    }

    private double winRate(RiotAccount riotAccount) {
        RankedStats rankedStats = riotAccount.getRankedStats();
        return WinRateCalculator.calculate(rankedStats.getWins(), rankedStats.getLosses());
    }

    private RankingEntryResponse toResponse(int position, RiotAccount riotAccount) {
        RankedStats rankedStats = riotAccount.getRankedStats();
        PlayerStats playerStats = riotAccount.getPlayerStats();

        return new RankingEntryResponse(
                position,
                riotAccount.getGameName(),
                rankedStats.getTier(),
                rankedStats.getDivision(),
                rankedStats.getLeaguePoints(),
                rankedStats.getWins(),
                rankedStats.getLosses(),
                winRate(riotAccount),
                playerStats != null ? playerStats.getPrimaryRole() : null,
                playerStats != null ? playerStats.getMostPlayedChampionId() : null);
    }

    private String searchPattern(String search) {
        String normalized = (search == null || search.isBlank()) ? "" : search.trim();
        return "%" + normalized + "%";
    }
}
