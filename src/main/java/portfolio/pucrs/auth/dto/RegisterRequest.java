package portfolio.pucrs.auth.dto;

public record RegisterRequest(String fullName, String email, String password, Long courseId) {
}
