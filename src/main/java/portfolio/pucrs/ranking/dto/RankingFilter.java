package portfolio.pucrs.ranking.dto;

import portfolio.pucrs.riot.entity.Tier;

public record RankingFilter(String search, Long courseId, String role, Tier tier, Integer championId) {
}
