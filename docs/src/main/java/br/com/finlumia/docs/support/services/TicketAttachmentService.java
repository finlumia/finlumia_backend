package br.com.finlumia.docs.support.services;

import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.docs.support.views.TicketAttachmentView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketAttachmentService {

    private static final long MAX_SIZE = 10_485_760L;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/png", "image/jpeg", "image/webp",
            "application/pdf", "text/plain", "text/csv");

    private final JdbcTemplate jdbc;
    private final TicketService ticketService;
    private final String uploadDir;

    public TicketAttachmentService(
            JdbcTemplate jdbc,
            TicketService ticketService,
            @Value("${finlumia.support.upload-dir:uploads/support}") String uploadDir) {
        this.jdbc = jdbc;
        this.ticketService = ticketService;
        this.uploadDir = uploadDir;
    }

    @Transactional
    public TicketAttachmentView upload(
            UUID ticketId,
            UUID callerId,
            String callerRole,
            MultipartFile file,
            UUID responseId) throws IOException {

        ticketService.ensureCanAccess(ticketId, callerId, callerRole);

        if (file.getSize() > MAX_SIZE) {
            throw new FinlumiaException(413, "Arquivo muito grande", "O arquivo excede o limite de 10MB.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME.contains(mimeType)) {
            throw new FinlumiaException(415, "Tipo nao suportado", "Tipo de arquivo nao permitido.");
        }

        UUID attachmentId = UUID.randomUUID();
        String originalName = Path.of(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file")
                .getFileName()
                .toString();
        String storagePath = ticketId + "/" + attachmentId + "_" + originalName;

        Path target = Paths.get(uploadDir).resolve(storagePath);
        Files.createDirectories(target.getParent());
        file.transferTo(target);

        String insertSql = """
                INSERT INTO docs.ticket_attachments
                    (id, ticket_id, response_id, uploaded_by, file_name, file_size_bytes, mime_type, storage_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbc.update(insertSql, attachmentId, ticketId, responseId, callerId,
                originalName, (int) file.getSize(), mimeType, storagePath);

        return new TicketAttachmentView(
                attachmentId,
                originalName,
                (int) file.getSize(),
                mimeType,
                "/api/v1/support/tickets/" + ticketId + "/attachments/" + attachmentId + "/download",
                java.time.Instant.now());
    }

    public Path resolveDownloadPath(UUID ticketId, UUID attachmentId, UUID callerId, String callerRole) throws IOException {
        ticketService.ensureCanAccess(ticketId, callerId, callerRole);

        List<String> paths = jdbc.queryForList(
                "SELECT storage_path FROM docs.ticket_attachments WHERE id = ? AND ticket_id = ?",
                String.class, attachmentId, ticketId);
        if (paths.isEmpty()) {
            throw new FinlumiaException(404, "Nao encontrado", "Anexo nao encontrado.");
        }

        Path file = Paths.get(uploadDir).resolve(paths.get(0));
        if (!Files.exists(file)) {
            throw new FinlumiaException(404, "Nao encontrado", "Arquivo fisico nao encontrado.");
        }
        return file;
    }
}
