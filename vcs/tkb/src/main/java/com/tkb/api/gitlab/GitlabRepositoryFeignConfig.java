package com.tkb.api.gitlab;

import feign.Client;
import org.springframework.context.annotation.Bean;

/**
 * @deprecated 不再使用，也沒有任何 {@code @FeignClient} 指向這個設定了（見
 * {@link GitlabRepositoryApiClient} 上的說明）。換用 {@code Client.Default} 後
 * 404 問題依舊，代表問題其實出在 Feign 自己的 URI 模板解析，不是 HTTP client 的選擇。
 * 保留這個檔案只是留紀錄，不會被 Spring 載入。
 */
@Deprecated
public class GitlabRepositoryFeignConfig {

    @Bean
    public Client feignClient() {
        return new Client.Default(null, null);
    }
}
