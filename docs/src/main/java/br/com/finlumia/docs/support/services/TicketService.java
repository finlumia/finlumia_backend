package br.com.finlumia.docs.support.services;

import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.docs.support.models.CreateTicketRequest;
import br.com.finlumia.docs.support.models.UpdateTicketRequest;
import br.com.finlumia.docs.support.views.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class TicketService {

    private final JdbcTemplate jdbc;
    private final TicketEmailService ticketEmailService;

    public TicketService(JdbcTemplate jdbc, TicketEmailService ticketEmailService) {
        this.jdbc = jdbc;
        this.ticketEmailService = ticketEmailService;
    }

    // ---------------------------------------------------------------
    // RBAC helper — role is not in JWT, so we query identify.users
    // ---------------------------------------------------------------

    public String getUserRole(UUID userId) {
        List<String> roles = jdbc.queryForList(
                "SELECT users_papel FROM identify.users WHERE users_key = ? AND d_e_l_e_t_e = FALSE",
                String.class, userId);
        return roles.isEmpty() || roles.get(0) == null
                ? "user"
                : roles.get(0).trim().toLowerCase(Locale.ROOT);
    }

    public boolean isPrivileged(String role) {
        return "admin".equals(role) || "gerente".equals(role);
    }

    public void ensureCanAccess(UUID ticketId, UUID callerId, String callerRole) {
        ensureExists(ticketId);
        if (isPrivileged(callerRole)) {
            return;
        }

        Long ownerCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM docs.tickets WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                Long.class, ticketId, callerId);
        if (ownerCount == null || ownerCount == 0) {
            throw new FinlumiaException(403, "Acesso negado", "Voce nao tem permissao para acessar este ticket.");
        }
    }

    // ---------------------------------------------------------------
    // LIST
    // ---------------------------------------------------------------

    public PagedResponse<TicketListItem> list(
            UUID callerId,
            String callerRole,
            String status,
            String category,
            String priority,
            String search,
            UUID filterUserId,
            int page,
            int limit,
            String sort) {

        page = Math.max(1, page);
        limit = Math.max(1, Math.min(100, limit));
        int offset = (page - 1) * limit;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE t.deleted_at IS NULL");

        if (!isPrivileged(callerRole)) {
            where.append(" AND t.user_id = ?");
            params.add(callerId);
        } else if (filterUserId != null) {
            where.append(" AND t.user_id = ?");
            params.add(filterUserId);
        }

        if (status != null && !status.isBlank()) {
            where.append(" AND t.status = ?");
            params.add(status);
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND t.category = ?");
            params.add(category);
        }
        if (priority != null && !priority.isBlank()) {
            where.append(" AND t.priority = ?");
            params.add(priority);
        }
        if (search != null && !search.isBlank()) {
            where.append(" AND (t.ticket_code ILIKE ? OR t.title ILIKE ? OR u.users_nome ILIKE ?)");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        String[] sortParts = (sort != null ? sort : "created_at:desc").split(":");
        String orderCol = resolveSortColumn(sortParts[0]);
        String orderDir = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]) ? "ASC" : "DESC";

        String countSql = """
                SELECT COUNT(*)
                FROM docs.tickets t
                JOIN identify.users u ON u.users_key = t.user_id
                """ + where;
        long total = Objects.requireNonNull(jdbc.queryForObject(countSql, Long.class, params.toArray()));

        String dataSql = """
                SELECT t.id, t.ticket_code, t.title, t.category, t.priority, t.status,
                       t.description, t.assigned_to, t.created_at, t.updated_at,
                       u.users_key AS u_id, u.users_nome AS u_name, u.users_email AS u_email,
                       (SELECT COUNT(*) FROM docs.ticket_responses r WHERE r.ticket_id = t.id) AS response_count
                FROM docs.tickets t
                JOIN identify.users u ON u.users_key = t.user_id
                """ + where + " ORDER BY " + orderCol + " " + orderDir + " LIMIT ? OFFSET ?";

        params.add(limit);
        params.add(offset);

        List<TicketListItem> data = jdbc.query(dataSql, (rs, rowNum) -> mapListItem(rs), params.toArray());
        int totalPages = (int) Math.ceil((double) total / limit);
        return new PagedResponse<>(data, new PaginationMeta(page, limit, total, totalPages));
    }

    // ---------------------------------------------------------------
    // GET BY ID
    // ---------------------------------------------------------------

    public TicketDetailView getById(UUID ticketId, UUID callerId, String callerRole) {
        String sql = """
                SELECT t.id, t.ticket_code, t.title, t.category, t.priority, t.status,
                       t.description, t.assigned_to, t.created_at, t.updated_at,
                       u.users_key AS u_id, u.users_nome AS u_name, u.users_email AS u_email
                FROM docs.tickets t
                JOIN identify.users u ON u.users_key = t.user_id
                WHERE t.id = ? AND t.deleted_at IS NULL
                """;

        List<TicketDetailView> result = jdbc.query(sql, (rs, rowNum) -> mapDetailBase(rs), ticketId);
        if (result.isEmpty()) throw new FinlumiaException(404, "Nao encontrado", "Ticket nao encontrado.");

        TicketDetailView detail = result.get(0);

        if (!isPrivileged(callerRole) && !detail.user().id().equals(callerId)) {
            throw new FinlumiaException(403, "Acesso negado", "Voce nao tem permissao para ver este ticket.");
        }

        List<TicketResponseView> responses = loadResponses(ticketId, isPrivileged(callerRole));
        List<TicketAttachmentView> attachments = loadAttachments(ticketId);

        return new TicketDetailView(
                detail.id(), detail.ticketCode(), detail.user(),
                detail.title(), detail.category(), detail.priority(), detail.status(),
                detail.description(), detail.assignedTo(),
                responses, attachments,
                detail.createdAt(), detail.updatedAt());
    }

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    @Transactional
    public TicketListItem create(UUID callerId, CreateTicketRequest request) {
        String sql = """
                INSERT INTO docs.tickets (user_id, title, category, priority, description)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;
        UUID newId = jdbc.queryForObject(sql, UUID.class,
                callerId,
                request.title(),
                request.category(),
                request.priority() != null ? request.priority() : "media",
                request.description());
        TicketListItem item = getListItemById(newId);
        ticketEmailService.sendTicketConfirmation(item);
        ticketEmailService.sendSupportAlert(item);
        return item;
    }

    // ---------------------------------------------------------------
    // UPDATE (admin/gerente only)
    // ---------------------------------------------------------------

    @Transactional
    public TicketListItem update(UUID ticketId, UUID callerId, String callerRole, UpdateTicketRequest request) {
        if (!isPrivileged(callerRole)) {
            throw new FinlumiaException(403, "Acesso negado", "Apenas admin e gerente podem atualizar tickets.");
        }
        ensureExists(ticketId);

        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (request.status() != null)    { sets.add("status = ?");      params.add(request.status()); }
        if (request.priority() != null)  { sets.add("priority = ?");    params.add(request.priority()); }
        if (request.assignedTo() != null) { sets.add("assigned_to = ?"); params.add(request.assignedTo()); }

        if (!sets.isEmpty()) {
            params.add(ticketId);
            jdbc.update("UPDATE docs.tickets SET " + String.join(", ", sets) + " WHERE id = ?", params.toArray());
        }
        return getListItemById(ticketId);
    }

    // ---------------------------------------------------------------
    // DELETE (admin only, soft-delete)
    // ---------------------------------------------------------------

    @Transactional
    public void delete(UUID ticketId, UUID callerId, String callerRole) {
        if (!"admin".equals(callerRole)) {
            throw new FinlumiaException(403, "Acesso negado", "Apenas admin pode excluir tickets.");
        }
        ensureExists(ticketId);
        jdbc.update("UPDATE docs.tickets SET deleted_at = NOW() WHERE id = ?", ticketId);
    }

    // ---------------------------------------------------------------
    // STATS (admin/gerente only)
    // ---------------------------------------------------------------

    public TicketStatsView stats(String callerRole, String from, String to) {
        if (!isPrivileged(callerRole)) {
            throw new FinlumiaException(403, "Acesso negado", "Apenas admin e gerente podem ver estatisticas.");
        }

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE deleted_at IS NULL");
        if (from != null && !from.isBlank()) { where.append(" AND created_at >= ?::date"); params.add(from); }
        if (to   != null && !to.isBlank())   { where.append(" AND created_at < (?::date + INTERVAL '1 day')"); params.add(to); }

        String base = "FROM docs.tickets " + where;

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String s : List.of("aberto", "em_analise", "respondido", "fechado")) {
            List<Object> p = new ArrayList<>(params);
            p.add(s);
            byStatus.put(s, Objects.requireNonNull(jdbc.queryForObject(
                    "SELECT COUNT(*) " + base + " AND status = ?", Long.class, p.toArray())));
        }

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (String c : List.of("duvida", "bug", "melhoria", "acesso", "outros")) {
            List<Object> p = new ArrayList<>(params);
            p.add(c);
            byCategory.put(c, Objects.requireNonNull(jdbc.queryForObject(
                    "SELECT COUNT(*) " + base + " AND category = ?", Long.class, p.toArray())));
        }

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (String pr : List.of("baixa", "media", "alta", "urgente")) {
            List<Object> p = new ArrayList<>(params);
            p.add(pr);
            byPriority.put(pr, Objects.requireNonNull(jdbc.queryForObject(
                    "SELECT COUNT(*) " + base + " AND priority = ?", Long.class, p.toArray())));
        }

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        Double avgHours = jdbc.queryForObject("""
                SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 3600)
                FROM docs.tickets
                """ + where + " AND status = 'fechado'",
                Double.class, params.toArray());

        return new TicketStatsView(byStatus, byCategory, byPriority, total,
                avgHours != null ? Math.round(avgHours * 10.0) / 10.0 : 0.0);
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    public void ensureExists(UUID ticketId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM docs.tickets WHERE id = ? AND deleted_at IS NULL", Long.class, ticketId);
        if (count == null || count == 0) {
            throw new FinlumiaException(404, "Nao encontrado", "Ticket nao encontrado.");
        }
    }

    private TicketListItem getListItemById(UUID ticketId) {
        String sql = """
                SELECT t.id, t.ticket_code, t.title, t.category, t.priority, t.status,
                       t.description, t.assigned_to, t.created_at, t.updated_at,
                       u.users_key AS u_id, u.users_nome AS u_name, u.users_email AS u_email,
                       (SELECT COUNT(*) FROM docs.ticket_responses r WHERE r.ticket_id = t.id) AS response_count
                FROM docs.tickets t
                JOIN identify.users u ON u.users_key = t.user_id
                WHERE t.id = ? AND t.deleted_at IS NULL
                """;
        List<TicketListItem> result = jdbc.query(sql, (rs, rowNum) -> mapListItem(rs), ticketId);
        if (result.isEmpty()) throw new FinlumiaException(404, "Nao encontrado", "Ticket nao encontrado.");
        return result.get(0);
    }

    private List<TicketResponseView> loadResponses(UUID ticketId, boolean includeInternal) {
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

    private List<TicketAttachmentView> loadAttachments(UUID ticketId) {
        String sql = """
                SELECT id, file_name, file_size_bytes, mime_type, conversion_status,
                       thumbnail_object_key, created_at
                FROM docs.ticket_attachments
                WHERE ticket_id = ?
                ORDER BY created_at ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> TicketAttachmentService.mapAttachmentView(rs, ticketId), ticketId);
    }

    private TicketListItem mapListItem(ResultSet rs) throws SQLException {
        return new TicketListItem(
                rs.getObject("id", UUID.class),
                rs.getString("ticket_code"),
                new UserRef(
                        rs.getObject("u_id", UUID.class),
                        rs.getString("u_name"),
                        rs.getString("u_email")),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getString("description"),
                rs.getString("assigned_to"),
                rs.getLong("response_count"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private TicketDetailView mapDetailBase(ResultSet rs) throws SQLException {
        return new TicketDetailView(
                rs.getObject("id", UUID.class),
                rs.getString("ticket_code"),
                new UserRef(
                        rs.getObject("u_id", UUID.class),
                        rs.getString("u_name"),
                        rs.getString("u_email")),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getString("description"),
                rs.getString("assigned_to"),
                Collections.emptyList(),
                Collections.emptyList(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private String resolveSortColumn(String sortBy) {
        if (sortBy == null) return "t.created_at";
        return switch (sortBy) {
            case "created_at" -> "t.created_at";
            case "updated_at" -> "t.updated_at";
            case "priority"   -> "t.priority";
            case "status"     -> "t.status";
            case "title"      -> "t.title";
            default           -> "t.created_at";
        };
    }
}
