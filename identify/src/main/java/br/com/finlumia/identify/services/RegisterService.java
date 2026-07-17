package br.com.finlumia.identify.services;

import br.com.finlumia.identify.models.UserProfileRecord;
import br.com.finlumia.identify.repositorys.UserRepository;
import br.com.finlumia.identify.views.RegisterResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public RegisterService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public RegisterResponse register(String name, String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new FinlumiaException(409, "E-mail ja cadastrado", "Ja existe uma conta com este e-mail.");
        }

        String passwordHash = passwordEncoder.encode(password);
        UserProfileRecord profile = userRepository.createLocalUser(normalizedEmail, name.trim(), passwordHash);
        emailVerificationService.sendVerificationCode(profile.email());

        return new RegisterResponse("Conta criada. Verifique seu e-mail para ativar o acesso.");
    }
}
