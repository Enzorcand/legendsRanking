package portfolio.pucrs.user.dto;

public record AuthenticatedPlayerResponse(
        Long id,
        String fullName,
        String tagLine,
        String courseName,
        PublicPlayerResponse profile
) {
}
