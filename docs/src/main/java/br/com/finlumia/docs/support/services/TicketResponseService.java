package br.com.finlumia.docs.support.services;

import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.docs.support.models.AddResponseRequest;
import br.com.finlumia.docs.support.views.AuthorRef;
import br.com.finlumia.docs.support.views.TicketResponseView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TicketResponseService {

    private final JdbcTemplate jdbc;
    private final TicketService ticketService;

    public TicketResponseService(JdbcTemplate jdbc, TicketService ticketService) {
        this.jdbc = jdbc;
        this.ticketService = ticketService;
    }

    // ---------------------------------------------------------------
    // ADD RESPONSE
    // ---------------------------------------------------------------

    @Transactional
    public TicketResponseView addResponse(
            UUID ticketId,
            UUID callerId,
            String callerRole,
            AddResponseRequest request) {

        ticketService.ensureCanAccess(ticketId, callerId, callerRole);

        String currentStatus = jdbc.queryForObject(
                "SELECT status FROM docs.tickets WHERE id = ? AND deleted_at IS NULL",
                String.class, ticketId);

        if ("fechado".equals(currentStatus)) {
            throw new FinlumiaException(403, "Ticket fechado", "Tickets fechados nao podem receber respostas.");
        }

        boolean isInternal = Boolean.TRUE.equals(request.isInternal());
        if (isInternal && !ticketService.isPrivileged(callerRole)) {
            isInternal = false;
        }

        String authorRole = resolveAuthorRole(callerRole);

        String insertSql = """
                INSERT INTO docs.ticket_responses (ticket_id, author_id, author_role, message, is_internal)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id, created_at
                """;

        final boolean finalIsInternal = isInternal;
        UUID[] responseId = {null};
        java.time.Instant[] createdAt = {null};

        jdbc.query(insertSql, rs -> {
            responseId[0] = rs.getObject("id", UUID.class);
            createdAt[0] = rs.getTimestamp("created_at").toInstant();
        }, ticketId, callerId, authorRole, request.message(), isInternal);

        if (request.newStatus() != null && !request.newStatus().isBlank()) {
            jdbc.update("UPDATE docs.tickets SET status = ? WHERE id = ?", request.newStatus(), ticketId);
        } else if (!isInternal && ticketService.isPrivileged(callerRole) && !"fechado".equals(currentStatus)) {
            jdbc.update("UPDATE docs.tickets SET status = 'respondido' WHERE id = ?", ticketId);
        }

        String authorName = jdbc.queryForObject(
                "SELECT users_nome FROM identify.users WHERE users_key = ?",
                String.class, callerId);

        return new TicketResponseView(
                responseId[0],
                new AuthorRef(callerId, authorName, callerRole),
                request.message(),
                finalIsInternal,
                createdAt[0]);
    }

    // ---------------------------------------------------------------
    // LIST RESPONSES
    // ---------------------------------------------------------------

    public List<TicketResponseView> listResponses(UUID ticketId, UUID callerId, String callerRole) {
        ticketService.ensureCanAccess(ticketId, callerId, callerRole);

        boolean includeInternal = ticketService.isPrivileged(callerRole);
        String sql = """
                SELECT r.id, r.message, r.is_internal, r.created_at,
                       a.users_key AS a_id, a.users_nome AS a_name, a.users_papel AS a_role
                FROM docs.ticket_responses r
                JOIN identify.users a ON a.users_key = r.author_id
                WHERE r.ticket_id = ?
                """ + (includeInternal ? "" : " AND r.is_internal = FALSE") + """
                 ORDER BY r.created_at ASC
                """;

        return jdbc.query(sql, (rs, rowNum) -> new TicketResponseView(
                rs.getObject("id", UUID.class),
                new AuthorRef(
                        rs.getObject("a_id", UUID.class),
                        rs.getString("a_name"),
                        rs.getString("a_role")),
                rs.getString("message"),
                rs.getBoolean("is_internal"),
                rs.getTimestamp("created_at").toInstant()),
                ticketId);
    }

    private String resolveAuthorRole(String role) {
        return switch (role) {
            case "admin"   -> "admin";
            case "gerente" -> "gerente";
            default        -> "user";
        };
    }
}
