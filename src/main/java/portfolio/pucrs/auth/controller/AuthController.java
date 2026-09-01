package portfolio.pucrs.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portfolio.pucrs.auth.dto.AuthResponse;
import portfolio.pucrs.auth.dto.LoginRequest;
import portfolio.pucrs.auth.dto.RegisterRequest;
import portfolio.pucrs.auth.dto.ResendVerificationRequest;
import portfolio.pucrs.auth.dto.VerifyEmailRequest;
import portfolio.pucrs.auth.service.AuthService;
import portfolio.pucrs.auth.service.EmailVerificationService;
import portfolio.pucrs.user.dto.UserSummaryResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserSummaryResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationCode(request.email());
        return ResponseEntity.accepted().build();
    }
}
