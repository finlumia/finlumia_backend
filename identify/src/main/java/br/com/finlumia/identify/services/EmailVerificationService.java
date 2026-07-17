package br.com.finlumia.identify.services;

import java.time.Instant;
import java.util.Locale;

import br.com.finlumia.identify.models.EmailVerificationRecord;
import br.com.finlumia.identify.repositorys.EmailVerificationRepository;
import br.com.finlumia.identify.repositorys.UserRepository;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    private static final long CODE_EXPIRES_SECONDS = 600;

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public EmailVerificationService(
            EmailVerificationRepository emailVerificationRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendVerificationCode(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String code = TokenSecurityUtils.generateOtp();
        String codeHash = TokenSecurityUtils.hashToken(code);
        Instant expiresAt = Instant.now().plusSeconds(CODE_EXPIRES_SECONDS);
        emailVerificationRepository.save(normalizedEmail, codeHash, expiresAt);
        emailService.sendVerificationCodeEmail(normalizedEmail, code);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        EmailVerificationRecord record = emailVerificationRepository.findActiveByEmail(normalizedEmail)
                .orElseThrow(() -> new FinlumiaException(400, "Codigo invalido", "Codigo invalido ou expirado."));

        if (!TokenSecurityUtils.hashToken(code).equals(record.codeHash())) {
            throw new FinlumiaException(400, "Codigo invalido", "Codigo invalido ou expirado.");
        }

        userRepository.markEmailVerified(normalizedEmail);
        emailVerificationRepository.deleteByEmail(normalizedEmail);
    }

    @Transactional
    public void resendVerificationCode(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!userRepository.existsByEmail(normalizedEmail)) {
            // Nao revela se o e-mail existe, mesmo padrao do forgot-password.
            return;
        }
        emailVerificationRepository.deleteByEmail(normalizedEmail);
        sendVerificationCode(normalizedEmail);
    }
}
