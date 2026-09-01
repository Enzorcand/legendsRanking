package portfolio.pucrs.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import portfolio.pucrs.auth.exception.EmailAlreadyRegisteredException;
import portfolio.pucrs.auth.exception.InvalidCourseException;
import portfolio.pucrs.auth.exception.InvalidCredentialsException;
import portfolio.pucrs.auth.exception.InvalidRegistrationException;
import portfolio.pucrs.auth.exception.InvalidVerificationCodeException;
import portfolio.pucrs.riot.exception.DuplicateRiotAccountException;
import portfolio.pucrs.riot.exception.RiotAccountAlreadyLinkedException;
import portfolio.pucrs.riot.exception.RiotAccountNotLinkedException;
import portfolio.pucrs.riot.exception.RiotApiException;
import portfolio.pucrs.user.exception.PlayerNotFoundException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException e) {
        return errorResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({InvalidCourseException.class, InvalidRegistrationException.class, InvalidVerificationCodeException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException e) {
        return errorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler({RiotAccountAlreadyLinkedException.class, DuplicateRiotAccountException.class})
    public ResponseEntity<Map<String, Object>> handleRiotAccountConflict(RuntimeException e) {
        return errorResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({RiotAccountNotLinkedException.class, PlayerNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException e) {
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RiotApiException.class)
    public ResponseEntity<Map<String, Object>> handleRiotApiException(RiotApiException e) {
        return errorResponse(HttpStatus.valueOf(e.getStatus()), e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "message", message));
    }
}
