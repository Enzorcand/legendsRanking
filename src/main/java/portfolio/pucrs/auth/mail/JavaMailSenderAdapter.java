package portfolio.pucrs.auth.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class JavaMailSenderAdapter implements MailSender {

    private final JavaMailSender javaMailSender;
    private final String fromAddress;

    public JavaMailSenderAdapter(JavaMailSender javaMailSender, @Value("${mail.from}") String fromAddress) {
        this.javaMailSender = javaMailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("AAAC Ranking - Codigo de verificacao");
        message.setText("Seu codigo de verificacao e: " + code + "\nEle expira em 15 minutos.");
        javaMailSender.send(message);
    }
}
