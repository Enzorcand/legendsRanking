package portfolio.pucrs.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import portfolio.pucrs.auth.entity.EmailVerification;
import portfolio.pucrs.auth.exception.InvalidVerificationCodeException;
import portfolio.pucrs.auth.mail.MailSender;
import portfolio.pucrs.auth.repository.EmailVerificationRepository;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    private final EmailVerificationRepository emailVerificationRepository = mock(EmailVerificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder codeEncoder = new BCryptPasswordEncoder();
    private final MailSender mailSender = mock(MailSender.class);
    private final EmailVerificationService emailVerificationService =
            new EmailVerificationService(emailVerificationRepository, userRepository, codeEncoder, mailSender);

    private User verifiedFalseUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("aluno@pucrs.br");
        user.setEmailVerified(false);
        return user;
    }

    @Test
    void sendsAVerificationCodeAndPersistsItHashed() {
        User user = verifiedFalseUser();
        when(emailVerificationRepository.findAllByUserIdAndConsumedAtIsNull(1L)).thenReturn(List.of());

        emailVerificationService.sendVerificationCode(user);

        verify(emailVerificationRepository).save(any(EmailVerification.class));
        verify(mailSender).sendVerificationCode(eq("aluno@pucrs.br"), anyString());
    }

    @Test
    void invalidatesPendingCodesBeforeSendingANewOne() {
        User user = verifiedFalseUser();
        EmailVerification pending = new EmailVerification();
        when(emailVerificationRepository.findAllByUserIdAndConsumedAtIsNull(1L)).thenReturn(List.of(pending));

        emailVerificationService.sendVerificationCode(user);

        assertNotNull(pending.getConsumedAt());
    }

    @Test
    void verifiesAValidCodeAndMarksTheUserAsVerified() {
        User user = verifiedFalseUser();
        String code = "123456";
        EmailVerification verification = new EmailVerification();
        verification.setUser(user);
        verification.setCodeHash(codeEncoder.encode(code));
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmailIgnoreCase("aluno@pucrs.br")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(verification));

        emailVerificationService.verifyCode("aluno@pucrs.br", code);

        assertTrue(user.isEmailVerified());
        assertNotNull(verification.getConsumedAt());
        verify(userRepository).save(user);
    }

    @Test
    void rejectsAWrongCode() {
        User user = verifiedFalseUser();
        EmailVerification verification = new EmailVerification();
        verification.setUser(user);
        verification.setCodeHash(codeEncoder.encode("123456"));
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmailIgnoreCase("aluno@pucrs.br")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(verification));

        assertThrows(InvalidVerificationCodeException.class,
                () -> emailVerificationService.verifyCode("aluno@pucrs.br", "000000"));
        assertFalse(user.isEmailVerified());
    }

    @Test
    void rejectsAnExpiredCode() {
        User user = verifiedFalseUser();
        EmailVerification verification = new EmailVerification();
        verification.setUser(user);
        verification.setCodeHash(codeEncoder.encode("123456"));
        verification.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmailIgnoreCase("aluno@pucrs.br")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(verification));

        assertThrows(InvalidVerificationCodeException.class,
                () -> emailVerificationService.verifyCode("aluno@pucrs.br", "123456"));
    }

    @Test
    void rejectsWhenThereIsNoPendingCodeForTheUser() {
        User user = verifiedFalseUser();
        when(userRepository.findByEmailIgnoreCase("aluno@pucrs.br")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());

        assertThrows(InvalidVerificationCodeException.class,
                () -> emailVerificationService.verifyCode("aluno@pucrs.br", "123456"));
    }

    @Test
    void rejectsWhenTheUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("ghost@pucrs.br")).thenReturn(Optional.empty());

        assertThrows(InvalidVerificationCodeException.class,
                () -> emailVerificationService.verifyCode("ghost@pucrs.br", "123456"));
    }

    @Test
    void doesNotSendANewCodeWhenTheUserIsAlreadyVerified() {
        User user = verifiedFalseUser();
        user.setEmailVerified(true);
        when(userRepository.findByEmailIgnoreCase("aluno@pucrs.br")).thenReturn(Optional.of(user));

        emailVerificationService.resendVerificationCode("aluno@pucrs.br");

        verify(mailSender, never()).sendVerificationCode(anyString(), anyString());
        verify(emailVerificationRepository, never()).save(any(EmailVerification.class));
    }

    @Test
    void resendsANewCodeWhenTheUserIsNotYetVerified() {
        User user = verifiedFalseUser();
        when(userRepository.findByEmailIgnoreCase("aluno@pucrs.br")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findAllByUserIdAndConsumedAtIsNull(1L)).thenReturn(List.of());

        emailVerificationService.resendVerificationCode("aluno@pucrs.br");

        verify(mailSender).sendVerificationCode(eq("aluno@pucrs.br"), anyString());
    }
}
