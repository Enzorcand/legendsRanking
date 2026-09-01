package portfolio.pucrs.riot.exception;

public class DuplicateRiotAccountException extends RuntimeException {

    public DuplicateRiotAccountException() {
        super("This Riot account is already linked to another user");
    }
}
