package br.com.finlumia.identify.services;

import br.com.finlumia.identify.models.UserProfileRecord;
import br.com.finlumia.identify.repositorys.RefreshTokenRepository;
import br.com.finlumia.identify.repositorys.UserRepository;
import br.com.finlumia.identify.views.GoogleLoginResponse;
import br.com.finlumia.identify.views.UserProfileResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Value("${finlumia.identify.google.client-id}") String googleClientId) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @Transactional
    public GoogleLoginResponse authenticate(String idToken) {
        GoogleClaims claims = verifyGoogleToken(idToken);

        String normalizedEmail = claims.email().trim().toLowerCase();
        Optional<UserProfileRecord> existing = userRepository.findFullProfileByEmail(normalizedEmail);

        boolean isNewUser = existing.isEmpty();
        UserProfileRecord profile = existing.orElseGet(() ->
                userRepository.createOAuthUser(normalizedEmail, claims.name()));

        Instant accessExpiresAt = Instant.now().plusSeconds(jwtService.getAccessTokenExpirationSeconds());
        Instant refreshExpiresAt = Instant.now().plusSeconds(jwtService.getRefreshTokenExpirationSeconds());

        String jti = UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(profile.key(), profile.email(), jti, accessExpiresAt);
        String refreshToken = TokenSecurityUtils.generateRefreshToken();
        refreshTokenRepository.save(profile.key(), TokenSecurityUtils.hashToken(refreshToken), refreshExpiresAt);

        UserProfileResponse userResponse = toResponse(profile);
        return new GoogleLoginResponse(accessToken, refreshToken, "Bearer",
                jwtService.getAccessTokenExpirationSeconds(), userResponse, isNewUser);
    }

    private GoogleClaims verifyGoogleToken(String idToken) {
        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException e) {
            throw new FinlumiaException(401, "Token invalido", "idToken do Google invalido ou expirado.");
        }
        if (token == null) {
            throw new FinlumiaException(401, "Token invalido", "idToken do Google invalido ou expirado.");
        }

        GoogleIdToken.Payload payload = token.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new FinlumiaException(401, "E-mail nao verificado", "O e-mail da conta Google nao esta verificado.");
        }

        String name = (String) payload.get("name");
        return new GoogleClaims(payload.getEmail(), name != null ? name : payload.getEmail());
    }

    private UserProfileResponse toResponse(UserProfileRecord profile) {
        return new UserProfileResponse(
                profile.key(),
                profile.name(),
                profile.email(),
                profile.role(),
                profile.status(),
                profile.mfa(),
                profile.lastLogin(),
                profile.createdAt(),
                profile.updatedAt());
    }

    private record GoogleClaims(String email, String name) {}
}
