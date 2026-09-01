package portfolio.pucrs.match.calculator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ChampionCalculator {

    private ChampionCalculator() {
    }

    public static Optional<Integer> mostPlayed(List<Integer> championIds) {
        Map<Integer, Long> countsByChampion = championIds.stream()
                .filter(championId -> championId != null)
                .collect(Collectors.groupingBy(championId -> championId, Collectors.counting()));

        return countsByChampion.entrySet().stream()
                .max(Comparator
                        .comparing(Map.Entry<Integer, Long>::getValue)
                        .thenComparing(entry -> entry.getKey(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey);
    }
}
