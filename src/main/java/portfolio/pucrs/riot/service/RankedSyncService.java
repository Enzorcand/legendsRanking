package portfolio.pucrs.riot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.riot.client.RiotApiClient;
import portfolio.pucrs.riot.client.dto.LeagueEntryDto;
import portfolio.pucrs.riot.entity.RankedStats;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.entity.Tier;
import portfolio.pucrs.riot.repository.RankedStatsRepository;
import portfolio.pucrs.riot.repository.RiotAccountRepository;
import portfolio.pucrs.season.entity.Season;
import portfolio.pucrs.season.repository.SeasonRepository;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.repository.UserRepository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class RankedSyncService {

    private static final String SOLO_DUO_QUEUE = "RANKED_SOLO_5x5";

    private final RiotApiClient riotApiClient;
    private final RankedStatsRepository rankedStatsRepository;
    private final RiotAccountRepository riotAccountRepository;
    private final SeasonRepository seasonRepository;
    private final UserRepository userRepository;

    public RankedSyncService(
            RiotApiClient riotApiClient,
            RankedStatsRepository rankedStatsRepository,
            RiotAccountRepository riotAccountRepository,
            SeasonRepository seasonRepository,
            UserRepository userRepository) {
        this.riotApiClient = riotApiClient;
        this.rankedStatsRepository = rankedStatsRepository;
        this.riotAccountRepository = riotAccountRepository;
        this.seasonRepository = seasonRepository;
        this.userRepository = userRepository;
    }

    public void syncRankedStats(RiotAccount riotAccount) {
        List<LeagueEntryDto> entries = riotApiClient.getLeagueEntriesByPuuid(riotAccount.getPuuid());

        Optional<LeagueEntryDto> soloDuo = entries.stream()
                .filter(entry -> SOLO_DUO_QUEUE.equals(entry.queueType()))
                .findFirst();

        if (soloDuo.isEmpty()) {
            return;
        }

        LeagueEntryDto entry = soloDuo.get();
        Season activeSeason = seasonRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("No active season configured"));

        boolean isFirstTimeRanked = riotAccount.getRankedStats() == null;
        RankedStats rankedStats = isFirstTimeRanked ? new RankedStats() : riotAccount.getRankedStats();

        rankedStats.setSeason(activeSeason);
        rankedStats.setTier(Tier.valueOf(entry.tier().toUpperCase(Locale.ROOT)));
        rankedStats.setDivision(entry.rank());
        rankedStats.setLeaguePoints(entry.leaguePoints());
        rankedStats.setWins(entry.wins());
        rankedStats.setLosses(entry.losses());

        RankedStats saved = rankedStatsRepository.save(rankedStats);
        riotAccount.setRankedStats(saved);
        riotAccountRepository.save(riotAccount);

        if (isFirstTimeRanked) {
            User user = riotAccount.getUser();
            user.setRankingEligible(true);
            userRepository.save(user);
        }
    }
}
