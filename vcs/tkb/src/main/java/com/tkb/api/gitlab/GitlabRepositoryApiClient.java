package com.tkb.api.gitlab;

import com.tkb.api.gitlab.dto.GitlabCommitRequest;
import com.tkb.api.gitlab.dto.GitlabCommitResponse;
import com.tkb.api.gitlab.dto.GitlabFileDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @deprecated 不再使用。原本想透過 Feign 呼叫 GitLab repository files / commits API，
 * 但不管底層用 OkHttp 還是 Feign 內建的 {@code Client.Default}，Feign 自己的
 * {@code RequestTemplate}/URI 模板解析都會把路徑變數裡「已編碼的斜線」(%2F) 還原成
 * 真正的 "/"，導致 GitLab 回 404（file_path、projectId 只要含編碼斜線都會中）。
 * <p>
 * 已改用 {@link com.tkb.service.impl.DeployRegistryServiceImpl} 內直接呼叫
 * {@code java.net.http.HttpClient} 手動組 URL 的方式繞開這個問題，故意拿掉
 * {@code @FeignClient} 註解讓 Spring 不再幫這個介面建立 Feign 代理。保留這個檔案
 * 只是為了留紀錄，避免之後有人重蹈覆轍改回 Feign 版本。
 */
@Deprecated
public interface GitlabRepositoryApiClient {

    GitlabFileDto getRepositoryFile(
            @PathVariable("projectId") String projectId,
            @PathVariable("filePath") String filePath,
            @RequestParam("ref") String ref,
            @RequestHeader("PRIVATE-TOKEN") String token
    );

    GitlabCommitResponse commitFiles(
            @PathVariable("projectId") String projectId,
            @RequestHeader("PRIVATE-TOKEN") String token,
            @RequestBody GitlabCommitRequest body
    );
}
