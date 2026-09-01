package portfolio.pucrs.match.calculator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class RoleCalculator {

    private RoleCalculator() {
    }

    public static Optional<String> mostPlayed(List<String> roles) {
        Map<String, Long> countsByRole = roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .collect(Collectors.groupingBy(role -> role, Collectors.counting()));

        return countsByRole.entrySet().stream()
                .max(Comparator
                        .comparing(Map.Entry<String, Long>::getValue)
                        .thenComparing(entry -> entry.getKey(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey);
    }
}
