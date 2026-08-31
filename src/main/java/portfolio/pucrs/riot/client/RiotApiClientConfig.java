package portfolio.pucrs.riot.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import portfolio.pucrs.riot.exception.RiotApiException;

@Configuration
@EnableConfigurationProperties(RiotApiProperties.class)
public class RiotApiClientConfig {

    @Bean
    RestClient riotRestClient(RiotApiProperties properties) {
        return RestClient.builder()
                .defaultHeader("X-Riot-Token", properties.apiKey())
                .defaultStatusHandler(status -> status.isError(), (request, response) -> {
                    throw new RiotApiException(response.getStatusCode().value(),
                            "Riot API request failed: " + request.getMethod() + " " + request.getURI().getPath());
                })
                .build();
    }
}
