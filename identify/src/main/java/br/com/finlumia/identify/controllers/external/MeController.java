package br.com.finlumia.identify.controllers.external;

import br.com.finlumia.identify.controllers.ExternalApi;
import br.com.finlumia.identify.models.ChangePasswordRequest;
import br.com.finlumia.identify.models.ToggleMfaRequest;
import br.com.finlumia.identify.models.UpdateProfileRequest;
import br.com.finlumia.identify.services.JwtAuthentication;
import br.com.finlumia.identify.services.UserProfileService;
import br.com.finlumia.identify.views.MfaToggleResponse;
import br.com.finlumia.identify.views.UserProfileResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static br.com.finlumia.identify.config.IdentifyOpenApiConfig.BEARER_AUTH;

@ExternalApi
@RestController
@RequestMapping("/api/identify/me")
@Tag(name = "Me", description = "Gerenciamento do perfil do usuario autenticado")
@SecurityRequirement(name = BEARER_AUTH)
public class MeController {

    private final UserProfileService userProfileService;

    public MeController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Operation(summary = "Obter perfil", description = "Retorna o perfil completo do usuario autenticado.")
    @ApiResponse(
            responseCode = "200",
            description = "Perfil retornado com sucesso",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class)))
    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResponse> getProfile() {
        UserProfileResponse response = userProfileService.getProfile(currentAuthentication().getUserKey());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Atualizar perfil", description = "Atualiza nome e preferencias do usuario autenticado.")
    @ApiResponse(
            responseCode = "200",
            description = "Perfil atualizado",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class)))
    @ApiResponse(responseCode = "422", description = "Dados invalidos")
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = userProfileService.updateProfile(
                currentAuthentication().getUserKey(),
                request.name(),
                request.locale(),
                request.theme());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trocar senha", description = "Troca a senha exigindo confirmacao da senha atual.")
    @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso")
    @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou senhas nao coincidem")
    @PostMapping(path = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(
                currentAuthentication().getUserKey(),
                request.currentPassword(),
                request.newPassword(),
                request.confirmPassword());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Habilitar / desabilitar MFA", description = "Ativa ou desativa MFA. Exige senha atual como confirmacao.")
    @ApiResponse(
            responseCode = "200",
            description = "MFA atualizado",
            content = @Content(schema = @Schema(implementation = MfaToggleResponse.class)))
    @ApiResponse(responseCode = "400", description = "Senha atual incorreta")
    @PatchMapping(path = "/mfa", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MfaToggleResponse> toggleMfa(@Valid @RequestBody ToggleMfaRequest request) {
        MfaToggleResponse response = userProfileService.toggleMfa(
                currentAuthentication().getUserKey(),
                request.enabled(),
                request.currentPassword());
        return ResponseEntity.ok(response);
    }

    private JwtAuthentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthentication jwtAuthentication) {
            return jwtAuthentication;
        }
        throw new FinlumiaException(401, "Nao autenticado", "Bearer token e obrigatorio.");
    }
}
