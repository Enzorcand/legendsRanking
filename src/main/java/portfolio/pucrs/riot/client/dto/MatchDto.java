package portfolio.pucrs.riot.client.dto;

import java.util.List;

public record MatchDto(MatchMetadataDto metadata, MatchInfoDto info) {

    public record MatchMetadataDto(String matchId) {
    }

    public record MatchInfoDto(
            long gameStartTimestamp,
            long gameDuration,
            int queueId,
            List<ParticipantDto> participants
    ) {
    }

    public record ParticipantDto(
            String puuid,
            int championId,
            String teamPosition,
            boolean win,
            int kills,
            int deaths,
            int assists,
            int totalMinionsKilled,
            int neutralMinionsKilled
    ) {
    }
}
