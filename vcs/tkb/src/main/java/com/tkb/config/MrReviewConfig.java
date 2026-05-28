package com.tkb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mr-review")
public class MrReviewConfig {

    /** GitLab Webhook 設定的 Secret Token（對應 X-Gitlab-Token） */
    private String gitlabWebhookSecret;

    /** 審核完成後是否自動在 GitLab MR 發留言 */
    private boolean postGitlabComment = true;

    private Scheduler scheduler = new Scheduler();

    @Data
    public static class Scheduler {
        private boolean enabled = false;
        private String cron = "0 */10 * * * ?";
    }
}
