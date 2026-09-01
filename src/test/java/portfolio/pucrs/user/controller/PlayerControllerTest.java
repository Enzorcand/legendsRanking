package portfolio.pucrs.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import portfolio.pucrs.match.dto.MatchHistoryEntryResponse;
import portfolio.pucrs.match.service.MatchHistoryService;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.user.dto.AuthenticatedPlayerResponse;
import portfolio.pucrs.user.dto.PublicPlayerResponse;
import portfolio.pucrs.user.service.PlayerProfileService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerControllerTest {

    private final PlayerProfileService playerProfileService = mock(PlayerProfileService.class);
    private final MatchHistoryService matchHistoryService = mock(MatchHistoryService.class);
    private final PlayerController playerController =
            new PlayerController(playerProfileService, matchHistoryService);

    private PublicPlayerResponse publicResponse() {
        return new PublicPlayerResponse("Raposa", false, null, null, 0, 0, 0, 0.0, null, null, null);
    }

    @Test
    void returnsThePublicProfileForAnAnonymousCaller() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        when(playerProfileService.getPublicProfile(1L)).thenReturn(publicResponse());

        Object response = playerController.getPlayer(1L, anonymous);

        assertEquals(publicResponse(), response);
        verify(playerProfileService, never()).getAuthenticatedProfile(1L);
    }

    @Test
    void returnsTheAuthenticatedProfileForALoggedInCaller() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(99L);
        AuthenticatedPlayerResponse expected = new AuthenticatedPlayerResponse(
                1L, "Ana Raposa", "BR1", "Ciência da Computação", publicResponse());
        when(playerProfileService.getAuthenticatedProfile(1L)).thenReturn(expected);

        Object response = playerController.getPlayer(1L, authentication);

        assertEquals(expected, response);
        verify(playerProfileService, never()).getPublicProfile(1L);
    }

    @Test
    void returnsTheMatchHistoryForThePlayersLinkedRiotAccount() {
        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setId(5L);
        when(playerProfileService.getViewableRiotAccount(1L)).thenReturn(riotAccount);
        MatchHistoryEntryResponse entry = new MatchHistoryEntryResponse(
                "BR1_1", Instant.now(), 1800, true, 103, "MIDDLE", 8, 2, 10, 180);
        Page<MatchHistoryEntryResponse> expected = new PageImpl<>(List.of(entry));
        when(matchHistoryService.getHistory(eq(5L), eq(PageRequest.of(0, 20)))).thenReturn(expected);

        Page<MatchHistoryEntryResponse> response = playerController.getMatchHistory(1L, 0, 20);

        assertEquals(expected, response);
    }
}
