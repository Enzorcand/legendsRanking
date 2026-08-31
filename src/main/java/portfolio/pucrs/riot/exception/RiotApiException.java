package portfolio.pucrs.riot.exception;

public class RiotApiException extends RuntimeException {

    private final int status;

    public RiotApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public RiotApiException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
