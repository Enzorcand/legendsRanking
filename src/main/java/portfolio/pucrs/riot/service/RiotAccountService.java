package portfolio.pucrs.riot.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final RiotAccountRepository riotAccountRepository;
    private final UserRepository userRepository;
    private final RiotApiClient riotApiClient;

    public RiotAccountService(
            RiotAccountRepository riotAccountRepository,
            UserRepository userRepository,
            RiotApiClient riotApiClient) {
        this.riotAccountRepository = riotAccountRepository;
        this.userRepository = userRepository;
        this.riotApiClient = riotApiClient;
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
