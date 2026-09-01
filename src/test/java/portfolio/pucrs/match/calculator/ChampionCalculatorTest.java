package portfolio.pucrs.match.calculator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChampionCalculatorTest {

    @Test
    void returnsTheMostFrequentChampion() {
        List<Integer> championIds = List.of(103, 22, 103, 45, 103);

        assertEquals(Optional.of(103), ChampionCalculator.mostPlayed(championIds));
    }

    @Test
    void breaksTiesByTheLowestChampionId() {
        List<Integer> championIds = List.of(103, 22, 103, 22);

        assertEquals(Optional.of(22), ChampionCalculator.mostPlayed(championIds));
    }

    @Test
    void returnsEmptyWhenThereAreNoChampions() {
        assertEquals(Optional.empty(), ChampionCalculator.mostPlayed(List.of()));
    }
}
