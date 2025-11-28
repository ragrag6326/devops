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

    @Data
    public static class ProjectItem {
        private String name;
        private Long id;
    }
}
