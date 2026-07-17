package br.com.finlumia.docs.support.services;

import br.com.finlumia.docs.support.views.TicketListItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class TicketEmailService {

    private static final Logger log = LoggerFactory.getLogger(TicketEmailService.class);

    private final JavaMailSender mailSender;
    private final String replyTo;

    public TicketEmailService(JavaMailSender mailSender, @Value("${finlumia.mail.reply-to}") String replyTo) {
        this.mailSender = mailSender;
        this.replyTo = replyTo;
    }

    public void sendTicketConfirmation(TicketListItem ticket) {
        send(ticket.user().email(),
                "Finlumia — Chamado #%s recebido".formatted(ticket.ticketCode()),
                """
                        Recebemos seu chamado de suporte.

                        Código: %s
                        Título: %s

                        Nossa equipe vai analisar e responder em breve.
                        """.formatted(ticket.ticketCode(), ticket.title()));
    }

    public void sendSupportAlert(TicketListItem ticket) {
        send(replyTo,
                "Novo chamado #%s: %s".formatted(ticket.ticketCode(), ticket.title()),
                """
                        Novo chamado de suporte aberto.

                        Código: %s
                        Título: %s
                        Categoria: %s
                        Prioridade: %s
                        Usuário: %s (%s)
                        """.formatted(
                        ticket.ticketCode(), ticket.title(), ticket.category(), ticket.priority(),
                        ticket.user().name(), ticket.user().email()));
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
