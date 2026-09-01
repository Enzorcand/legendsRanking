package portfolio.pucrs.riot.exception;

public class RiotAccountNotLinkedException extends RuntimeException {

    public RiotAccountNotLinkedException() {
        super("User has no linked Riot account");
    }
}
