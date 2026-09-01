package portfolio.pucrs.match.calculator;

public final class WinRateCalculator {

    private WinRateCalculator() {
    }

    public static double calculate(int wins, int losses) {
        int totalGames = wins + losses;
        if (totalGames == 0) {
            return 0.0;
        }
        return (double) wins / totalGames;
    }
}
