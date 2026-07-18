package br.com.finlumia.identify.services;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import br.com.finlumia.identify.models.RefreshTokenRecord;
import br.com.finlumia.identify.models.User;
import br.com.finlumia.identify.models.UserProfileRecord;
import br.com.finlumia.identify.repositorys.RefreshTokenRepository;
import br.com.finlumia.identify.repositorys.TokenBlacklistRepository;
import br.com.finlumia.identify.repositorys.UserRepository;
import br.com.finlumia.identify.views.TokenResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;
    private static final long DEFAULT_REFRESH_TTL_SECONDS = 604_800L;   // 7 dias
    private static final long REMEMBER_REFRESH_TTL_SECONDS = 2_592_000L; // 30 dias

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public TokenService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenBlacklistRepository tokenBlacklistRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponse authenticate(String email, String password, boolean remember, String clientIp, String userAgent) {
        String normalized = normalizeEmail(email);

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> {
                    log.warn("AUTH_FAILURE email={} ip={} reason=USER_NOT_FOUND", mask(normalized), clientIp);
                    return invalidCredentials();
                });

        if (!user.active()) {
            log.warn("AUTH_FAILURE email={} ip={} reason=USER_INACTIVE", mask(normalized), clientIp);
            throw new FinlumiaException(403, "Acesso negado", "Usuário inativo ou bloqueado. Entre em contato com o suporte.");
        }

        if (user.lockedUntil() != null && Instant.now().isBefore(user.lockedUntil())) {
            log.warn("AUTH_FAILURE email={} ip={} reason=ACCOUNT_LOCKED locked_until={}",
                    mask(normalized), clientIp, user.lockedUntil());
            throw new FinlumiaException(429, "Conta bloqueada",
                    "Muitas tentativas incorretas. Tente novamente em " + LOCKOUT_MINUTES + " minutos.");
        }

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            userRepository.incrementFailedAttempts(normalized);
            log.warn("AUTH_FAILURE email={} ip={} reason=WRONG_PASSWORD attempts={}",
                    mask(normalized), clientIp, user.failedAttempts() + 1);
            throw invalidCredentials();
        }

        if (!user.emailVerified()) {
            log.warn("AUTH_FAILURE email={} ip={} reason=EMAIL_NOT_VERIFIED", mask(normalized), clientIp);
            throw new FinlumiaException(403, "E-mail nao verificado",
                    "Confirme seu e-mail antes de entrar. Reenviamos o codigo se necessario.");
        }

        userRepository.resetFailedAttempts(user.key());

        UserProfileRecord profile = userRepository.findFullProfileByKey(user.key())
                .orElseThrow(() -> new FinlumiaException(500, "Erro interno", "Perfil de usuário não encontrado."));

        TokenResponse tokens = issueTokenPair(user, profile, remember);
        log.info("AUTH_SUCCESS user_id={} ip={} ua={} remember={}", user.key(), clientIp, truncate(userAgent), remember);
        return tokens;
    }

    @Transactional
    public TokenResponse refresh(String refreshToken, String clientIp) {
        String tokenHash = hash(refreshToken);
        Optional<RefreshTokenRecord> anyToken = refreshTokenRepository.findByTokenHashIncludeRevoked(tokenHash);

        if (anyToken.isPresent() && anyToken.get().revoked()) {
            UUID userId = anyToken.get().userKey();
            refreshTokenRepository.revokeAllByUserKey(userId);
            log.warn("AUTH_REPLAY_ATTACK user_id={} ip={} action=revoked_all_tokens", userId, clientIp);
            throw new FinlumiaException(401, "Sessão comprometida",
                    "Sessão encerrada por motivo de segurança. Faça login novamente.");
        }

        RefreshTokenRecord storedToken = refreshTokenRepository.findActiveByTokenHash(tokenHash)
                .orElseThrow(() -> new FinlumiaException(401, "Refresh token invalido",
                        "Refresh token invalido ou expirado."));

        User user = userRepository.findActiveByKey(storedToken.userKey())
                .orElseThrow(() -> new FinlumiaException(401, "Usuario invalido",
                        "Usuario nao encontrado ou inativo."));

        refreshTokenRepository.revokeByTokenHash(storedToken.tokenHash());

        UserProfileRecord profile = userRepository.findFullProfileByKey(user.key())
                .orElseThrow(() -> new FinlumiaException(500, "Erro interno", "Perfil de usuário não encontrado."));

        TokenResponse tokens = issueTokenPair(user, profile, false);
        log.info("AUTH_REFRESH user_id={} ip={}", user.key(), clientIp);
        return tokens;
    }

    @Transactional
    public void revoke(String accessToken, String refreshToken, String clientIp) {
        Claims claims = jwtService.parseClaimsAllowExpired(accessToken);

        if (!jwtService.isAccessToken(claims)) {
            throw new FinlumiaException(401, "Token invalido", "Somente access tokens podem ser revogados.");
        }

        String jti = jwtService.extractJti(claims);
        if (tokenBlacklistRepository.existsActiveByJti(jti)) {
            return;
        }

        tokenBlacklistRepository.save(jti, jwtService.extractExpiration(claims));

        UUID userId = jwtService.extractUserKey(claims);
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.revokeByTokenHash(hash(refreshToken));
        }

        log.info("AUTH_REVOKE user_id={} ip={}", userId, clientIp);
    }

    public void ensureAccessTokenIsActive(String accessToken) {
        Claims claims = jwtService.parseClaims(accessToken);

        if (!jwtService.isAccessToken(claims)) {
            throw new FinlumiaException(401, "Token invalido", "Access token invalido.");
        }

        if (tokenBlacklistRepository.existsActiveByJti(jwtService.extractJti(claims))) {
            throw new FinlumiaException(401, "Token revogado", "Access token foi revogado.");
        }
    }

    private TokenResponse issueTokenPair(User user, UserProfileRecord profile, boolean remember) {
        long refreshTtlSeconds = remember ? REMEMBER_REFRESH_TTL_SECONDS : DEFAULT_REFRESH_TTL_SECONDS;

        Instant accessExpiresAt = Instant.now().plusSeconds(jwtService.getAccessTokenExpirationSeconds());
        Instant refreshExpiresAt = Instant.now().plusSeconds(refreshTtlSeconds);

        String jti = UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(user.key(), user.email(), jti, accessExpiresAt);

        String refreshToken = TokenSecurityUtils.generateRefreshToken();
        refreshTokenRepository.save(user.key(), hash(refreshToken), refreshExpiresAt);

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                new TokenResponse.UserInfo(
                        profile.key(),
                        profile.name(),
                        profile.email(),
                        profile.role().name(),
                        profile.status().name(),
                        profile.mfa(),
                        profile.lastLogin(),
                        profile.createdAt(),
                        profile.updatedAt()));
    }

    private FinlumiaException invalidCredentials() {
        return new FinlumiaException(401, "Credenciais invalidas", "Email ou senha invalidos.");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String token) {
        return TokenSecurityUtils.hashToken(token);
    }

    private String mask(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + domain;
    }

    private String truncate(String value) {
        if (value == null) return "";
        return value.length() > 100 ? value.substring(0, 100) + "…" : value;
    }
}
