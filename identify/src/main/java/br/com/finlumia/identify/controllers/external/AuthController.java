package br.com.finlumia.identify.controllers.external;

import br.com.finlumia.identify.controllers.ExternalApi;
import br.com.finlumia.identify.models.ForgotPasswordRequest;
import br.com.finlumia.identify.models.GoogleLoginRequest;
import br.com.finlumia.identify.models.RegisterRequest;
import br.com.finlumia.identify.models.ResendVerificationRequest;
import br.com.finlumia.identify.models.ResetPasswordRequest;
import br.com.finlumia.identify.models.VerifyEmailRequest;
import br.com.finlumia.identify.models.VerifyResetTokenRequest;
import br.com.finlumia.identify.services.EmailVerificationService;
import br.com.finlumia.identify.services.GoogleAuthService;
import br.com.finlumia.identify.services.PasswordResetService;
import br.com.finlumia.identify.services.RegisterService;
import br.com.finlumia.identify.views.ForgotPasswordResponse;
import br.com.finlumia.identify.views.GoogleLoginResponse;
import br.com.finlumia.identify.views.RegisterResponse;
import br.com.finlumia.identify.views.VerifyResetTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/identify/auth")
@Tag(name = "Auth", description = "Autenticacao social e recuperacao de senha")
public class AuthController {

    private final GoogleAuthService googleAuthService;
    private final PasswordResetService passwordResetService;
    private final RegisterService registerService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            GoogleAuthService googleAuthService,
            PasswordResetService passwordResetService,
            RegisterService registerService,
            EmailVerificationService emailVerificationService) {
        this.googleAuthService = googleAuthService;
        this.passwordResetService = passwordResetService;
        this.registerService = registerService;
        this.emailVerificationService = emailVerificationService;
    }

    @Operation(
            summary = "Criar conta",
            description = "Cria uma conta local (e-mail/senha) e envia um codigo de verificacao de 6 digitos por e-mail. Login fica bloqueado ate a verificacao.")
    @ApiResponse(
            responseCode = "201",
            description = "Conta criada, codigo de verificacao enviado",
            content = @Content(schema = @Schema(implementation = RegisterResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ja existe uma conta com este e-mail")
    @PostMapping(
            path = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registerService.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Verificar e-mail",
            description = "Confirma o codigo de 6 digitos enviado no cadastro, liberando o login.")
    @ApiResponse(responseCode = "200", description = "E-mail verificado com sucesso")
    @ApiResponse(responseCode = "400", description = "Codigo invalido ou expirado")
    @PostMapping(
            path = "/verify-email",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Reenviar codigo de verificacao",
            description = "Gera e envia um novo codigo de verificacao de e-mail. Sempre retorna 200 para nao vazar existencia de e-mail.")
    @ApiResponse(responseCode = "200", description = "Codigo reenviado (se o e-mail existir)")
    @PostMapping(
            path = "/resend-verification",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationCode(request.email());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Login com Google",
            description = "Troca o idToken do Google por tokens JWT da plataforma. Cria o usuario caso seja o primeiro acesso.")
    @ApiResponse(
            responseCode = "200",
            description = "Login bem-sucedido",
            content = @Content(schema = @Schema(implementation = GoogleLoginResponse.class)))
    @ApiResponse(responseCode = "401", description = "idToken invalido ou expirado")
    @PostMapping(
            path = "/login/google",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GoogleLoginResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleLoginResponse response = googleAuthService.authenticate(request.idToken());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Esqueci minha senha",
            description = "Envia codigo OTP de 6 digitos para o e-mail cadastrado. Sempre retorna 200 para nao vazar existencia de e-mail.")
    @ApiResponse(
            responseCode = "200",
            description = "Codigo enviado",
            content = @Content(schema = @Schema(implementation = ForgotPasswordResponse.class)))
    @PostMapping(
            path = "/forgot-password",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = passwordResetService.sendOtp(request.email());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Verificar codigo OTP",
            description = "Valida o OTP de 6 digitos e retorna um resetSession valido por 15 minutos.")
    @ApiResponse(
            responseCode = "200",
            description = "Token valido",
            content = @Content(schema = @Schema(implementation = VerifyResetTokenResponse.class)))
    @ApiResponse(responseCode = "400", description = "Token invalido ou expirado")
    @PostMapping(
            path = "/verify-reset-token",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifyResetTokenResponse> verifyResetToken(@Valid @RequestBody VerifyResetTokenRequest request) {
        VerifyResetTokenResponse response = passwordResetService.verifyOtp(request.email(), request.token());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Redefinir senha",
            description = "Redefine a senha usando o resetSession obtido na etapa de verificacao do OTP.")
    @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso")
    @ApiResponse(responseCode = "400", description = "Senhas nao coincidem ou sessao invalida")
    @PostMapping(
            path = "/reset-password",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(
                request.resetSession(), request.newPassword(), request.confirmPassword());
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
