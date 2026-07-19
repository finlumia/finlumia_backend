package br.com.finlumia.identify.services;

import br.com.finlumia.identify.models.UserProfileRecord;
import br.com.finlumia.identify.models.UserTheme;
import br.com.finlumia.identify.repositorys.UserRepository;
import br.com.finlumia.identify.views.MfaToggleResponse;
import br.com.finlumia.identify.views.UserProfileResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileResponse getProfile(UUID userKey) {
        UserProfileRecord profile = requireProfile(userKey);
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userKey, String name, String locale, UserTheme theme) {
        UserProfileRecord profile = requireProfile(userKey);

        String updatedName = name != null ? name : profile.name();
        String updatedLocale = locale != null ? locale : profile.locale();
        String updatedTheme = theme != null ? theme.name() : profile.theme();

        userRepository.updateProfile(userKey, updatedName, updatedLocale, updatedTheme);
        return toResponse(requireProfile(userKey));
    }

    @Transactional
    public void changePassword(UUID userKey, String currentPassword, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new FinlumiaException(400, "Senhas nao coincidem", "As senhas informadas nao sao iguais.");
        }

        UserProfileRecord profile = requireProfile(userKey);

        if (!passwordEncoder.matches(currentPassword, profile.passwordHash())) {
            throw new FinlumiaException(400, "Senha incorreta", "Senha atual invalida.");
        }

        userRepository.updatePassword(userKey, passwordEncoder.encode(newPassword));
    }

    @Transactional
    public MfaToggleResponse toggleMfa(UUID userKey, boolean enabled, String currentPassword) {
        UserProfileRecord profile = requireProfile(userKey);

        if (!passwordEncoder.matches(currentPassword, profile.passwordHash())) {
            throw new FinlumiaException(400, "Senha incorreta", "Senha atual invalida.");
        }

        userRepository.updateMfa(userKey, enabled);

        String qrCodeUrl = enabled
                ? "otpauth://totp/Finlumia:" + profile.email() + "?secret=PLACEHOLDER&issuer=Finlumia"
                : null;

        return new MfaToggleResponse(enabled, qrCodeUrl);
    }

    private UserProfileRecord requireProfile(UUID userKey) {
        return userRepository.findFullProfileByKey(userKey)
                .orElseThrow(() -> new FinlumiaException(404, "Usuario nao encontrado", "Perfil nao encontrado."));
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
}
