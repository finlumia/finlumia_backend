package br.com.finlumia.identify.controllers.external;

import br.com.finlumia.identify.models.TokenLoginRequest;
import br.com.finlumia.identify.models.TokenRefreshRequest;
import br.com.finlumia.identify.models.TokenRevokeRequest;
import br.com.finlumia.identify.services.LoginRateLimiter;
import br.com.finlumia.identify.services.TokenService;
import br.com.finlumia.identify.views.TokenResponse;
import br.com.finlumia.identify.controllers.ExternalApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExternalApi
@RestController
@RequestMapping("/api/identify/token")
@Tag(name = "Token", description = "Autenticacao JWT hibrida (access + refresh + blacklist)")
public class TokenController {

    private final TokenService tokenService;
    private final LoginRateLimiter rateLimiter;

    public TokenController(TokenService tokenService, LoginRateLimiter rateLimiter) {
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
    }

    @Operation(
            summary = "Login",
            description = "Valida email e senha e retorna par access/refresh token.")
    @ApiResponse(
            responseCode = "200",
            description = "Tokens emitidos",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "401", description = "Credenciais invalidas")
    @ApiResponse(responseCode = "429", description = "Muitas tentativas — ver header Retry-After")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody TokenLoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        rateLimiter.consumeForIp(clientIp);
        TokenResponse response = tokenService.authenticate(
                request.email(), request.password(),
                Boolean.TRUE.equals(request.remember()),
                clientIp, httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh",
            description = "Rotaciona refresh token e emite novo par de tokens.")
    @ApiResponse(
            responseCode = "200",
            description = "Novo par de tokens",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "401", description = "Refresh token invalido ou expirado")
    @PostMapping(
            path = "/refresh",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> refresh(
            @Valid @RequestBody TokenRefreshRequest request,
            HttpServletRequest httpRequest) {
        TokenResponse response = tokenService.refresh(request.refreshToken(), extractClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Revogar",
            description = "Coloca o jti do access token na blacklist e revoga o refresh token informado.")
    @ApiResponse(responseCode = "204", description = "Tokens revogados")
    @ApiResponse(responseCode = "401", description = "Access token invalido")
    @PostMapping(
            path = "/revoke",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> revoke(
            @Valid @RequestBody TokenRevokeRequest request,
            HttpServletRequest httpRequest) {
        tokenService.revoke(request.accessToken(), request.refreshToken(), extractClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
