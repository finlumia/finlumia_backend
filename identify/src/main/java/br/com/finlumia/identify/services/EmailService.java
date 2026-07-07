package br.com.finlumia.identify.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAccount;
    private final String fromRecover;
    private final String replyTo;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${finlumia.mail.from.account}") String fromAccount,
            @Value("${finlumia.mail.from.recover}") String fromRecover,
            @Value("${finlumia.mail.reply-to}") String replyTo) {
        this.mailSender = mailSender;
        this.fromAccount = fromAccount;
        this.fromRecover = fromRecover;
        this.replyTo = replyTo;
    }

    public void sendVerificationCodeEmail(String to, String code) {
        send(fromAccount, to, "Finlumia — Confirme seu e-mail", """
                Seu codigo de verificacao Finlumia e: %s

                Este codigo expira em 10 minutos. Se voce nao criou uma conta, ignore este e-mail.
                """.formatted(code));
    }

    public void sendPasswordResetEmail(String to, String code) {
        send(fromRecover, to, "Finlumia — Codigo de redefinicao de senha", """
                Recebemos uma solicitacao para redefinir sua senha Finlumia.

                Seu codigo e: %s

                Este codigo expira em 10 minutos. Se voce nao solicitou, ignore este e-mail — sua senha permanece inalterada.
                """.formatted(code));
    }

    /**
     * Falha de envio nao derruba o fluxo que a chamou (cadastro, recuperacao de
     * senha) — o codigo ja foi persistido e pode ser reenviado depois que o
     * SMTP estiver configurado corretamente.
     */
    private void send(String from, String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setReplyTo(replyTo);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("EMAIL_SEND_FAILURE to={} subject='{}' reason={}", to, subject, e.getMessage());
        }
    }
}
