package br.com.finlumia.docs.support.controllers.external;

import br.com.finlumia.docs.support.controllers.ExternalApi;
import br.com.finlumia.docs.support.models.CreateTicketRequest;
import br.com.finlumia.docs.support.models.UpdateTicketRequest;
import br.com.finlumia.docs.support.services.JwtAuthenticationFilter;
import br.com.finlumia.docs.support.services.TicketService;
import br.com.finlumia.docs.support.views.PagedResponse;
import br.com.finlumia.docs.support.views.TicketDetailView;
import br.com.finlumia.docs.support.views.TicketListItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static br.com.finlumia.docs.config.DocsOpenApiConfig.BEARER_AUTH;

@ExternalApi
@RestController
@RequestMapping("/api/v1/support/tickets")
@Tag(name = "Tickets", description = "Gerenciamento de tickets de suporte")
@SecurityRequirement(name = BEARER_AUTH)
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Listar tickets", description = "Admin/Gerente veem todos; usuario comum ve apenas os seus.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedResponse<TicketListItem>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @RequestParam(name = "user_id", required = false) UUID filterUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "created_at:desc") String sort) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        return ResponseEntity.ok(ticketService.list(
                callerId, callerRole, status, category, priority, search, filterUserId, page, limit, sort));
    }

    @Operation(summary = "Criar ticket")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketListItem> create(
            HttpServletRequest request,
            @Valid @RequestBody CreateTicketRequest body) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(callerId, body));
    }

    @Operation(summary = "Obter ticket por ID")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketDetailView> getById(
            HttpServletRequest request,
            @PathVariable UUID id) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        return ResponseEntity.ok(ticketService.getById(id, callerId, callerRole));
    }

    @Operation(summary = "Atualizar ticket (admin/gerente)")
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketListItem> update(
            HttpServletRequest request,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest body) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        return ResponseEntity.ok(ticketService.update(id, callerId, callerRole, body));
    }

    @Operation(summary = "Excluir ticket (soft delete, apenas admin)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable UUID id) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        ticketService.delete(id, callerId, callerRole);
        return ResponseEntity.noContent().build();
    }
}
