package br.com.finlumia.identify.services;

import br.com.finlumia.identify.models.PasswordResetRecord;
import br.com.finlumia.identify.repositorys.PasswordResetRepository;
import br.com.finlumia.identify.repositorys.UserRepository;
import br.com.finlumia.identify.views.ForgotPasswordResponse;
import br.com.finlumia.identify.views.VerifyResetTokenResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final long OTP_EXPIRES_SECONDS = 600;
    private static final long RESET_SESSION_EXPIRES_SECONDS = 900;

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetRepository passwordResetRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public ForgotPasswordResponse sendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        // Always returns 200 to avoid leaking whether the email is registered
        userRepository.findActiveByEmail(normalizedEmail).ifPresent(user -> {
            String otp = TokenSecurityUtils.generateOtp();
            String otpHash = TokenSecurityUtils.hashToken(otp);
            Instant expiresAt = Instant.now().plusSeconds(OTP_EXPIRES_SECONDS);
            passwordResetRepository.save(normalizedEmail, otpHash, expiresAt);
            emailService.sendPasswordResetEmail(normalizedEmail, otp);
        });
        return new ForgotPasswordResponse("Codigo enviado para o e-mail informado.", OTP_EXPIRES_SECONDS);
    }

    @Transactional
    public VerifyResetTokenResponse verifyOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        PasswordResetRecord record = passwordResetRepository.findActiveByEmail(normalizedEmail)
                .orElseThrow(() -> new FinlumiaException(400, "Token invalido", "Codigo invalido ou expirado."));

        if (!TokenSecurityUtils.hashToken(otp).equals(record.otpHash())) {
            throw new FinlumiaException(400, "Token invalido", "Codigo invalido ou expirado.");
        }

        String resetSession = UUID.randomUUID().toString();
        Instant sessionExpiresAt = Instant.now().plusSeconds(RESET_SESSION_EXPIRES_SECONDS);
        passwordResetRepository.promoteToResetSession(normalizedEmail, resetSession, sessionExpiresAt);
        return new VerifyResetTokenResponse(resetSession);
    }

    @Transactional
    public void resetPassword(String resetSession, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new FinlumiaException(400, "Senhas nao coincidem", "As senhas informadas nao sao iguais.");
        }

        PasswordResetRecord record = passwordResetRepository.findActiveByResetSession(resetSession)
                .orElseThrow(() -> new FinlumiaException(400, "Sessao invalida", "Sessao de redefinicao invalida ou expirada."));

        userRepository.updatePasswordByEmail(record.email(), passwordEncoder.encode(newPassword));
        passwordResetRepository.deleteByEmail(record.email());
    }
}
