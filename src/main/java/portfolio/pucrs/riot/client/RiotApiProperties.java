package portfolio.pucrs.riot.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "riot")
public record RiotApiProperties(String apiKey, String platform, String continent) {
}
