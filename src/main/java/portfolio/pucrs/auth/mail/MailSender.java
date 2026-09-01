package portfolio.pucrs.auth.mail;

public interface MailSender {

    void sendVerificationCode(String to, String code);
}
