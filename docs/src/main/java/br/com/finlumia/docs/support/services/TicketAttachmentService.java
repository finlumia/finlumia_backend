package br.com.finlumia.docs.support.services;

import br.com.finlumia.docs.support.models.CompleteUploadRequest;
import br.com.finlumia.docs.support.models.PresignUploadRequest;
import br.com.finlumia.docs.support.views.PresignUploadResponse;
import br.com.finlumia.docs.support.views.TicketAttachmentView;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.net.URI;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketAttachmentService {

    private static final long MAX_SIZE = 10_485_760L;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/png", "image/jpeg", "image/webp",
            "application/pdf", "text/plain", "text/csv");
    private static final Set<String> ALLOWED_VIDEO_MIME = Set.of(
            "video/mp4", "video/quicktime", "video/webm");
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofSeconds(60);

    private final JdbcTemplate jdbc;
    private final TicketService ticketService;
    private final StorageService storageService;
    private final VideoConversionService videoConversionService;
    private final long maxVideoSize;

    public TicketAttachmentService(
            JdbcTemplate jdbc,
            TicketService ticketService,
            StorageService storageService,
            VideoConversionService videoConversionService,
            @Value("${finlumia.support.video.max-size-bytes}") long maxVideoSize) {
        this.jdbc = jdbc;
        this.ticketService = ticketService;
        this.storageService = storageService;
        this.videoConversionService = videoConversionService;
        this.maxVideoSize = maxVideoSize;
    }

    // ---------------------------------------------------------------
    // Upload (presigned, 2 fases)
    // ---------------------------------------------------------------

    public PresignUploadResponse presignUpload(
            UUID ticketId, UUID callerId, String callerRole, PresignUploadRequest request) {
        ticketService.ensureCanAccess(ticketId, callerId, callerRole);
        validateMimeAndSize(request.mimeType(), request.fileSizeBytes());

        UUID attachmentId = UUID.randomUUID();
        String objectKey = storageService.buildRawObjectKey(ticketId, attachmentId, request.fileName());
        URI uploadUrl = storageService.presignPutUrl(
                objectKey, request.mimeType(), request.fileSizeBytes(), UPLOAD_URL_TTL);

        return new PresignUploadResponse(attachmentId, uploadUrl.toString(), Instant.now().plus(UPLOAD_URL_TTL));
    }

    @Transactional
    public TicketAttachmentView completeUpload(
            UUID ticketId, UUID attachmentId, UUID callerId, String callerRole, CompleteUploadRequest request) {
        ticketService.ensureCanAccess(ticketId, callerId, callerRole);

        String objectKey = storageService.buildRawObjectKey(ticketId, attachmentId, request.fileName());
        HeadObjectResponse head = storageService.headObject(objectKey);

        boolean isVideo = request.mimeType() != null && request.mimeType().startsWith("video/");
        String conversionStatus = isVideo ? "pending" : "not_applicable";
        String originalName = Path.of(request.fileName()).getFileName().toString();

        String insertSql = """
                INSERT INTO docs.ticket_attachments
                    (id, ticket_id, response_id, uploaded_by, file_name, file_size_bytes, mime_type,
                     storage_path, conversion_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbc.update(insertSql, attachmentId, ticketId, request.responseId(), callerId,
                originalName, head.contentLength().intValue(), request.mimeType(), objectKey, conversionStatus);

        if (isVideo) {
            videoConversionService.enqueue(attachmentId, ticketId, objectKey);
        }

        return getAttachmentView(ticketId, attachmentId);
    }

    private void validateMimeAndSize(String mimeType, long fileSizeBytes) {
        boolean isVideo = mimeType != null && mimeType.startsWith("video/");
        if (isVideo) {
            if (!ALLOWED_VIDEO_MIME.contains(mimeType)) {
                throw new FinlumiaException(415, "Tipo nao suportado", "Formato de video nao permitido.");
            }
            if (fileSizeBytes > maxVideoSize) {
                throw new FinlumiaException(413, "Arquivo muito grande",
                        "O video excede o limite de " + (maxVideoSize / (1024 * 1024)) + "MB.");
            }
        } else {
            if (mimeType == null || !ALLOWED_MIME.contains(mimeType)) {
                throw new FinlumiaException(415, "Tipo nao suportado", "Tipo de arquivo nao permitido.");
            }
            if (fileSizeBytes > MAX_SIZE) {
                throw new FinlumiaException(413, "Arquivo muito grande", "O arquivo excede o limite de 10MB.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Download / miniatura (redirect para URL assinada)
    // ---------------------------------------------------------------

    public URI presignDownloadRedirect(UUID ticketId, UUID attachmentId, UUID callerId, String callerRole) {
        ticketService.ensureCanAccess(ticketId, callerId, callerRole);
        AttachmentRow row = loadRow(ticketId, attachmentId);

        boolean isVideo = row.mimeType() != null && row.mimeType().startsWith("video/");
        if (!isVideo) {
            return storageService.presignGetUrl(row.storagePath(), DOWNLOAD_URL_TTL);
        }

        return switch (row.conversionStatus()) {
            case "completed" -> storageService.presignGetUrl(row.convertedObjectKey(), DOWNLOAD_URL_TTL);
            case "pending", "processing" -> throw new FinlumiaException(
                    409, "Processando", "O video ainda esta sendo processado. Tente novamente em instantes.");
            case "failed" -> {
                if (ticketService.isPrivileged(callerRole)) {
                    yield storageService.presignGetUrl(row.storagePath(), DOWNLOAD_URL_TTL);
                }
                throw new FinlumiaException(409, "Falha no processamento", "Nao foi possivel processar este video.");
            }
            default -> throw new FinlumiaException(
                    409, "Processando", "O video ainda esta sendo processado. Tente novamente em instantes.");
        };
    }

    public URI presignThumbnailRedirect(UUID ticketId, UUID attachmentId, UUID callerId, String callerRole) {
        ticketService.ensureCanAccess(ticketId, callerId, callerRole);
        AttachmentRow row = loadRow(ticketId, attachmentId);
        if (row.thumbnailObjectKey() == null) {
            throw new FinlumiaException(404, "Nao encontrado", "Miniatura nao disponivel.");
        }
        return storageService.presignGetUrl(row.thumbnailObjectKey(), DOWNLOAD_URL_TTL);
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private record AttachmentRow(
            String mimeType, String storagePath, String convertedObjectKey,
            String thumbnailObjectKey, String conversionStatus) {
    }

    private AttachmentRow loadRow(UUID ticketId, UUID attachmentId) {
        String sql = """
                SELECT mime_type, storage_path, converted_object_key, thumbnail_object_key, conversion_status
                FROM docs.ticket_attachments
                WHERE id = ? AND ticket_id = ?
                """;
        List<AttachmentRow> rows = jdbc.query(sql, (rs, rowNum) -> new AttachmentRow(
                rs.getString("mime_type"),
                rs.getString("storage_path"),
                rs.getString("converted_object_key"),
                rs.getString("thumbnail_object_key"),
                rs.getString("conversion_status")
        ), attachmentId, ticketId);
        if (rows.isEmpty()) {
            throw new FinlumiaException(404, "Nao encontrado", "Anexo nao encontrado.");
        }
        return rows.get(0);
    }

    private TicketAttachmentView getAttachmentView(UUID ticketId, UUID attachmentId) {
        String sql = """
                SELECT id, file_name, file_size_bytes, mime_type, conversion_status,
                       thumbnail_object_key, created_at
                FROM docs.ticket_attachments
                WHERE id = ? AND ticket_id = ?
                """;
        List<TicketAttachmentView> result = jdbc.query(
                sql, (rs, rowNum) -> mapAttachmentView(rs, ticketId), attachmentId, ticketId);
        if (result.isEmpty()) {
            throw new FinlumiaException(404, "Nao encontrado", "Anexo nao encontrado.");
        }
        return result.get(0);
    }

    static TicketAttachmentView mapAttachmentView(ResultSet rs, UUID ticketId) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        String thumbnailKey = rs.getString("thumbnail_object_key");
        return new TicketAttachmentView(
                id,
                rs.getString("file_name"),
                rs.getInt("file_size_bytes"),
                rs.getString("mime_type"),
                "/api/v1/support/tickets/" + ticketId + "/attachments/" + id + "/download",
                thumbnailKey != null ? "/api/v1/support/tickets/" + ticketId + "/attachments/" + id + "/thumbnail" : null,
                rs.getString("conversion_status"),
                rs.getTimestamp("created_at").toInstant());
    }
}
