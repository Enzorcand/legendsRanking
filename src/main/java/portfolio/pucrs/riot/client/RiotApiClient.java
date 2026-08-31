package portfolio.pucrs.riot.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import portfolio.pucrs.riot.client.dto.LeagueEntryDto;
import portfolio.pucrs.riot.client.dto.MatchDto;
import portfolio.pucrs.riot.client.dto.RiotAccountDto;
import portfolio.pucrs.riot.exception.RiotAccountNotFoundException;
import portfolio.pucrs.riot.exception.RiotApiException;

import java.net.URI;
import java.util.List;

@Component
public class RiotApiClient {

    private final RestClient restClient;
    private final RiotApiProperties properties;

    public RiotApiClient(RestClient riotRestClient, RiotApiProperties properties) {
        this.restClient = riotRestClient;
        this.properties = properties;
    }

    public RiotAccountDto getAccountByRiotId(String gameName, String tagLine) {
        return fetchAccount(uri -> uri
                .scheme("https")
                .host(properties.continent() + ".api.riotgames.com")
                .path("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
                .build(gameName, tagLine), gameName + "#" + tagLine);
    }

    public RiotAccountDto getAccountByPuuid(String puuid) {
        return fetchAccount(uri -> uri
                .scheme("https")
                .host(properties.continent() + ".api.riotgames.com")
                .path("/riot/account/v1/accounts/by-puuid/{puuid}")
                .build(puuid), puuid);
    }

    private RiotAccountDto fetchAccount(java.util.function.Function<UriBuilder, URI> uriFunction, String identifier) {
        try {
            return restClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .body(RiotAccountDto.class);
        } catch (RiotApiException e) {
            if (e.getStatus() == 404) {
                throw new RiotAccountNotFoundException(identifier);
            }
            throw e;
        }
    }

    public List<LeagueEntryDto> getLeagueEntriesByPuuid(String puuid) {
        LeagueEntryDto[] entries = restClient.get()
                .uri(uri -> uri
                        .scheme("https")
                        .host(properties.platform() + ".api.riotgames.com")
                        .path("/lol/league/v4/entries/by-puuid/{puuid}")
                        .build(puuid))
                .retrieve()
                .body(LeagueEntryDto[].class);
        return entries == null ? List.of() : List.of(entries);
    }

    public List<String> getMatchIdsByPuuid(String puuid, int start, int count, Integer queue, Long startTime) {
        String[] ids = restClient.get()
                .uri(uri -> {
                    uri = uri.scheme("https")
                            .host(properties.continent() + ".api.riotgames.com")
                            .path("/lol/match/v5/matches/by-puuid/{puuid}/ids")
                            .queryParam("start", start)
                            .queryParam("count", count);
                    if (queue != null) {
                        uri = uri.queryParam("queue", queue);
                    }
                    if (startTime != null) {
                        uri = uri.queryParam("startTime", startTime);
                    }
                    return uri.build(puuid);
                })
                .retrieve()
                .body(String[].class);
        return ids == null ? List.of() : List.of(ids);
    }

    public MatchDto getMatchById(String matchId) {
        return restClient.get()
                .uri(uri -> uri
                        .scheme("https")
                        .host(properties.continent() + ".api.riotgames.com")
                        .path("/lol/match/v5/matches/{matchId}")
                        .build(matchId))
                .retrieve()
                .body(MatchDto.class);
    }
}
