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
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.exception.DuplicateRiotAccountException;
import portfolio.pucrs.riot.exception.RiotAccountAlreadyLinkedException;
import portfolio.pucrs.riot.exception.RiotAccountNotLinkedException;
import portfolio.pucrs.riot.repository.RiotAccountRepository;
import portfolio.pucrs.user.repository.UserRepository;

@Service
@Transactional
public class RiotAccountService {

    private static final Logger log = LoggerFactory.getLogger(RiotAccountService.class);

    private final RiotAccountRepository riotAccountRepository;
    private final UserRepository userRepository;
    private final RiotApiClient riotApiClient;
    private final RankedSyncService rankedSyncService;
    private final MatchSyncService matchSyncService;

    public RiotAccountService(
            RiotAccountRepository riotAccountRepository,
            UserRepository userRepository,
            RiotApiClient riotApiClient,
            RankedSyncService rankedSyncService,
            MatchSyncService matchSyncService) {
        this.riotAccountRepository = riotAccountRepository;
        this.userRepository = userRepository;
        this.riotApiClient = riotApiClient;
        this.rankedSyncService = rankedSyncService;
        this.matchSyncService = matchSyncService;
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

        return toResponse(saved);
    }

    public void unlinkAccount(Long userId) {
        RiotAccount riotAccount = riotAccountRepository.findByUserId(userId)
                .orElseThrow(RiotAccountNotLinkedException::new);
        riotAccountRepository.delete(riotAccount);
    }

    private RiotAccountResponse toResponse(RiotAccount riotAccount) {
        return new RiotAccountResponse(riotAccount.getGameName(), riotAccount.getTagLine(), riotAccount.getRegion());
    }
}
