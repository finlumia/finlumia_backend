package br.com.finlumia.movement.services;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.finlumia.movement.models.Budget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class BudgetEmailService {

    private static final Logger log = LoggerFactory.getLogger(BudgetEmailService.class);

    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbc;
    private final String replyTo;

    public BudgetEmailService(
            JavaMailSender mailSender,
            JdbcTemplate jdbc,
            @Value("${finlumia.mail.reply-to}") String replyTo) {
        this.mailSender = mailSender;
        this.jdbc = jdbc;
        this.replyTo = replyTo;
    }

    public void sendBudgetAlert(UUID userKey, Budget budget, BigDecimal total) {
        String to = jdbc.queryForObject(
                "SELECT users_email FROM identify.users WHERE users_key = ?", String.class, userKey);
        if (to == null) {
            return;
        }

        boolean isReceita = budget.type().getValue().equals("receita");
        String subject = isReceita
                ? "Finlumia — Parabéns! Meta de receita atingida"
                : "Finlumia — Atenção: orçamento atingido";
        String text = isReceita
                ? """
                        Parabéns! Você atingiu sua meta de receita "%s".

                        Total recebido no período (%s a %s): R$ %s
                        Meta definida: R$ %s
                        """.formatted(budget.name(), budget.periodStart(), budget.periodEnd(), total, budget.limitAmount())
                : """
                        Você atingiu o limite do orçamento "%s".

                        Total gasto no período (%s a %s): R$ %s
                        Limite definido: R$ %s

                        Fique atento para não ultrapassar ainda mais o planejado.
                        """.formatted(budget.name(), budget.periodStart(), budget.periodEnd(), total, budget.limitAmount());

        send(to, subject, text);
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(replyTo);
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
