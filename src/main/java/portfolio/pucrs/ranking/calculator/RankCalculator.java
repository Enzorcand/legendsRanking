package portfolio.pucrs.ranking.calculator;

import portfolio.pucrs.riot.entity.RankedStats;

import java.util.Comparator;
import java.util.Map;

public final class RankCalculator {

    private static final Map<String, Integer> DIVISION_RANK = Map.of(
            "IV", 0,
            "III", 1,
            "II", 2,
            "I", 3
    );

    private RankCalculator() {
    }

    /**
     * Best player first: higher tier wins; within the same tier, higher division wins;
     * within the same tier and division, more league points wins.
     */
    public static Comparator<RankedStats> bestFirst() {
        return Comparator
                .comparing((RankedStats stats) -> stats.getTier().ordinal())
                .thenComparing(stats -> divisionRank(stats.getDivision()))
                .thenComparingInt(RankedStats::getLeaguePoints)
                .reversed();
    }

    private static int divisionRank(String division) {
        return DIVISION_RANK.getOrDefault(division, 0);
    }
}
