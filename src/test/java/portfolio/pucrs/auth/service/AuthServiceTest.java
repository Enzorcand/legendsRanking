package portfolio.pucrs.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService("test-only-secret-key-with-at-least-32-bytes", 60);
    private final EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    private final AuthService authService =
            new AuthService(userRepository, courseRepository, passwordEncoder, jwtService, emailVerificationService);

    private Course activeCourse() {
        Course course = new Course();
        course.setId(1L);
        course.setName("Ciência da Computação");
        course.setActive(true);
        return course;
    }

    @Test
    void registersAUserWithAHashedPasswordAndNormalizedEmail() {
        when(userRepository.existsByEmailIgnoreCase("aluno@edu.pucrs.br")).thenReturn(false);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(activeCourse()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        UserSummaryResponse response = authService.register(
                new RegisterRequest("Ana Raposa", " ALUNO@EDU.PUCRS.BR ", "senhaSegura1", 1L));

        assertEquals(new UserSummaryResponse(10L, "Ana Raposa"), response);
        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).sendVerificationCode(any(User.class));
    }

    @Test
    void rejectsRegistrationWithNonInstitutionalEmail() {
        assertThrows(InvalidRegistrationException.class, () -> authService.register(
                new RegisterRequest("Ana Raposa", "ana@gmail.com", "senhaSegura1", 1L)));
    }

    @Test
    void rejectsRegistrationWithShortPassword() {
        assertThrows(InvalidRegistrationException.class, () -> authService.register(
                new RegisterRequest("Ana Raposa", "aluno@edu.pucrs.br", "123", 1L)));
    }

    @Test
    void rejectsRegistrationWithDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("aluno@edu.pucrs.br")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> authService.register(
                new RegisterRequest("Ana Raposa", "aluno@edu.pucrs.br", "senhaSegura1", 1L)));
    }

    @Test
    void rejectsRegistrationWithUnknownOrInactiveCourse() {
        when(userRepository.existsByEmailIgnoreCase("aluno@edu.pucrs.br")).thenReturn(false);
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(InvalidCourseException.class, () -> authService.register(
                new RegisterRequest("Ana Raposa", "aluno@edu.pucrs.br", "senhaSegura1", 99L)));
    }

    @Test
    void logsInWithCorrectCredentialsAndReturnsAToken() {
        User user = new User();
        user.setId(5L);
        user.setEmail("aluno@edu.pucrs.br");
        user.setPasswordHash(passwordEncoder.encode("senhaSegura1"));
        user.setActive(true);
        when(userRepository.findByEmailIgnoreCase("aluno@edu.pucrs.br")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("aluno@edu.pucrs.br", "senhaSegura1"));

        assertEquals(Optional.of(5L), jwtService.parseUserId(response.token()));
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        User user = new User();
        user.setId(5L);
        user.setEmail("aluno@edu.pucrs.br");
        user.setPasswordHash(passwordEncoder.encode("senhaSegura1"));
        user.setActive(true);
        when(userRepository.findByEmailIgnoreCase("aluno@edu.pucrs.br")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(
                new LoginRequest("aluno@edu.pucrs.br", "wrong-password")));
    }

    @Test
    void rejectsLoginForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("ghost@edu.pucrs.br")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(
                new LoginRequest("ghost@edu.pucrs.br", "senhaSegura1")));
    }

    @Test
    void rejectsLoginForInactiveUser() {
        User user = new User();
        user.setId(5L);
        user.setEmail("aluno@edu.pucrs.br");
        user.setPasswordHash(passwordEncoder.encode("senhaSegura1"));
        user.setActive(false);
        when(userRepository.findByEmailIgnoreCase("aluno@edu.pucrs.br")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(
                new LoginRequest("aluno@edu.pucrs.br", "senhaSegura1")));
    }
}
