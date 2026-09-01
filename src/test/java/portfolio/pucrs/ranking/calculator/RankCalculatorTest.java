package portfolio.pucrs.ranking.calculator;

import org.junit.jupiter.api.Test;
import portfolio.pucrs.riot.entity.RankedStats;
import portfolio.pucrs.riot.entity.Tier;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RankCalculatorTest {

    private RankedStats stats(Tier tier, String division, int leaguePoints) {
        RankedStats rankedStats = new RankedStats();
        rankedStats.setTier(tier);
        rankedStats.setDivision(division);
        rankedStats.setLeaguePoints(leaguePoints);
        return rankedStats;
    }

    @Test
    void higherTierWinsRegardlessOfDivisionOrLeaguePoints() {
        RankedStats better = stats(Tier.DIAMOND, "IV", 0);
        RankedStats worse = stats(Tier.GOLD, "I", 100);

        Comparator<RankedStats> comparator = RankCalculator.bestFirst();

        assertTrue(comparator.compare(better, worse) < 0);
    }

    @Test
    void sameTierHigherDivisionWins() {
        RankedStats better = stats(Tier.GOLD, "I", 0);
        RankedStats worse = stats(Tier.GOLD, "IV", 100);

        Comparator<RankedStats> comparator = RankCalculator.bestFirst();

        assertTrue(comparator.compare(better, worse) < 0);
    }

    @Test
    void sameTierAndDivisionMoreLeaguePointsWins() {
        RankedStats better = stats(Tier.GOLD, "II", 60);
        RankedStats worse = stats(Tier.GOLD, "II", 10);

        Comparator<RankedStats> comparator = RankCalculator.bestFirst();

        assertTrue(comparator.compare(better, worse) < 0);
    }

    @Test
    void comparesApexTiersByTierThenLeaguePoints() {
        RankedStats challenger = stats(Tier.CHALLENGER, "I", 500);
        RankedStats grandmaster = stats(Tier.GRANDMASTER, "I", 999);
        RankedStats master = stats(Tier.MASTER, "I", 200);

        Comparator<RankedStats> comparator = RankCalculator.bestFirst();

        assertTrue(comparator.compare(challenger, grandmaster) < 0);
        assertTrue(comparator.compare(grandmaster, master) < 0);
    }
}
