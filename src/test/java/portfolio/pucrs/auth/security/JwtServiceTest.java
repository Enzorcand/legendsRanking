package portfolio.pucrs.auth.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-only-secret-key-with-at-least-32-bytes", 60);

    @Test
    void generatesATokenThatResolvesBackToTheSameUserId() {
        String token = jwtService.generateToken(42L, "aluno@pucrs.br");

        Optional<Long> userId = jwtService.parseUserId(token);

        assertEquals(Optional.of(42L), userId);
    }

    @Test
    void rejectsATamperedToken() {
        String token = jwtService.generateToken(1L, "aluno@pucrs.br");

        Optional<Long> userId = jwtService.parseUserId(token + "tampered");

        assertTrue(userId.isEmpty());
    }

    @Test
    void rejectsAGarbageToken() {
        Optional<Long> userId = jwtService.parseUserId("not-a-jwt");

        assertTrue(userId.isEmpty());
    }
}
