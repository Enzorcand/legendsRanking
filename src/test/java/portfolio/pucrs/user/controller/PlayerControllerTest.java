package portfolio.pucrs.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import portfolio.pucrs.riot.entity.Region;
import portfolio.pucrs.user.dto.AuthenticatedPlayerResponse;
import portfolio.pucrs.user.dto.PublicPlayerResponse;
import portfolio.pucrs.user.service.PlayerProfileService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerControllerTest {

    private final PlayerProfileService playerProfileService = mock(PlayerProfileService.class);
    private final PlayerController playerController = new PlayerController(playerProfileService);

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
}
