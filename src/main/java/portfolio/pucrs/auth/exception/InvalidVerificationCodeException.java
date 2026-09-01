package portfolio.pucrs.auth.exception;

public class InvalidVerificationCodeException extends RuntimeException {

    public InvalidVerificationCodeException() {
        super("Invalid or expired verification code");
    }
}
