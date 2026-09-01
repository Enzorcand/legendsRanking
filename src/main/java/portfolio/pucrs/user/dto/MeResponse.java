package portfolio.pucrs.user.dto;

import portfolio.pucrs.course.dto.CourseResponse;
import portfolio.pucrs.riot.dto.RiotAccountResponse;

public record MeResponse(
        Long id,
        String fullName,
        String email,
        CourseResponse course,
        boolean emailVerified,
        boolean rankingEligible,
        RiotAccountResponse riotAccount
) {
}
