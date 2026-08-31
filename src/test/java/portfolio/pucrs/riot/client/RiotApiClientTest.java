package portfolio.pucrs.riot.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;
import portfolio.pucrs.riot.client.dto.LeagueEntryDto;
import portfolio.pucrs.riot.client.dto.MatchDto;
import portfolio.pucrs.riot.client.dto.RiotAccountDto;
import portfolio.pucrs.riot.exception.RiotAccountNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiotApiClientTest {

    private final RiotApiProperties properties = new RiotApiProperties("test-key", "br1", "americas");
    private MockRestServiceServer server;
    private RiotApiClient riotApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder
                .defaultHeader("X-Riot-Token", properties.apiKey())
                .defaultStatusHandler(status -> status.isError(), (request, response) -> {
                    throw new portfolio.pucrs.riot.exception.RiotApiException(response.getStatusCode().value(),
                            "Riot API request failed: " + request.getMethod() + " " + request.getURI().getPath());
                })
                .build();
        riotApiClient = new RiotApiClient(restClient, properties);
    }

    @Test
    void returnsTheAccountForAValidRiotId() {
        server.expect(MockRestRequestMatchers.requestTo(
                        "https://americas.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Raposa/BR1"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"puuid\":\"abc-123\",\"gameName\":\"Raposa\",\"tagLine\":\"BR1\"}",
                        MediaType.APPLICATION_JSON));

        RiotAccountDto account = riotApiClient.getAccountByRiotId("Raposa", "BR1");

        assertEquals(new RiotAccountDto("abc-123", "Raposa", "BR1"), account);
    }

    @Test
    void throwsRiotAccountNotFoundWhenTheRiotIdDoesNotExist() {
        server.expect(MockRestRequestMatchers.requestTo(
                        "https://americas.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Unknown/BR1"))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(RiotAccountNotFoundException.class,
                () -> riotApiClient.getAccountByRiotId("Unknown", "BR1"));
    }

    @Test
    void returnsAnEmptyListWhenThePlayerIsUnranked() {
        server.expect(MockRestRequestMatchers.requestTo(
                        "https://br1.api.riotgames.com/lol/league/v4/entries/by-puuid/abc-123"))
                .andRespond(MockRestResponseCreators.withSuccess("[]", MediaType.APPLICATION_JSON));

        List<LeagueEntryDto> entries = riotApiClient.getLeagueEntriesByPuuid("abc-123");

        assertTrue(entries.isEmpty());
    }

    @Test
    void sendsQueueAndStartTimeAsQueryParamsWhenInformed() {
        server.expect(MockRestRequestMatchers.requestTo(
                        "https://americas.api.riotgames.com/lol/match/v5/matches/by-puuid/abc-123/ids?start=0&count=20&queue=420&startTime=1700000000"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "[\"BR1_111\",\"BR1_222\"]", MediaType.APPLICATION_JSON));

        List<String> matchIds = riotApiClient.getMatchIdsByPuuid("abc-123", 0, 20, 420, 1700000000L);

        assertEquals(List.of("BR1_111", "BR1_222"), matchIds);
    }

    @Test
    void returnsMatchDetailsNeededForStatistics() {
        server.expect(MockRestRequestMatchers.requestTo(
                        "https://americas.api.riotgames.com/lol/match/v5/matches/BR1_111"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {
                          "metadata": {"matchId": "BR1_111"},
                          "info": {
                            "gameStartTimestamp": 1700000000000,
                            "gameDuration": 1800,
                            "queueId": 420,
                            "participants": [
                              {
                                "puuid": "abc-123",
                                "championId": 103,
                                "teamPosition": "MIDDLE",
                                "win": true,
                                "kills": 8,
                                "deaths": 2,
                                "assists": 10,
                                "totalMinionsKilled": 180,
                                "neutralMinionsKilled": 5
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        MatchDto match = riotApiClient.getMatchById("BR1_111");

        assertEquals("BR1_111", match.metadata().matchId());
        assertEquals(420, match.info().queueId());
        assertEquals(1, match.info().participants().size());
        assertEquals("abc-123", match.info().participants().get(0).puuid());
        assertTrue(match.info().participants().get(0).win());
    }
}
