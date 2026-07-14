package com.tkb.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gitlab")
public class GitlabConfig {
    private String url;
    private String token;
    private List<ProjectItem> projects;

    /**
     * deploy.git（Jenkins buildWithParameters 使用的部署腳本倉庫）的 GitLab project 識別碼，
     * 使用「URL-encode 過的 namespace/path」而非數字 ID，這樣不需要額外呼叫 API 查詢 ID。
     * 例如 http://192.168.1.35/HsuDing/deploy.git → "HsuDing%2Fdeploy"
     */
    private String deployProjectId;

    /**
     * deploy.git 讀寫 config/project_deploy.json 所使用的分支，
     * 請設定成與 Jenkins Job SCM 設定一致的分支（例如 main / test）。
     */
    private String deployBranch;

    @Data
    public static class ProjectItem {
        private String name;
        private Long id;
    }
}
