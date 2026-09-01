package portfolio.pucrs.match.calculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WinRateCalculatorTest {

    @Test
    void returnsZeroWhenThereAreNoGames() {
        assertEquals(0.0, WinRateCalculator.calculate(0, 0));
    }

    @Test
    void calculatesTheProportionOfWins() {
        assertEquals(0.75, WinRateCalculator.calculate(3, 1));
    }

    @Test
    void returnsOneWhenAllGamesAreWins() {
        assertEquals(1.0, WinRateCalculator.calculate(5, 0));
    }

    @Test
    void returnsZeroWhenAllGamesAreLosses() {
        assertEquals(0.0, WinRateCalculator.calculate(0, 5));
    }
}
