package portfolio.pucrs.riot.exception;

public class RiotAccountNotFoundException extends RiotApiException {

    public RiotAccountNotFoundException(String identifier) {
        super(404, "Riot account not found: " + identifier);
    }
}
