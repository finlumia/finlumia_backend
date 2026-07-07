package br.com.finlumia.docs.support.controllers.external;

import br.com.finlumia.docs.support.controllers.ExternalApi;
import br.com.finlumia.docs.support.services.JwtAuthenticationFilter;
import br.com.finlumia.docs.support.services.TicketService;
import br.com.finlumia.docs.support.views.TicketStatsView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static br.com.finlumia.docs.config.DocsOpenApiConfig.BEARER_AUTH;

@ExternalApi
@RestController
@RequestMapping("/api/v1/support/tickets/stats")
@Tag(name = "Tickets", description = "Estatisticas de tickets")
@SecurityRequirement(name = BEARER_AUTH)
public class TicketStatsController {

    private final TicketService ticketService;

    public TicketStatsController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Estatisticas de tickets (admin/gerente)", description = "Contadores por status, categoria e prioridade.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketStatsView> stats(
            HttpServletRequest request,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        UUID callerId = (UUID) request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTR_USER_KEY);
        String callerRole = ticketService.getUserRole(callerId);
        return ResponseEntity.ok(ticketService.stats(callerRole, from, to));
    }
}
