package com.tkb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "n8n.mr-review")
public class N8nConfig {

    /** N8N Webhook 完整 URL */
    private String webhookUrl;

    /** 請求標頭名稱，例如 Authorization */
    private String authHeader = "Authorization";

    /** Webhook 驗證 Token */
    private String authToken;

    /** 後端 callback 基底 URL，供 N8N 回寫審核結果 */
    private String callbackBaseUrl = "http://192.168.1.35:8000/api/mr-review/callback";
}
