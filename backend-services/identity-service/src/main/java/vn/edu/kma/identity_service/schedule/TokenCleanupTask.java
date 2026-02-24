package vn.edu.kma.identity_service.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.kma.identity_service.repository.InvalidatedTokenRepository;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    // Chạy vào lúc 12 giờ đêm mỗi ngày (Cron expression)
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupExpiredTokens() {
        log.info("Bắt đầu dọn dẹp Token hết hạn...");

        long startTime = System.currentTimeMillis();
        invalidatedTokenRepository.deleteExpiredTokens(new Date());
        long endTime = System.currentTimeMillis();

        log.info("Dọn dẹp hoàn tất.");
    }
}
