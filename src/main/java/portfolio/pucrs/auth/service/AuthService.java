package portfolio.pucrs.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.auth.dto.AuthResponse;
import portfolio.pucrs.auth.dto.LoginRequest;
import portfolio.pucrs.auth.dto.RegisterRequest;
import portfolio.pucrs.auth.exception.EmailAlreadyRegisteredException;
import portfolio.pucrs.auth.exception.InvalidCourseException;
import portfolio.pucrs.auth.exception.InvalidCredentialsException;
import portfolio.pucrs.auth.exception.InvalidRegistrationException;
import portfolio.pucrs.auth.security.JwtService;
import portfolio.pucrs.course.entity.Course;
import portfolio.pucrs.course.repository.CourseRepository;
import portfolio.pucrs.user.dto.UserSummaryResponse;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.repository.UserRepository;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional
public class AuthService {

    private static final Pattern INSTITUTIONAL_EMAIL = Pattern.compile("^[^@\\s]+@pucrs\\.br$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    public UserSummaryResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validateFullName(request.fullName());
        validateInstitutionalEmail(email);
        validatePassword(request.password());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        Course course = courseRepository.findById(request.courseId())
                .filter(Course::isActive)
                .orElseThrow(() -> new InvalidCourseException(request.courseId()));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCourse(course);

        User saved = userRepository.save(user);
        emailVerificationService.sendVerificationCode(saved);
        return new UserSummaryResponse(saved.getId(), saved.getFullName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, "Bearer", jwtService.expirationSeconds());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidRegistrationException("Email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidRegistrationException("Full name must not be blank");
        }
    }

    private void validateInstitutionalEmail(String email) {
        if (!INSTITUTIONAL_EMAIL.matcher(email).matches()) {
            throw new InvalidRegistrationException("Email must be a valid @pucrs.br address");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidRegistrationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
    }
}
