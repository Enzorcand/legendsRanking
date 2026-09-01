package portfolio.pucrs.riot.exception;

public class SyncCooldownException extends RuntimeException {

    public SyncCooldownException(long remainingSeconds) {
        super("Sincronização recente demais. Tente novamente em " + remainingSeconds + " segundos.");
    }
}
