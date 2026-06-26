package com.ddarahakit.identity.config.security.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료/무효화된 리프레시 토큰을 주기적으로 정리하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final TokenService tokenService;

    /**
     * 매일 새벽 3시에 만료/무효화된 토큰 정리
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        log.info("[SCHEDULER] 만료된 토큰 정리 시작");
        try {
            tokenService.cleanupExpiredTokens();
            log.info("[SCHEDULER] 만료된 토큰 정리 완료");
        } catch (Exception e) {
            log.error("[SCHEDULER] 토큰 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
