package br.com.finlumia.identify.services;

import br.com.finlumia.identify.repositorys.RefreshTokenRepository;
import br.com.finlumia.identify.repositorys.TokenBlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public TokenCleanupService(
            RefreshTokenRepository refreshTokenRepository,
            TokenBlacklistRepository tokenBlacklistRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedRefresh = refreshTokenRepository.deleteExpiredTokens();
        int deletedBlacklist = tokenBlacklistRepository.deleteExpiredBlacklistEntries();
        log.info("TOKEN_CLEANUP deleted_refresh={} deleted_blacklist={}", deletedRefresh, deletedBlacklist);
    }
}
