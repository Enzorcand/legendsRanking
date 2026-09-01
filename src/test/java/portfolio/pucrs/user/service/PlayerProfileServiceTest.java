package portfolio.pucrs.user.service;

import org.junit.jupiter.api.Test;
import portfolio.pucrs.course.entity.Course;
import portfolio.pucrs.ranking.service.RankingService;
import portfolio.pucrs.riot.entity.PlayerStats;
import portfolio.pucrs.riot.entity.RankedStats;
import portfolio.pucrs.riot.entity.Region;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.entity.Tier;
import portfolio.pucrs.user.dto.AuthenticatedPlayerResponse;
import portfolio.pucrs.user.dto.PublicPlayerResponse;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.exception.PlayerNotFoundException;
import portfolio.pucrs.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RankingService rankingService = mock(RankingService.class);
    private final PlayerProfileService playerProfileService =
            new PlayerProfileService(userRepository, rankingService);

    private Course course(String name) {
        Course course = new Course();
        course.setId(1L);
        course.setName(name);
        course.setActive(true);
        return course;
    }

    private RiotAccount riotAccount(Long id, String gameName, String tagLine) {
        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setId(id);
        riotAccount.setGameName(gameName);
        riotAccount.setTagLine(tagLine);
        riotAccount.setRegion(Region.BR1);
        return riotAccount;
    }

    private User activeUser(Long id, RiotAccount riotAccount) {
        User user = new User();
        user.setId(id);
        user.setFullName("Ana Raposa");
        user.setActive(true);
        user.setCourse(course("Ciência da Computação"));
        user.setRiotAccount(riotAccount);
        return user;
    }

    @Test
    void publicProfileHidesFullNameCourseAndTagLine() {
        RiotAccount riotAccount = riotAccount(5L, "Raposa", "BR1");
        User user = activeUser(1L, riotAccount);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PublicPlayerResponse response = playerProfileService.getPublicProfile(1L);

        assertEquals("Raposa", response.nickname());
        assertFalse(response.ranked());
    }

    @Test
    void authenticatedProfileIncludesFullNameTagLineAndCourse() {
        RiotAccount riotAccount = riotAccount(5L, "Raposa", "BR1");
        User user = activeUser(1L, riotAccount);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuthenticatedPlayerResponse response = playerProfileService.getAuthenticatedProfile(1L);

        assertEquals(1L, response.id());
        assertEquals("Ana Raposa", response.fullName());
        assertEquals("BR1", response.tagLine());
        assertEquals("Ciência da Computação", response.courseName());
        assertEquals("Raposa", response.profile().nickname());
    }

    @Test
    void appearsAsUnrankedWithoutRankedStats() {
        RiotAccount riotAccount = riotAccount(5L, "Raposa", "BR1");
        User user = activeUser(1L, riotAccount);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PublicPlayerResponse response = playerProfileService.getPublicProfile(1L);

        assertFalse(response.ranked());
        assertNull(response.tier());
        assertNull(response.position());
    }

    @Test
    void reportsThePositionWhenTheAccountIsRanked() {
        RiotAccount riotAccount = riotAccount(5L, "Raposa", "BR1");
        RankedStats rankedStats = new RankedStats();
        rankedStats.setTier(Tier.GOLD);
        rankedStats.setDivision("II");
        rankedStats.setLeaguePoints(40);
        rankedStats.setWins(10);
        rankedStats.setLosses(5);
        riotAccount.setRankedStats(rankedStats);
        User user = activeUser(1L, riotAccount);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rankingService.findPosition(5L)).thenReturn(Optional.of(7));

        PublicPlayerResponse response = playerProfileService.getPublicProfile(1L);

        assertEquals(true, response.ranked());
        assertEquals(Tier.GOLD, response.tier());
        assertEquals(7, response.position());
        assertEquals(10.0 / 15.0, response.winRate());
    }

    @Test
    void includesLaneAndChampionFromPlayerStats() {
        RiotAccount riotAccount = riotAccount(5L, "Raposa", "BR1");
        PlayerStats playerStats = new PlayerStats();
        playerStats.setPrimaryRole("MIDDLE");
        playerStats.setMostPlayedChampionId(103);
        riotAccount.setPlayerStats(playerStats);
        User user = activeUser(1L, riotAccount);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PublicPlayerResponse response = playerProfileService.getPublicProfile(1L);

        assertEquals("MIDDLE", response.lane());
        assertEquals(103, response.mostPlayedChampionId());
    }

    @Test
    void rejectsAnUnknownId() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class, () -> playerProfileService.getPublicProfile(404L));
    }

    @Test
    void rejectsAnInactiveUser() {
        User user = activeUser(1L, riotAccount(5L, "Raposa", "BR1"));
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(PlayerNotFoundException.class, () -> playerProfileService.getPublicProfile(1L));
    }

    @Test
    void rejectsAUserWithoutALinkedRiotAccount() {
        User user = activeUser(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(PlayerNotFoundException.class, () -> playerProfileService.getPublicProfile(1L));
    }
}
