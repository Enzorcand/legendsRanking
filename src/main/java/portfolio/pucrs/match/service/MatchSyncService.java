package portfolio.pucrs.match.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.match.entity.Match;
import portfolio.pucrs.match.repository.MatchRepository;
import portfolio.pucrs.riot.client.RiotApiClient;
import portfolio.pucrs.riot.client.dto.MatchDto;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.season.entity.Season;
import portfolio.pucrs.season.repository.SeasonRepository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MatchSyncService {

    private static final int SOLO_DUO_QUEUE_ID = 420;
    private static final String SOLO_DUO_QUEUE_TYPE = "RANKED_SOLO_5x5";
    private static final int MATCHES_PER_SYNC = 100;

    private final RiotApiClient riotApiClient;
    private final MatchRepository matchRepository;
    private final SeasonRepository seasonRepository;

    public MatchSyncService(RiotApiClient riotApiClient, MatchRepository matchRepository, SeasonRepository seasonRepository) {
        this.riotApiClient = riotApiClient;
        this.matchRepository = matchRepository;
        this.seasonRepository = seasonRepository;
    }

    public void syncMatches(RiotAccount riotAccount) {
        Season activeSeason = seasonRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("No active season configured"));

        long seasonStartEpochSeconds = activeSeason.getStartDate().atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        List<String> matchIds = riotApiClient.getMatchIdsByPuuid(
                riotAccount.getPuuid(), 0, MATCHES_PER_SYNC, SOLO_DUO_QUEUE_ID, seasonStartEpochSeconds);

        for (String matchId : matchIds) {
            if (matchRepository.existsByRiotMatchId(matchId)) {
                break;
            }
            saveMatchIfRelevant(matchId, riotAccount, activeSeason);
        }
    }

    private void saveMatchIfRelevant(String matchId, RiotAccount riotAccount, Season season) {
        MatchDto matchDto = riotApiClient.getMatchById(matchId);

        if (matchDto.info().queueId() != SOLO_DUO_QUEUE_ID) {
            return;
        }

        Optional<MatchDto.ParticipantDto> participant = matchDto.info().participants().stream()
                .filter(p -> riotAccount.getPuuid().equals(p.puuid()))
                .findFirst();

        if (participant.isEmpty()) {
            return;
        }

        Match match = toMatch(matchDto, participant.get(), riotAccount, season);

        try {
            matchRepository.save(match);
        } catch (DataIntegrityViolationException e) {
            // already synced concurrently; safe to ignore
        }
    }

    private Match toMatch(MatchDto matchDto, MatchDto.ParticipantDto participant, RiotAccount riotAccount, Season season) {
        Match match = new Match();
        match.setRiotMatchId(matchDto.metadata().matchId());
        match.setRiotAccount(riotAccount);
        match.setSeason(season);
        match.setGameStart(Instant.ofEpochMilli(matchDto.info().gameStartTimestamp()));
        match.setGameDuration((int) matchDto.info().gameDuration());
        match.setQueueType(SOLO_DUO_QUEUE_TYPE);
        match.setWin(participant.win());
        match.setChampionId(participant.championId());
        match.setRole(participant.teamPosition());
        match.setKills(participant.kills());
        match.setDeaths(participant.deaths());
        match.setAssists(participant.assists());
        match.setCs(participant.totalMinionsKilled() + participant.neutralMinionsKilled());
        return match;
    }
}
