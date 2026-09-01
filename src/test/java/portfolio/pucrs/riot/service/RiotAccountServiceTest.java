package portfolio.pucrs.riot.service;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import portfolio.pucrs.match.service.MatchSyncService;
import portfolio.pucrs.riot.client.RiotApiClient;
import portfolio.pucrs.riot.client.dto.RiotAccountDto;
import portfolio.pucrs.riot.dto.LinkRiotAccountRequest;
import portfolio.pucrs.riot.dto.RiotAccountResponse;
import portfolio.pucrs.riot.entity.PlayerStats;
import portfolio.pucrs.riot.entity.RankedStats;
import portfolio.pucrs.riot.entity.Region;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.exception.DuplicateRiotAccountException;
import portfolio.pucrs.riot.exception.RiotAccountAlreadyLinkedException;
import portfolio.pucrs.riot.exception.RiotAccountNotLinkedException;
import portfolio.pucrs.riot.exception.SyncCooldownException;
import portfolio.pucrs.riot.repository.PlayerStatsRepository;
import portfolio.pucrs.riot.repository.RankedStatsRepository;
import portfolio.pucrs.riot.repository.RiotAccountRepository;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiotAccountServiceTest {

    private final RiotAccountRepository riotAccountRepository = mock(RiotAccountRepository.class);
    private final RankedStatsRepository rankedStatsRepository = mock(RankedStatsRepository.class);
    private final PlayerStatsRepository playerStatsRepository = mock(PlayerStatsRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RiotApiClient riotApiClient = mock(RiotApiClient.class);
    private final RankedSyncService rankedSyncService = mock(RankedSyncService.class);
    private final MatchSyncService matchSyncService = mock(MatchSyncService.class);
    private final PlayerStatsService playerStatsService = mock(PlayerStatsService.class);
    private final RiotAccountService riotAccountService = new RiotAccountService(
            riotAccountRepository, rankedStatsRepository, playerStatsRepository, userRepository, riotApiClient,
            rankedSyncService, matchSyncService, playerStatsService);

    private static final LinkRiotAccountRequest REQUEST = new LinkRiotAccountRequest("Raposa", "BR1", Region.BR1);

    @Test
    void linksTheRiotAccountWhenNothingConflicts() {
        when(riotAccountRepository.existsByUserId(1L)).thenReturn(false);
        when(riotApiClient.getAccountByRiotId("Raposa", "BR1"))
                .thenReturn(new RiotAccountDto("puuid-1", "Raposa", "BR1"));
        when(riotAccountRepository.existsByPuuid("puuid-1")).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(riotAccountRepository.save(any(RiotAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RiotAccountResponse response = riotAccountService.linkAccount(1L, REQUEST);

        assertEquals(new RiotAccountResponse("Raposa", "BR1", Region.BR1), response);
        verify(rankedSyncService).syncRankedStats(any(RiotAccount.class));
        verify(matchSyncService).syncMatches(any(RiotAccount.class));
        verify(playerStatsService).recalculate(any(RiotAccount.class));
    }

    @Test
    void stillLinksTheAccountWhenTheInitialRankedSyncFails() {
        when(riotAccountRepository.existsByUserId(1L)).thenReturn(false);
        when(riotApiClient.getAccountByRiotId("Raposa", "BR1"))
                .thenReturn(new RiotAccountDto("puuid-1", "Raposa", "BR1"));
        when(riotAccountRepository.existsByPuuid("puuid-1")).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(riotAccountRepository.save(any(RiotAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Riot API unavailable"))
                .when(rankedSyncService).syncRankedStats(any(RiotAccount.class));

        RiotAccountResponse response = riotAccountService.linkAccount(1L, REQUEST);

        assertEquals(new RiotAccountResponse("Raposa", "BR1", Region.BR1), response);
        verify(matchSyncService).syncMatches(any(RiotAccount.class));
    }

    @Test
    void stillLinksTheAccountWhenTheInitialMatchSyncFails() {
        when(riotAccountRepository.existsByUserId(1L)).thenReturn(false);
        when(riotApiClient.getAccountByRiotId("Raposa", "BR1"))
                .thenReturn(new RiotAccountDto("puuid-1", "Raposa", "BR1"));
        when(riotAccountRepository.existsByPuuid("puuid-1")).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(riotAccountRepository.save(any(RiotAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Riot API unavailable"))
                .when(matchSyncService).syncMatches(any(RiotAccount.class));

        RiotAccountResponse response = riotAccountService.linkAccount(1L, REQUEST);

        assertEquals(new RiotAccountResponse("Raposa", "BR1", Region.BR1), response);
        verify(playerStatsService).recalculate(any(RiotAccount.class));
    }

    @Test
    void stillLinksTheAccountWhenTheInitialPlayerStatsRecalculationFails() {
        when(riotAccountRepository.existsByUserId(1L)).thenReturn(false);
        when(riotApiClient.getAccountByRiotId("Raposa", "BR1"))
                .thenReturn(new RiotAccountDto("puuid-1", "Raposa", "BR1"));
        when(riotAccountRepository.existsByPuuid("puuid-1")).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(riotAccountRepository.save(any(RiotAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Unexpected failure"))
                .when(playerStatsService).recalculate(any(RiotAccount.class));

        RiotAccountResponse response = riotAccountService.linkAccount(1L, REQUEST);

        assertEquals(new RiotAccountResponse("Raposa", "BR1", Region.BR1), response);
    }

    @Test
    void rejectsLinkingWhenTheUserAlreadyHasARiotAccount() {
        when(riotAccountRepository.existsByUserId(1L)).thenReturn(true);

        assertThrows(RiotAccountAlreadyLinkedException.class, () -> riotAccountService.linkAccount(1L, REQUEST));
        verify(riotApiClient, never()).getAccountByRiotId(any(), any());
    }

    @Test
    void rejectsLinkingWhenThePuuidIsAlreadyUsedByAnotherUser() {
        when(riotAccountRepository.existsByUserId(1L)).thenReturn(false);
        when(riotApiClient.getAccountByRiotId("Raposa", "BR1"))
                .thenReturn(new RiotAccountDto("puuid-1", "Raposa", "BR1"));
        when(riotAccountRepository.existsByPuuid("puuid-1")).thenReturn(true);

        assertThrows(DuplicateRiotAccountException.class, () -> riotAccountService.linkAccount(1L, REQUEST));
        verify(riotAccountRepository, never()).save(any());
    }

    @Test
    void translatesAConcurrentDuplicateSaveIntoADomainException() {
        when(riotAccountRepository.existsByUserId(1L)).thenReturn(false);
        when(riotApiClient.getAccountByRiotId("Raposa", "BR1"))
                .thenReturn(new RiotAccountDto("puuid-1", "Raposa", "BR1"));
        when(riotAccountRepository.existsByPuuid("puuid-1")).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(riotAccountRepository.save(any(RiotAccount.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThrows(DuplicateRiotAccountException.class, () -> riotAccountService.linkAccount(1L, REQUEST));
    }

    @Test
    void unlinksAnExistingRiotAccount() {
        RiotAccount riotAccount = new RiotAccount();
        when(riotAccountRepository.findByUserId(1L)).thenReturn(Optional.of(riotAccount));

        riotAccountService.unlinkAccount(1L);

        verify(riotAccountRepository).delete(riotAccount);
        verify(rankedStatsRepository, never()).delete(any());
        verify(playerStatsRepository, never()).delete(any());
    }

    @Test
    void alsoDeletesTheRankedStatsWhenUnlinkingAnAccountThatHasOne() {
        RiotAccount riotAccount = new RiotAccount();
        RankedStats rankedStats = new RankedStats();
        riotAccount.setRankedStats(rankedStats);
        when(riotAccountRepository.findByUserId(1L)).thenReturn(Optional.of(riotAccount));

        riotAccountService.unlinkAccount(1L);

        verify(riotAccountRepository).delete(riotAccount);
        verify(rankedStatsRepository).delete(rankedStats);
    }

    @Test
    void alsoDeletesTheDetachedPlayerStatsWhenUnlinkingAnAccountThatHasOne() {
        RiotAccount riotAccount = new RiotAccount();
        PlayerStats playerStats = new PlayerStats();
        riotAccount.setPlayerStats(playerStats);
        when(riotAccountRepository.findByUserId(1L)).thenReturn(Optional.of(riotAccount));

        riotAccountService.unlinkAccount(1L);

        verify(playerStatsRepository).delete(playerStats);
        assertEquals(null, riotAccount.getPlayerStats());
        verify(riotAccountRepository).delete(riotAccount);
    }

    @Test
    void rejectsUnlinkingWhenTheUserHasNoRiotAccount() {
        when(riotAccountRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(RiotAccountNotLinkedException.class, () -> riotAccountService.unlinkAccount(1L));
    }

    @Test
    void syncsAnAccountThatWasNeverSyncedBefore() {
        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setGameName("Raposa");
        riotAccount.setTagLine("BR1");
        riotAccount.setRegion(Region.BR1);
        when(riotAccountRepository.findByUserId(1L)).thenReturn(Optional.of(riotAccount));

        RiotAccountResponse response = riotAccountService.syncAccount(1L);

        assertEquals(new RiotAccountResponse("Raposa", "BR1", Region.BR1), response);
        verify(rankedSyncService).syncRankedStats(riotAccount);
        verify(matchSyncService).syncMatches(riotAccount);
        verify(playerStatsService).recalculate(riotAccount);
        assertNotNull(riotAccount.getLastSyncedAt());
    }

    @Test
    void rejectsSyncingWhenTheCooldownHasNotElapsed() {
        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setLastSyncedAt(LocalDateTime.now().minusSeconds(30));
        when(riotAccountRepository.findByUserId(1L)).thenReturn(Optional.of(riotAccount));

        assertThrows(SyncCooldownException.class, () -> riotAccountService.syncAccount(1L));
        verify(rankedSyncService, never()).syncRankedStats(any());
        verify(matchSyncService, never()).syncMatches(any());
        verify(playerStatsService, never()).recalculate(any());
    }

    @Test
    void allowsSyncingWhenTheCooldownHasElapsed() {
        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setLastSyncedAt(LocalDateTime.now().minusMinutes(3));
        when(riotAccountRepository.findByUserId(1L)).thenReturn(Optional.of(riotAccount));

        riotAccountService.syncAccount(1L);

        verify(rankedSyncService).syncRankedStats(riotAccount);
    }

    @Test
    void rejectsSyncingWhenTheUserHasNoRiotAccount() {
        when(riotAccountRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(RiotAccountNotLinkedException.class, () -> riotAccountService.syncAccount(1L));
    }
}
