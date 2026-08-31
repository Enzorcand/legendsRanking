package portfolio.pucrs.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import portfolio.pucrs.auth.dto.AuthResponse;
import portfolio.pucrs.auth.dto.LoginRequest;
import portfolio.pucrs.auth.dto.RegisterRequest;
import portfolio.pucrs.auth.service.AuthService;
import portfolio.pucrs.user.dto.UserSummaryResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController authController = new AuthController(authService);

    @Test
    void registersAUserAndReturns201() {
        RegisterRequest request = new RegisterRequest("Ana Raposa", "aluno@pucrs.br", "senhaSegura1", 1L);
        UserSummaryResponse expected = new UserSummaryResponse(10L, "Ana Raposa");
        when(authService.register(request)).thenReturn(expected);

        ResponseEntity<UserSummaryResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void logsInAndReturnsAToken() {
        LoginRequest request = new LoginRequest("aluno@pucrs.br", "senhaSegura1");
        AuthResponse expected = new AuthResponse("jwt-token", "Bearer", 3600);
        when(authService.login(request)).thenReturn(expected);

        AuthResponse response = authController.login(request);

        assertEquals(expected, response);
    }
}
