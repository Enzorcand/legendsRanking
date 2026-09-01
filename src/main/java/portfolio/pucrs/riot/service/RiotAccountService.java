package portfolio.pucrs.riot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.match.service.MatchSyncService;
import portfolio.pucrs.riot.client.RiotApiClient;
import portfolio.pucrs.riot.client.dto.RiotAccountDto;
import portfolio.pucrs.riot.dto.LinkRiotAccountRequest;
import portfolio.pucrs.riot.dto.RiotAccountResponse;
import portfolio.pucrs.riot.entity.PlayerStats;
import portfolio.pucrs.riot.entity.RankedStats;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.exception.DuplicateRiotAccountException;
import portfolio.pucrs.riot.exception.RiotAccountAlreadyLinkedException;
import portfolio.pucrs.riot.exception.RiotAccountNotLinkedException;
import portfolio.pucrs.riot.exception.SyncCooldownException;
import portfolio.pucrs.riot.repository.PlayerStatsRepository;
import portfolio.pucrs.riot.repository.RankedStatsRepository;
import portfolio.pucrs.riot.repository.RiotAccountRepository;
import portfolio.pucrs.user.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Transactional
public class RiotAccountService {

    private static final Logger log = LoggerFactory.getLogger(RiotAccountService.class);
    private static final Duration SYNC_COOLDOWN = Duration.ofMinutes(2);

    private final RiotAccountRepository riotAccountRepository;
    private final RankedStatsRepository rankedStatsRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final UserRepository userRepository;
    private final RiotApiClient riotApiClient;
    private final RankedSyncService rankedSyncService;
    private final MatchSyncService matchSyncService;
    private final PlayerStatsService playerStatsService;

    public RiotAccountService(
            RiotAccountRepository riotAccountRepository,
            RankedStatsRepository rankedStatsRepository,
            PlayerStatsRepository playerStatsRepository,
            UserRepository userRepository,
            RiotApiClient riotApiClient,
            RankedSyncService rankedSyncService,
            MatchSyncService matchSyncService,
            PlayerStatsService playerStatsService) {
        this.riotAccountRepository = riotAccountRepository;
        this.rankedStatsRepository = rankedStatsRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.userRepository = userRepository;
        this.riotApiClient = riotApiClient;
        this.rankedSyncService = rankedSyncService;
        this.matchSyncService = matchSyncService;
        this.playerStatsService = playerStatsService;
    }

    public RiotAccountResponse linkAccount(Long userId, LinkRiotAccountRequest request) {
        if (riotAccountRepository.existsByUserId(userId)) {
            throw new RiotAccountAlreadyLinkedException();
        }

        RiotAccountDto accountDto = riotApiClient.getAccountByRiotId(request.gameName(), request.tagLine());

        if (riotAccountRepository.existsByPuuid(accountDto.puuid())) {
            throw new DuplicateRiotAccountException();
        }

        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setUser(userRepository.getReferenceById(userId));
        riotAccount.setPuuid(accountDto.puuid());
        riotAccount.setGameName(accountDto.gameName());
        riotAccount.setTagLine(accountDto.tagLine());
        riotAccount.setRegion(request.region());

        RiotAccount saved;
        try {
            saved = riotAccountRepository.save(riotAccount);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRiotAccountException();
        }

        try {
            rankedSyncService.syncRankedStats(saved);
        } catch (RuntimeException e) {
            log.warn("Initial ranked sync failed for riot account {}: {}", saved.getId(), e.getMessage());
        }

        try {
            matchSyncService.syncMatches(saved);
        } catch (RuntimeException e) {
            log.warn("Initial match sync failed for riot account {}: {}", saved.getId(), e.getMessage());
        }

        try {
            playerStatsService.recalculate(saved);
        } catch (RuntimeException e) {
            log.warn("Initial player stats recalculation failed for riot account {}: {}", saved.getId(), e.getMessage());
        }

        return toResponse(saved);
    }

    public RiotAccountResponse syncAccount(Long userId) {
        RiotAccount riotAccount = riotAccountRepository.findByUserId(userId)
                .orElseThrow(RiotAccountNotLinkedException::new);

        LocalDateTime now = LocalDateTime.now();
        if (riotAccount.getLastSyncedAt() != null) {
            Duration elapsed = Duration.between(riotAccount.getLastSyncedAt(), now);
            if (elapsed.compareTo(SYNC_COOLDOWN) < 0) {
                throw new SyncCooldownException(SYNC_COOLDOWN.minus(elapsed).toSeconds() + 1);
            }
        }

        // lastSyncedAt is stamped before the syncs run, so a Riot API failure still starts the
        // cooldown — otherwise a failing sync could be retried in a tight loop against the API.
        riotAccount.setLastSyncedAt(now);
        riotAccountRepository.save(riotAccount);

        rankedSyncService.syncRankedStats(riotAccount);
        matchSyncService.syncMatches(riotAccount);
        playerStatsService.recalculate(riotAccount);

        return toResponse(riotAccount);
    }

    public void unlinkAccount(Long userId) {
        RiotAccount riotAccount = riotAccountRepository.findByUserId(userId)
                .orElseThrow(RiotAccountNotLinkedException::new);

        RankedStats rankedStats = riotAccount.getRankedStats();
        PlayerStats playerStats = riotAccount.getPlayerStats();

        // PlayerStats is deleted explicitly (and detached first) instead of relying only on
        // ON DELETE CASCADE: Hibernate's flush-time transient-reference check walks the
        // bidirectional RiotAccount.playerStats mapping and errors out if a PlayerStats row
        // still points at a RiotAccount that is queued for deletion.
        if (playerStats != null) {
            riotAccount.setPlayerStats(null);
            playerStatsRepository.delete(playerStats);
        }

        // Matches are removed by ON DELETE CASCADE (see V8 migration) — there is no
        // bidirectional mapping from RiotAccount to Match, so Hibernate never loads them
        // and the check above doesn't apply.
        // RankedStats is referenced the other way around (riot_accounts.ranked_stats_id),
        // so it has to be deleted explicitly, after the RiotAccount row stops pointing to it.
        riotAccountRepository.delete(riotAccount);
        riotAccountRepository.flush();

        if (rankedStats != null) {
            rankedStatsRepository.delete(rankedStats);
        }
    }

    private RiotAccountResponse toResponse(RiotAccount riotAccount) {
        return new RiotAccountResponse(riotAccount.getGameName(), riotAccount.getTagLine(), riotAccount.getRegion());
    }
}
