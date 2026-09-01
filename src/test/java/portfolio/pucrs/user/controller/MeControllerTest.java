package portfolio.pucrs.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import portfolio.pucrs.course.dto.CourseResponse;
import portfolio.pucrs.user.dto.MeResponse;
import portfolio.pucrs.user.dto.UpdateMeRequest;
import portfolio.pucrs.user.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeControllerTest {

    private final UserService userService = mock(UserService.class);
    private final MeController meController = new MeController(userService);
    private final Authentication authentication = mock(Authentication.class);

    private MeResponse sampleResponse() {
        return new MeResponse(1L, "Ana Raposa", "ana@pucrs.br",
                new CourseResponse(1L, "Ciência da Computação"), true, true, null);
    }

    @Test
    void returnsTheProfileOfTheAuthenticatedUser() {
        when(authentication.getPrincipal()).thenReturn(1L);
        when(userService.getMe(1L)).thenReturn(sampleResponse());

        MeResponse response = meController.me(authentication);

        assertEquals(sampleResponse(), response);
    }

    @Test
    void updatesTheProfileOfTheAuthenticatedUser() {
        when(authentication.getPrincipal()).thenReturn(1L);
        UpdateMeRequest request = new UpdateMeRequest("Novo Nome", null);
        when(userService.updateMe(1L, request)).thenReturn(sampleResponse());

        MeResponse response = meController.updateMe(authentication, request);

        assertEquals(sampleResponse(), response);
    }

    @Test
    void togglesRankingParticipationForTheAuthenticatedUser() {
        when(authentication.getPrincipal()).thenReturn(1L);
        MeResponse toggled = new MeResponse(1L, "Ana Raposa", "ana@pucrs.br",
                new CourseResponse(1L, "Ciência da Computação"), true, false, null);
        when(userService.toggleRankingParticipation(1L)).thenReturn(toggled);

        MeResponse response = meController.toggleRankingParticipation(authentication);

        assertEquals(toggled, response);
        verify(userService).toggleRankingParticipation(1L);
    }
}
