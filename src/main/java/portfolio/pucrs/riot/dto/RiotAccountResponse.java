package portfolio.pucrs.riot.dto;

import portfolio.pucrs.riot.entity.Region;

public record RiotAccountResponse(String gameName, String tagLine, Region region) {
}
