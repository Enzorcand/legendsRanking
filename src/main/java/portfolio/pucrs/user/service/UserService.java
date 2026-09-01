package portfolio.pucrs.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.auth.exception.InvalidCourseException;
import portfolio.pucrs.auth.exception.InvalidRegistrationException;
import portfolio.pucrs.course.dto.CourseResponse;
import portfolio.pucrs.course.entity.Course;
import portfolio.pucrs.course.repository.CourseRepository;
import portfolio.pucrs.riot.dto.RiotAccountResponse;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.user.dto.MeResponse;
import portfolio.pucrs.user.dto.UpdateMeRequest;
import portfolio.pucrs.user.dto.UserSummaryResponse;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.repository.UserRepository;

import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public UserService(UserRepository userRepository, CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    public UserSummaryResponse getUserSummaryByEmail(String email) {
        var user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return new UserSummaryResponse(user.getId(), user.getFullName());
    }

    public MeResponse getMe(Long userId) {
        return toMeResponse(getUserOrThrow(userId));
    }

    @Transactional
    public MeResponse updateMe(Long userId, UpdateMeRequest request) {
        boolean hasFullName = request.fullName() != null && !request.fullName().isBlank();
        boolean hasCourseId = request.courseId() != null;
        if (!hasFullName && !hasCourseId) {
            throw new InvalidRegistrationException("At least one field must be provided");
        }

        User user = getUserOrThrow(userId);

        if (hasFullName) {
            user.setFullName(request.fullName().trim());
        }
        if (hasCourseId) {
            Course course = courseRepository.findById(request.courseId())
                    .filter(Course::isActive)
                    .orElseThrow(() -> new InvalidCourseException(request.courseId()));
            user.setCourse(course);
        }

        return toMeResponse(userRepository.save(user));
    }

    @Transactional
    public MeResponse toggleRankingParticipation(Long userId) {
        User user = getUserOrThrow(userId);
        user.setRankingEligible(!user.isRankingEligible());
        return toMeResponse(userRepository.save(user));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private MeResponse toMeResponse(User user) {
        CourseResponse course = new CourseResponse(user.getCourse().getId(), user.getCourse().getName());
        RiotAccountResponse riotAccount = toRiotAccountResponse(user.getRiotAccount());
        return new MeResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                course,
                user.isEmailVerified(),
                user.isRankingEligible(),
                riotAccount);
    }

    private RiotAccountResponse toRiotAccountResponse(RiotAccount riotAccount) {
        if (riotAccount == null) {
            return null;
        }
        return new RiotAccountResponse(riotAccount.getGameName(), riotAccount.getTagLine(), riotAccount.getRegion());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
