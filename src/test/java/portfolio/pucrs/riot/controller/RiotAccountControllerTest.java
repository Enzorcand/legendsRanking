package portfolio.pucrs.riot.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import portfolio.pucrs.riot.dto.LinkRiotAccountRequest;
import portfolio.pucrs.riot.dto.RiotAccountResponse;
import portfolio.pucrs.riot.entity.Region;
import portfolio.pucrs.riot.service.RiotAccountService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiotAccountControllerTest {

    private final RiotAccountService riotAccountService = mock(RiotAccountService.class);
    private final RiotAccountController riotAccountController = new RiotAccountController(riotAccountService);
    private final Authentication authentication = mock(Authentication.class);

    @Test
    void linksTheAccountForTheAuthenticatedUserAndReturns201() {
        when(authentication.getPrincipal()).thenReturn(1L);
        LinkRiotAccountRequest request = new LinkRiotAccountRequest("Raposa", "BR1", Region.BR1);
        RiotAccountResponse expected = new RiotAccountResponse("Raposa", "BR1", Region.BR1);
        when(riotAccountService.linkAccount(1L, request)).thenReturn(expected);

        ResponseEntity<RiotAccountResponse> response = riotAccountController.link(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void unlinksTheAccountForTheAuthenticatedUserAndReturns204() {
        when(authentication.getPrincipal()).thenReturn(1L);

        ResponseEntity<Void> response = riotAccountController.unlink(authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(riotAccountService).unlinkAccount(1L);
    }

    @Test
    void syncsTheAccountForTheAuthenticatedUserAndReturns200() {
        when(authentication.getPrincipal()).thenReturn(1L);
        RiotAccountResponse expected = new RiotAccountResponse("Raposa", "BR1", Region.BR1);
        when(riotAccountService.syncAccount(1L)).thenReturn(expected);

        ResponseEntity<RiotAccountResponse> response = riotAccountController.sync(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }
}
