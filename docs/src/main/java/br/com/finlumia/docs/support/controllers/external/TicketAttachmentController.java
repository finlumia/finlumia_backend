package br.com.finlumia.docs.support.controllers.external;

import br.com.finlumia.docs.support.controllers.ExternalApi;
import br.com.finlumia.docs.support.services.JwtAuthenticationFilter;
import br.com.finlumia.docs.support.services.TicketAttachmentService;
import br.com.finlumia.docs.support.services.TicketService;
import br.com.finlumia.docs.support.views.TicketAttachmentView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static br.com.finlumia.docs.config.DocsOpenApiConfig.BEARER_AUTH;

@ExternalApi
@RestController
@RequestMapping("/api/v1/support/tickets/{ticketId}/attachments")
@Tag(name = "Anexos", description = "Upload e download de arquivos em tickets")
@SecurityRequirement(name = BEARER_AUTH)
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;
    private final TicketService ticketService;

    public TicketAttachmentController(TicketAttachmentService attachmentService, TicketService ticketService) {
        this.attachmentService = attachmentService;
        this.ticketService = ticketService;
    }

    @Operation(summary = "Upload de arquivo (max 10MB)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketAttachmentView> upload(
            HttpServletRequest request,
            @PathVariable UUID ticketId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "response_id", required = false) UUID responseId) throws IOException {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(ticketId, callerId, callerRole, file, responseId));
    }

    @Operation(summary = "Download de anexo")
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            HttpServletRequest request,
            @PathVariable UUID ticketId,
            @PathVariable UUID attachmentId) throws IOException {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        Path filePath = attachmentService.resolveDownloadPath(ticketId, attachmentId, callerId, callerRole);

        Resource resource = new FileSystemResource(filePath);
        String fileName = filePath.getFileName().toString();
        String name = fileName.contains("_") ? fileName.substring(fileName.indexOf('_') + 1) : fileName;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
