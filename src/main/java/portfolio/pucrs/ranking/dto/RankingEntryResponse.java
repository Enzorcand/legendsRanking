package portfolio.pucrs.ranking.dto;

import portfolio.pucrs.riot.entity.Tier;

public record RankingEntryResponse(
        int position,
        String nickname,
        Tier tier,
        String division,
        int leaguePoints,
        int wins,
        int losses,
        double winRate,
        String lane,
        Integer mostPlayedChampionId
) {
}
