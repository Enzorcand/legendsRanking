package portfolio.pucrs.riot.exception;

public class RiotAccountAlreadyLinkedException extends RuntimeException {

    public RiotAccountAlreadyLinkedException() {
        super("User already has a linked Riot account");
    }
}
