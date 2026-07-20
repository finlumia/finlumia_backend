package br.com.finlumia.docs.support.controllers.external;

import br.com.finlumia.docs.support.controllers.ExternalApi;
import br.com.finlumia.docs.support.models.CompleteUploadRequest;
import br.com.finlumia.docs.support.models.PresignUploadRequest;
import br.com.finlumia.docs.support.services.JwtAuthenticationFilter;
import br.com.finlumia.docs.support.services.TicketAttachmentService;
import br.com.finlumia.docs.support.services.TicketService;
import br.com.finlumia.docs.support.views.PresignUploadResponse;
import br.com.finlumia.docs.support.views.TicketAttachmentView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

import static br.com.finlumia.docs.config.DocsOpenApiConfig.BEARER_AUTH;

@ExternalApi
@RestController
@RequestMapping("/api/v1/support/tickets/{ticketId}/attachments")
@Tag(name = "Anexos", description = "Upload (URL assinada MinIO) e download de arquivos em tickets")
@SecurityRequirement(name = BEARER_AUTH)
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;
    private final TicketService ticketService;

    public TicketAttachmentController(TicketAttachmentService attachmentService, TicketService ticketService) {
        this.attachmentService = attachmentService;
        this.ticketService = ticketService;
    }

    @Operation(summary = "Solicitar URL de upload (10MB para imagem/documento, 150MB para video)",
            description = "O cliente envia o arquivo via PUT direto pra URL retornada, depois confirma com /complete.")
    @PostMapping(path = "/presign", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PresignUploadResponse> presign(
            HttpServletRequest request,
            @PathVariable UUID ticketId,
            @Valid @RequestBody PresignUploadRequest body) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        return ResponseEntity.ok(attachmentService.presignUpload(ticketId, callerId, callerRole, body));
    }

    @Operation(summary = "Confirmar upload concluido no storage")
    @PostMapping(path = "/{attachmentId}/complete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketAttachmentView> complete(
            HttpServletRequest request,
            @PathVariable UUID ticketId,
            @PathVariable UUID attachmentId,
            @Valid @RequestBody CompleteUploadRequest body) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.completeUpload(ticketId, attachmentId, callerId, callerRole, body));
    }

    @Operation(summary = "Download de anexo",
            description = "Redireciona para uma URL assinada do storage. Para video, serve sempre a versao " +
                    "convertida quando disponivel — nunca o arquivo bruto (exceto admin/gerente em caso de falha).")
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Void> download(
            HttpServletRequest request,
            @PathVariable UUID ticketId,
            @PathVariable UUID attachmentId) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        URI target = attachmentService.presignDownloadRedirect(ticketId, attachmentId, callerId, callerRole);
        return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
    }

    @Operation(summary = "Miniatura de anexo de video", description = "Redireciona para a URL assinada da miniatura.")
    @GetMapping("/{attachmentId}/thumbnail")
    public ResponseEntity<Void> thumbnail(
            HttpServletRequest request,
            @PathVariable UUID ticketId,
            @PathVariable UUID attachmentId) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        URI target = attachmentService.presignThumbnailRedirect(ticketId, attachmentId, callerId, callerRole);
        return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
    }
}
