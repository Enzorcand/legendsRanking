package portfolio.pucrs.match.dto;

import java.time.Instant;

public record MatchHistoryEntryResponse(
        String matchId,
        Instant gameStart,
        int gameDuration,
        boolean win,
        int championId,
        String lane,
        int kills,
        int deaths,
        int assists,
        int cs
) {
}
