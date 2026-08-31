package portfolio.pucrs.auth.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds) {
}
