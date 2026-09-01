package portfolio.pucrs.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.match.calculator.WinRateCalculator;
import portfolio.pucrs.ranking.service.RankingService;
import portfolio.pucrs.riot.entity.PlayerStats;
import portfolio.pucrs.riot.entity.RankedStats;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.user.dto.AuthenticatedPlayerResponse;
import portfolio.pucrs.user.dto.PublicPlayerResponse;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.exception.PlayerNotFoundException;
import portfolio.pucrs.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class PlayerProfileService {

    private final UserRepository userRepository;
    private final RankingService rankingService;

    public PlayerProfileService(UserRepository userRepository, RankingService rankingService) {
        this.userRepository = userRepository;
        this.rankingService = rankingService;
    }

    public PublicPlayerResponse getPublicProfile(Long id) {
        return toPublicResponse(getViewableUser(id));
    }

    public RiotAccount getViewableRiotAccount(Long id) {
        return getViewableUser(id).getRiotAccount();
    }

    public AuthenticatedPlayerResponse getAuthenticatedProfile(Long id) {
        User user = getViewableUser(id);
        RiotAccount riotAccount = user.getRiotAccount();

        return new AuthenticatedPlayerResponse(
                user.getId(),
                user.getFullName(),
                riotAccount.getTagLine(),
                user.getCourse().getName(),
                toPublicResponse(user));
    }

    private User getViewableUser(Long id) {
        User user = userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new PlayerNotFoundException(id));

        if (user.getRiotAccount() == null) {
            throw new PlayerNotFoundException(id);
        }

        return user;
    }

    private PublicPlayerResponse toPublicResponse(User user) {
        RiotAccount riotAccount = user.getRiotAccount();
        RankedStats rankedStats = riotAccount.getRankedStats();
        PlayerStats playerStats = riotAccount.getPlayerStats();
        boolean ranked = rankedStats != null;

        Integer position = ranked ? rankingService.findPosition(riotAccount.getId()).orElse(null) : null;

        return new PublicPlayerResponse(
                riotAccount.getGameName(),
                ranked,
                ranked ? rankedStats.getTier() : null,
                ranked ? rankedStats.getDivision() : null,
                ranked ? rankedStats.getLeaguePoints() : 0,
                ranked ? rankedStats.getWins() : 0,
                ranked ? rankedStats.getLosses() : 0,
                ranked ? WinRateCalculator.calculate(rankedStats.getWins(), rankedStats.getLosses()) : 0.0,
                playerStats != null ? playerStats.getPrimaryRole() : null,
                playerStats != null ? playerStats.getMostPlayedChampionId() : null,
                position);
    }
}
