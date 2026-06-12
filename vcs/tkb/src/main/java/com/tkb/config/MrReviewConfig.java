package com.tkb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mr-review")
public class MrReviewConfig {

    private String gitlabWebhookSecret;
    private boolean postGitlabComment = true;
    private Scheduler scheduler = new Scheduler();

    @Data
    public static class Scheduler {
        private boolean enabled = false;
        private String cron = "0 */10 * * * ?";
    }
}
