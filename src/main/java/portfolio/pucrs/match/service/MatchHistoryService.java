package portfolio.pucrs.match.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.match.dto.MatchHistoryEntryResponse;
import portfolio.pucrs.match.entity.Match;
import portfolio.pucrs.match.repository.MatchRepository;

@Service
@Transactional(readOnly = true)
public class MatchHistoryService {

    private final MatchRepository matchRepository;

    public MatchHistoryService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public Page<MatchHistoryEntryResponse> getHistory(Long riotAccountId, Pageable pageable) {
        return matchRepository.findAllByRiotAccountIdOrderByGameStartDesc(riotAccountId, pageable)
                .map(this::toResponse);
    }

    private MatchHistoryEntryResponse toResponse(Match match) {
        return new MatchHistoryEntryResponse(
                match.getRiotMatchId(),
                match.getGameStart(),
                match.getGameDuration(),
                match.isWin(),
                match.getChampionId(),
                match.getRole(),
                match.getKills(),
                match.getDeaths(),
                match.getAssists(),
                match.getCs());
    }
}
