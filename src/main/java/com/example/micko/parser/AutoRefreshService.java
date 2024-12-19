package com.example.micko.parser;

import com.example.micko.rule.RuleCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class AutoRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(AutoRefreshService.class);
    private final RuleCacheManager cacheManager;

    public AutoRefreshService(RuleCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Scheduled Task for Auto-Refresh Every 5 Minutes
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void autoRefreshFromS3() {
        logger.info("Starting automatic refresh from S3...");
        try {
            cacheManager.loadRules();
            logger.info("Auto-refresh completed successfully.");
        } catch (Exception e) {
            logger.error("Auto-refresh failed: {}", e.getMessage(), e);
        }
    }
}
