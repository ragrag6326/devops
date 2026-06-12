package com.tkb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "n8n.mr-review")
public class N8nConfig {

    private String webhookUrl;
    private String authHeader = "Authorization";
    private String authToken;
    private String callbackBaseUrl = "http://192.168.1.35:8000/api/mr-review/callback";
}
