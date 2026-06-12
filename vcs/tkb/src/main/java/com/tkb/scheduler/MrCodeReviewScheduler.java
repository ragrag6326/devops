package com.tkb.scheduler;

import com.tkb.service.MrCodeReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mr-review.scheduler", name = "enabled", havingValue = "true")
public class MrCodeReviewScheduler {

    private final MrCodeReviewService mrCodeReviewService;

    @Scheduled(cron = "${mr-review.scheduler.cron:0 */10 * * * ?}")
    public void scheduledScan() {
        log.info("定時 MR AI Code Review 掃描開始");
        log.info("掃描完成: {}", mrCodeReviewService.scanAllProjects().getMessage());
    }
}
