package portfolio.pucrs.match.calculator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleCalculatorTest {

    @Test
    void returnsTheMostFrequentRole() {
        List<String> roles = List.of("MIDDLE", "TOP", "MIDDLE", "JUNGLE", "MIDDLE");

        assertEquals(Optional.of("MIDDLE"), RoleCalculator.mostPlayed(roles));
    }

    @Test
    void breaksTiesAlphabetically() {
        List<String> roles = List.of("TOP", "JUNGLE", "TOP", "JUNGLE");

        assertEquals(Optional.of("JUNGLE"), RoleCalculator.mostPlayed(roles));
    }

    @Test
    void ignoresNullAndBlankRoles() {
        List<String> roles = java.util.Arrays.asList("TOP", null, "", "TOP");

        assertEquals(Optional.of("TOP"), RoleCalculator.mostPlayed(roles));
    }

    @Test
    void returnsEmptyWhenThereAreNoRoles() {
        assertEquals(Optional.empty(), RoleCalculator.mostPlayed(List.of()));
    }
}
