package portfolio.pucrs.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.auth.entity.EmailVerification;
import portfolio.pucrs.auth.exception.InvalidRegistrationException;
import portfolio.pucrs.auth.exception.InvalidVerificationCodeException;
import portfolio.pucrs.auth.mail.MailSender;
import portfolio.pucrs.auth.repository.EmailVerificationRepository;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
public class EmailVerificationService {

    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(15);

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder codeEncoder;
    private final MailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationRepository emailVerificationRepository,
            UserRepository userRepository,
            PasswordEncoder codeEncoder,
            MailSender mailSender) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.userRepository = userRepository;
        this.codeEncoder = codeEncoder;
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(User user) {
        invalidatePendingCodes(user.getId());

        String code = generateCode();
        EmailVerification verification = new EmailVerification();
        verification.setUser(user);
        verification.setCodeHash(codeEncoder.encode(code));
        verification.setExpiresAt(LocalDateTime.now().plus(CODE_EXPIRATION));
        emailVerificationRepository.save(verification);

        mailSender.sendVerificationCode(user.getEmail(), code);
    }

    public void resendVerificationCode(String email) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerificationCode);
    }

    public void verifyCode(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(InvalidVerificationCodeException::new);

        EmailVerification verification = emailVerificationRepository
                .findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(InvalidVerificationCodeException::new);

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())
                || code == null
                || !codeEncoder.matches(code, verification.getCodeHash())) {
            throw new InvalidVerificationCodeException();
        }

        verification.setConsumedAt(LocalDateTime.now());
        emailVerificationRepository.save(verification);

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    private void invalidatePendingCodes(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.findAllByUserIdAndConsumedAtIsNull(userId)
                .forEach(pending -> pending.setConsumedAt(now));
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidRegistrationException("Email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
