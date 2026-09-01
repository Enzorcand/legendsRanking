package portfolio.pucrs.riot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.match.calculator.ChampionCalculator;
import portfolio.pucrs.match.calculator.RoleCalculator;
import portfolio.pucrs.match.calculator.WinRateCalculator;
import portfolio.pucrs.match.entity.Match;
import portfolio.pucrs.match.repository.MatchRepository;
import portfolio.pucrs.riot.entity.PlayerStats;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.repository.PlayerStatsRepository;
import portfolio.pucrs.season.entity.Season;
import portfolio.pucrs.season.repository.SeasonRepository;

import java.util.List;

@Service
@Transactional
public class PlayerStatsService {

    private final MatchRepository matchRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final SeasonRepository seasonRepository;

    public PlayerStatsService(
            MatchRepository matchRepository,
            PlayerStatsRepository playerStatsRepository,
            SeasonRepository seasonRepository) {
        this.matchRepository = matchRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.seasonRepository = seasonRepository;
    }

    public void recalculate(RiotAccount riotAccount) {
        Season activeSeason = seasonRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("No active season configured"));

        List<Match> matches = matchRepository.findAllByRiotAccountIdAndSeasonId(riotAccount.getId(), activeSeason.getId());

        int wins = (int) matches.stream().filter(Match::isWin).count();
        int losses = matches.size() - wins;
        double winRate = WinRateCalculator.calculate(wins, losses);
        String primaryRole = RoleCalculator.mostPlayed(matches.stream().map(Match::getRole).toList()).orElse(null);
        Integer mostPlayedChampionId = ChampionCalculator
                .mostPlayed(matches.stream().map(Match::getChampionId).toList())
                .orElse(null);

        PlayerStats playerStats = playerStatsRepository.findByRiotAccountId(riotAccount.getId())
                .orElseGet(PlayerStats::new);
        playerStats.setRiotAccount(riotAccount);
        playerStats.setSeason(activeSeason);
        playerStats.setTotalGames(matches.size());
        playerStats.setWins(wins);
        playerStats.setLosses(losses);
        playerStats.setWinRate(winRate);
        playerStats.setPrimaryRole(primaryRole);
        playerStats.setMostPlayedChampionId(mostPlayedChampionId);

        playerStatsRepository.save(playerStats);
    }
}
