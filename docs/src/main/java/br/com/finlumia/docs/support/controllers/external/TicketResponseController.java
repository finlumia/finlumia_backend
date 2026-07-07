package br.com.finlumia.docs.support.controllers.external;

import br.com.finlumia.docs.support.controllers.ExternalApi;
import br.com.finlumia.docs.support.models.AddResponseRequest;
import br.com.finlumia.docs.support.services.JwtAuthenticationFilter;
import br.com.finlumia.docs.support.services.TicketResponseService;
import br.com.finlumia.docs.support.services.TicketService;
import br.com.finlumia.docs.support.views.TicketResponseView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static br.com.finlumia.docs.config.DocsOpenApiConfig.BEARER_AUTH;

@ExternalApi
@RestController
@RequestMapping("/api/v1/support/tickets/{ticketId}/responses")
@Tag(name = "Respostas", description = "Respostas e comentarios de tickets")
@SecurityRequirement(name = BEARER_AUTH)
public class TicketResponseController {

    private final TicketResponseService responseService;
    private final TicketService ticketService;

    public TicketResponseController(TicketResponseService responseService, TicketService ticketService) {
        this.responseService = responseService;
        this.ticketService = ticketService;
    }

    @Operation(summary = "Listar respostas do ticket", description = "Usuarios comuns nao veem notas internas.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TicketResponseView>> list(
            HttpServletRequest request,
            @PathVariable UUID ticketId) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        return ResponseEntity.ok(responseService.listResponses(ticketId, callerId, callerRole));
    }

    @Operation(summary = "Adicionar resposta ao ticket")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketResponseView> add(
            HttpServletRequest request,
            @PathVariable UUID ticketId,
            @Valid @RequestBody AddResponseRequest body) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseService.addResponse(ticketId, callerId, callerRole, body));
    }
}
