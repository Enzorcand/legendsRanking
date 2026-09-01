package portfolio.pucrs.user.dto;

import portfolio.pucrs.riot.entity.Tier;

public record PublicPlayerResponse(
        String nickname,
        boolean ranked,
        Tier tier,
        String division,
        int leaguePoints,
        int wins,
        int losses,
        double winRate,
        String lane,
        Integer mostPlayedChampionId,
        Integer position
) {
}
