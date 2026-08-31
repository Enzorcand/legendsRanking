package portfolio.pucrs.riot.client.dto;

public record LeagueEntryDto(
        String puuid,
        String queueType,
        String tier,
        String rank,
        int leaguePoints,
        int wins,
        int losses
) {
}
