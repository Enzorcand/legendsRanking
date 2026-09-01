package portfolio.pucrs.riot.dto;

import portfolio.pucrs.riot.entity.Region;

public record LinkRiotAccountRequest(String gameName, String tagLine, Region region) {
}
