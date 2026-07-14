package com.tkb.api.gitlab;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tkb.api.gitlab.dto.GitlabCommitRequest;
import com.tkb.api.gitlab.dto.GitlabFileDto;
import com.tkb.config.GitlabConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * deploy.git（Jenkins buildWithParameters 用的部署腳本倉庫）檔案讀寫共用 client。
 * <p>
 * 供 DeployRegistryService（config/project_deploy.json）與 DeployGitConfigService
 * （config/remote_gitlab_repo.json、config/vmIP.json、template/*_Dockerfile）共用。
 * <p>
 * 這裡故意「不」透過 Feign 呼叫 GitLab API，改用 {@link HttpClient} 手動組 URL 直接送出。
 * 原因：GitLab 的 repository files API 需要在 URL 路徑裡放「已經 URL-encode 過的斜線」
 * (%2F)，Feign 的 RequestTemplate/URI 模板解析階段會把 %2F decode 回 "/" 導致 404，
 * 跟底層換哪個 HTTP client 無關。直接用 JDK HttpClient + 手動組字串最單純可靠。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeployGitFileClient {

    /** deploy.git 內各設定檔位置（shell 端用 jq / cp 讀取同一組檔案） */
    public static final String PROJECT_DEPLOY_PATH = "config/project_deploy.json";
    public static final String REMOTE_GITLAB_REPO_PATH = "config/remote_gitlab_repo.json";
    public static final String VM_IP_PATH = "config/vmIP.json";
    public static final String TEMPLATE_DIR = "template";

    private final GitlabConfig gitlabConfig;

    // GitLab API 之後可能會加新欄位，忽略未知欄位比每次都改 DTO 穩定
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** 讀取檔案（含 metadata，例如 last_commit_id）。檔案不存在回傳 null。 */
    public GitlabFileDto readFileMeta(String filePath) {
        String url = projectBase()
                + "/repository/files/" + encodePath(filePath)
                + "?ref=" + gitlabConfig.getDeployBranch();

        HttpResponse<String> response = send(
                HttpRequest.newBuilder(URI.create(url))
                        .header("PRIVATE-TOKEN", gitlabConfig.getToken())
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build());

        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("讀取 " + filePath + " 失敗，HTTP " + response.statusCode()
                    + "：" + response.body()
                    + "（請確認 gitlab.deploy-project-id / gitlab.deploy-branch 設定是否正確）");
        }
        try {
            return objectMapper.readValue(response.body(), GitlabFileDto.class);
        } catch (Exception e) {
            throw new RuntimeException("解析 " + filePath + " 回應失敗: " + e.getMessage(), e);
        }
    }

    /** 讀取檔案內容（已解 base64）。檔案不存在回傳 null。 */
    public String readFile(String filePath) {
        GitlabFileDto file = readFileMeta(filePath);
        return file == null ? null : decode(file);
    }

    public boolean fileExists(String filePath) {
        return readFileMeta(filePath) != null;
    }

    /** 列出指定路徑下所有檔案（blob）的完整路徑，遞迴＋自動翻頁 */
    public List<String> listTree(String path) {
        List<String> result = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = projectBase()
                    + "/repository/tree?path=" + encodePath(path)
                    + "&ref=" + gitlabConfig.getDeployBranch()
                    + "&recursive=true&per_page=100&page=" + page;

            HttpResponse<String> response = send(
                    HttpRequest.newBuilder(URI.create(url))
                            .header("PRIVATE-TOKEN", gitlabConfig.getToken())
                            .timeout(Duration.ofSeconds(15))
                            .GET()
                            .build());

            if (response.statusCode() == 404) {
                return result; // 目錄不存在視為空清單
            }
            if (response.statusCode() != 200) {
                throw new RuntimeException("列出 " + path + " 失敗，HTTP " + response.statusCode()
                        + "：" + response.body());
            }

            try {
                JsonNode items = objectMapper.readTree(response.body());
                if (!items.isArray() || items.isEmpty()) {
                    break;
                }
                for (JsonNode item : items) {
                    if ("blob".equals(item.path("type").asText())) {
                        result.add(item.path("path").asText());
                    }
                }
                if (items.size() < 100) {
                    break;
                }
                page++;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("解析 tree 回應失敗: " + e.getMessage(), e);
            }
        }
        return result;
    }

    /** 以單一 commit 寫入多個檔案異動（create/update/delete），直接 push 至 deploy.git */
    public void commit(String message, List<GitlabCommitRequest.Action> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new RuntimeException("commit 內容為空，未執行任何寫入");
        }
        try {
            GitlabCommitRequest body = new GitlabCommitRequest(
                    gitlabConfig.getDeployBranch(), message, actions);
            String payload = objectMapper.writeValueAsString(body);

            String url = projectBase() + "/repository/commits";

            HttpResponse<String> response = send(
                    HttpRequest.newBuilder(URI.create(url))
                            .header("PRIVATE-TOKEN", gitlabConfig.getToken())
                            .header("Content-Type", "application/json; charset=utf-8")
                            .timeout(Duration.ofSeconds(15))
                            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                            .build());

            if (response.statusCode() / 100 != 2) {
                // GitLab 樂觀鎖衝突時通常回 400 且訊息含 "You are attempting to update a file that has changed"
                throw new RuntimeException("commit 失敗，HTTP " + response.statusCode() + "：" + response.body());
            }
            log.info("[DeployGit] 已 commit 至 deploy.git ({} 分支): {}（{} 個檔案異動）",
                    gitlabConfig.getDeployBranch(), message, actions.size());
        } catch (RuntimeException e) {
            log.error("[DeployGit] commit 失敗: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[DeployGit] commit 失敗: {}", e.getMessage());
            throw new RuntimeException("寫回 deploy.git 失敗: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────── private ───────────────────────────

    private String projectBase() {
        return gitlabConfig.getUrl() + "/projects/" + gitlabConfig.getDeployProjectId();
    }

    /** 檔案路徑 URL-encode（"/" → %2F）。URLEncoder 會把空白轉 "+"，需再修正為 %20 */
    private String encodePath(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("呼叫 GitLab API 失敗: " + e.getMessage(), e);
        }
    }

    private String decode(GitlabFileDto file) {
        if (file.getContent() == null) {
            throw new RuntimeException(file.getFile_path() + " 內容為空");
        }
        if (file.getEncoding() == null || "base64".equalsIgnoreCase(file.getEncoding())) {
            return new String(Base64.getDecoder().decode(file.getContent()), StandardCharsets.UTF_8);
        }
        return file.getContent();
    }
}
